/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager.attestati.service;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.ContractDao;
import dao.ContractMonthRecapDao;
import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.PersonsOfficesDao;
import it.cnr.iit.epas.DateInterval;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.PersonDayManager;
import manager.configurations.EpasParam;
import manager.services.mealtickets.BlockMealTicket;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.mealtickets.IMealTicketsService;
import manager.services.mealtickets.MealTicketRecap;
import manager.services.mealtickets.MealTicketStaticUtility;
import models.Certification;
import models.Competence;
import models.CompetenceCodeGroup;
import models.Configuration;
import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.Person;
import models.PersonMonthRecap;
import models.PersonsOffices;
import models.absences.Absence;
import models.dto.MealTicketComposition;
import models.enumerate.BlockType;
import models.enumerate.CertificationType;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Rappresenta i dati della situazione mensile di una persona.
 */
@Slf4j
public class PersonMonthlySituationData {

  private final PersonMonthRecapDao personMonthRecapDao;
  private final AbsenceDao absenceDao;
  private final CompetenceDao competenceDao;
  private final IMealTicketsService mealTicketService;
  private final ContractDao contractDao;
  private final ContractMonthRecapDao contractMonthRecapDao;

  private final MealTicketDao mealTicketDao;
  private final PersonStampingRecapFactory stampingsRecapFactory;
  private final PersonsOfficesDao personOfficeDao;


  /**
   * Injector.
   *
   * @param personDayManager il persondaymanager
   * @param personMonthRecapDao il dao dei riepiloghi mensili
   * @param absenceDao il dao delle assenze
   * @param competenceDao il dao delle competenze
   * @param personDayDao il dao sui personDay
   * @param contractDao il dao sui contratti
   * @param contractMonthRecapDao il dao sui contract month recap
   */
  @Inject
  public PersonMonthlySituationData(PersonDayManager personDayManager,
      PersonMonthRecapDao personMonthRecapDao, AbsenceDao absenceDao,
      CompetenceDao competenceDao, PersonDayDao personDayDao, 
      IMealTicketsService mealTicketService, ContractDao contractDao,
      ContractMonthRecapDao contractMonthRecapDao, MealTicketDao mealTicketDao, 
      PersonStampingRecapFactory stampingsRecapFactory,
      PersonsOfficesDao personOfficeDao) {

    this.personMonthRecapDao = personMonthRecapDao;
    this.absenceDao = absenceDao;
    this.competenceDao = competenceDao;
    this.mealTicketService = mealTicketService;
    this.contractDao = contractDao;
    this.contractMonthRecapDao = contractMonthRecapDao;

    this.mealTicketDao = mealTicketDao;

    this.stampingsRecapFactory = stampingsRecapFactory;
    this.personOfficeDao = personOfficeDao;

  }

  /**
   * Crea la mappa con le info mensili per i dipendenti.
   *
   * @param person il dipendente di cui cercare le informazioni
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return la mappa contenente le info su assenze, competenze, buoni pasto e ore di formazione 
   *     del dipendente person nell'anno year e nel mese month.
   */
  public Map<String, Certification> getCertification(Person person, int year, int month, 
      Range<LocalDate> affiliationRange) {

    Map<String, Certification> actualCertifications = Maps.newHashMap();
    actualCertifications = trainingHours(person, year, month, 
        actualCertifications, affiliationRange);
    actualCertifications = absences(person, year, month, actualCertifications, affiliationRange);
    actualCertifications = competences(person, year, month, actualCertifications);
    actualCertifications = mealTicket(person, year, month, actualCertifications, affiliationRange);

    return actualCertifications;
  }

  /**
   * Crea la mappa con le info mensili per la lista dei dipendenti passati.
   *
   * @param persons la lista dei dipendenti di cui cercare le informazioni
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return la mappa contenente per ogni dipendente una mappa con le info su assenze,
   *     competenze, buoni pasto e ore di formazione dei dipendente person nell'anno year
   *     e nel mese month.
   */
  public Map<Person, Map<String, Certification>> getCertifications(
      List<Person> persons, int year, int month) {
    
    Map<Person, Map<String, Certification>> actualCertifications = Maps.newHashMap();
    //Popolare la mappa delle certifications di una persona con una mappa vuota, così
    //se nessuno dei metodi successi trova dati/competence per la persona comunque
    //la persona viene restituita
    populateEmptyCertifications(persons, actualCertifications);
    actualCertifications = trainingHours(persons, year, month, actualCertifications);
    //actualCertifications = absences(persons, year, month, actualCertifications);
    actualCertifications = competences(persons, year, month, actualCertifications);
    actualCertifications = mealTickets(persons, year, month, actualCertifications);

    return actualCertifications;
  }

  private void populateEmptyCertifications(
      List<Person> persons, Map<Person, Map<String, Certification>> actualCertifications) {
    persons.forEach(p -> {
      actualCertifications.put(p, Maps.newHashMap());
    });
  }

  /**
   * Produce le certification delle ore di formazione per la persona.
   *
   * @param persons persona
   * @param year anno
   * @param month mese
   * @return certificazioni (sotto forma di mappa)
   */
  private Map<String, Certification> trainingHours(Person person, int year, int month,
      Map<String, Certification> certifications, Range<LocalDate> affiliationRange) {

    List<PersonMonthRecap> trainingHoursList = personMonthRecapDao
        .getPersonMonthRecapInYearOrWithMoreDetails(person, year,
            Optional.fromNullable(month), Optional.absent());
    for (PersonMonthRecap personMonthRecap : trainingHoursList) {


      if (affiliationRange.contains(personMonthRecap.getFromDate()) 
          && affiliationRange.contains(personMonthRecap.getToDate())) {
        // Nuova certificazione
        Certification certification = new Certification();
        certification.setPerson(person);
        certification.setYear(year);
        certification.setMonth(month);
        certification.setCertificationType(CertificationType.FORMATION);
        certification.setContent(Certification
            .serializeTrainingHours(personMonthRecap.getFromDate().getDayOfMonth(),
                personMonthRecap.getToDate().getDayOfMonth(), personMonthRecap.getTrainingHours()));

        certifications.put(certification.aMapKey(), certification);
      }


    }
    return certifications;
  }

  /**
   * Produce una mappa contenente per ogni persona le certification delle ore di
   * formazione della persona nel mese.
   *
   * @param persons la lista dei dipendenti di cui cercare le informazioni
   * @param year anno
   * @param month mese
   * @return certificazioni (sotto forma di mappa)
   */
  private Map<Person, Map<String, Certification>> trainingHours(
      List<Person> persons, int year, int month,
      Map<Person, Map<String, Certification>> certificationsMap) {

    Map<Person, List<PersonMonthRecap>> trainingHoursMap = 
        personMonthRecapDao.getPersonMonthRecaps(persons, year, month);

    trainingHoursMap.entrySet().forEach(es -> {
      Map<String, Certification> certificationMap = new HashMap<>();
      es.getValue().stream().forEach(pmr -> {
        Certification certification = certificationByPersonMonthRecap(pmr);
        certificationMap.put(certification.aMapKey(), certification);
      });
      certificationsMap.put(es.getKey(), certificationMap);
    });
    return certificationsMap;
  }

  /**
   * Costruisce l'oggetto Certification a partire dal PersonMonthRecap passato.
   */
  static Certification certificationByPersonMonthRecap(PersonMonthRecap personMonthRecap) {
    // Nuova certificazione
    Certification certification = new Certification();
    certification.setPerson(personMonthRecap.getPerson());
    certification.setYear(personMonthRecap.getYear());
    certification.setMonth(personMonthRecap.getMonth());
    certification.setCertificationType(CertificationType.FORMATION);
    certification.setContent(Certification
        .serializeTrainingHours(personMonthRecap.getFromDate().getDayOfMonth(),
            personMonthRecap.getToDate().getDayOfMonth(), personMonthRecap.getTrainingHours()));
    return certification;
  }

  /**
   * Produce le certification delle assenze per la persona.
   *
   * @param person persona
   * @param year anno
   * @param month mese
   * @return certificazioni (sotto forma di mappa)
   */
  private Map<String, Certification> absences(Person person, int year, int month,
      Map<String, Certification> certifications, Range<LocalDate> affiliationRange) {

    List<Absence> absences = absenceDao
        .getAbsenceWithNotInternalUseInMonth(person, affiliationRange.lowerEndpoint(), 
            affiliationRange.upperEndpoint());
    if (absences.isEmpty()) {
      return certifications;
    }

    List<Certification> certificationList = byAbsences(absences);
    for (Certification c : certificationList) {
      certifications.put(c.aMapKey(), c);
    }

    return certifications;
  }

  
  /**
   * Produce una lista di Certification corrispondente alla lista delle assenze passate.
   */
  private List<Certification> byAbsences(List<Absence> absences) {
    List<Certification> certifications = Lists.newArrayList();

    Certification certification = null;
    LocalDate previousDate = null;
    String previousAbsenceCode = null;
    Integer dayBegin = null;

    for (Absence absence : absences) {
      //codici a uso interno li salto
      if (absence.getAbsenceType().isInternalUse()) {
        continue;
      }

      //Codice per attestati
      String absenceCodeToSend = absence.getAbsenceType().getCode().toUpperCase();
      if (absence.getAbsenceType().getCertificateCode() != null
          && !absence.getAbsenceType().getCertificateCode().trim().isEmpty()) {
        absenceCodeToSend = absence.getAbsenceType().getCertificateCode().toUpperCase();
      }

      // 1) Continua Assenza più giorni
      Integer dayEnd;
      if (previousDate != null && previousDate.plusDays(1).equals(absence.getPersonDay().getDate())
          && previousAbsenceCode.equals(absenceCodeToSend)) {
        dayEnd = absence.getPersonDay().getDate().getDayOfMonth();
        previousDate = absence.getPersonDay().getDate();
        certification.setContent(absenceCodeToSend + ";" + dayBegin + ";" + dayEnd);
        continue;
      }

      // 2) Fine Assenza più giorni
      if (previousDate != null) {

        certifications.add(certification);
        previousDate = null;
      }

      // 3) Nuova Assenza  
      dayBegin = absence.getPersonDay().getDate().getDayOfMonth();
      dayEnd = absence.getPersonDay().getDate().getDayOfMonth();
      previousDate = absence.getPersonDay().getDate();
      previousAbsenceCode = absenceCodeToSend;

      certification = new Certification();
      certification.setPerson(absence.getPersonDay().getPerson());
      certification.setYear(absence.getPersonDay().getDate().getYear());
      certification.setMonth(absence.getPersonDay().getDate().getMonthOfYear());
      certification.setCertificationType(CertificationType.ABSENCE);
      certification.setContent(Certification.serializeAbsences(absenceCodeToSend,
          dayBegin, dayEnd));
    }

    certifications.add(certification);

    return certifications;
  }

  /**
   * Metodo che compone la certificazione aggiungendo le competenze.
   *
   * @param person la persona di cui si cercano le competenze
   * @param year l'anno
   * @param month il mese
   * @param certifications le certificazioni già presenti
   * @return certificazione (sotto forma di mappa).
   */
  private Map<String, Certification> competences(Person person, int year, int month,
      Map<String, Certification> certifications) {

    List<Competence> competences = competenceDao
        .getCompetenceInMonthForUploadSituation(person, year, month, 
            Optional.<CompetenceCodeGroup>absent());

    for (Competence competence : competences) {
      Certification certification = byCompetence(competence);
      certifications.put(certification.aMapKey(), certification);
    }

    return certifications;
  }

  private Map<Person, Map<String, Certification>> competences(
      List<Person> persons, int year, int month,
      final Map<Person, Map<String, Certification>> certifications) {

    Map<Person, List<Competence>> competencesMap = 
        competenceDao.competencesInMonth(persons, year, month);

    competencesMap.keySet().stream().forEach(person -> {
      competencesMap.get(person).forEach(competence -> {
        Certification certification = byCompetence(competence);
        certifications.get(person).put(certification.aMapKey(), certification);
      });
    });
    return certifications;
  }

  private static Certification byCompetence(Competence competence) {
    Certification certification = new Certification();
    certification.setPerson(competence.getPerson());
    certification.setYear(competence.getYear());
    certification.setMonth(competence.getMonth());
    certification.setCertificationType(CertificationType.COMPETENCE);
    certification.setContent(Certification.serializeCompetences(competence.getCompetenceCode()
        .getCode(), competence.getValueApproved()));
    return certification;
  }

  /**
   * Produce la certificazione buoni pasto della persona.
   *
   * @param person persona
   * @param year anno
   * @param month mese
   * @return certification (sotto forma di mappa)
   */
  private Map<String, Certification> mealTicket(Person person, int year, int month,
      Map<String, Certification> certifications, Range<LocalDate> affiliationRange) {

    Certification certification = new Certification();

    certification.setPerson(person);
    certification.setYear(year);
    certification.setMonth(month);
    certification.setCertificationType(CertificationType.MEAL);
    //Inserire qui il conteggio dei buoni pasto

    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate end = begin.dayOfMonth().withMaximumValue();

    int mealTickets = 0;
    //Caso di più di un'afferenza nell'anno/mese
    if (Range.closed(begin, end).encloses(affiliationRange) 
        && (!begin.isEqual(affiliationRange.lowerEndpoint()) 
            || !end.isEqual(affiliationRange.upperEndpoint()))) {
      Optional<PersonsOffices> po = personOfficeDao.affiliationByPeriod(person, 
          affiliationRange.lowerEndpoint(), affiliationRange.upperEndpoint());
      PersonStampingRecap psDto = 
          stampingsRecapFactory.create(person, year, month, true, 
              Optional.absent());  
      for (PersonStampingDayRecap recap : psDto.daysRecap) {
        if (affiliationRange.contains(recap.personDay.getDate()) && recap.personDay.isTicketAvailable()) {
          mealTickets ++;
        }

      }
      BlockType blockType = null;
      final java.util.Optional<Configuration> conf = 
          po.get().office.getConfigurations().stream()
          .filter(configuration -> 
          configuration.getEpasParam() == EpasParam.MEAL_TICKET_BLOCK_TYPE).findFirst();
      if (conf.isPresent()) {
        blockType = blockType.valueOf(conf.get().getFieldValue());
        switch (blockType) {
          case electronic:
            certification.setContent(String.valueOf(0) + ";" + String.valueOf(mealTickets));           
            break;
          case papery:
            certification.setContent(String.valueOf(mealTickets) + ";" + String.valueOf(0));
            break;
          default:
            break;
        }        
        certifications.put(certification.aMapKey(), certification);
      }

    } else {
      List<Contract> contractList = contractDao
          .getActiveContractsInPeriod(person, begin, Optional.of(end));
      YearMonth yearMonth = new YearMonth(year, month);
      int buoniCartacei = 0;
      int buoniElettronici = 0;
      for (Contract contract : contractList) {
        ContractMonthRecap monthRecap = contractMonthRecapDao
            .getContractMonthRecap(contract, yearMonth);
        if (monthRecap == null) {
          log.info("ContractMonthRecap non presente nel mese {}/{} per {} ( id = {} )",
              month, year, person.getFullname(), person.id);
        } else {
          MealTicketRecap recap = mealTicketService.create(contract).orNull();
          MealTicketComposition composition = mealTicketService
              .whichBlock(recap, monthRecap, contract);
          buoniCartacei = buoniCartacei + composition.paperyMealTicket;
          buoniElettronici = buoniElettronici + composition.electronicMealTicket;
        }
      }
      certification.setContent(
          String.valueOf(buoniCartacei) + ";" + String.valueOf(buoniElettronici));
      log.trace("certification = {}, certification.key = {}", 
          certification, certification.aMapKey());

      certifications.put(certification.aMapKey(), certification);
    }

    return certifications;
  }

  /**
   * Lista dei buoni pasto consegnati in un intervallo temporale, odrinati per
   * data di consegna descrescente.
   */
  public static List<BlockMealTicket> getBlockMealTicketReceivedDeliveryDesc(
      List<MealTicket> deliveryOrderedDesc, DateInterval mealTicketInverval) {
    return MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        deliveryOrderedDesc,
        Optional.fromNullable(mealTicketInverval));
  }

  Map<Person, Map<String, Certification>> mealTickets(
      List<Person> persons, int year, int month,
      final Map<Person, Map<String, Certification>> certifications) {
    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate end = begin.dayOfMonth().withMaximumValue();
    val mealTicketMap = mealTicketDao.getNumberOfMealTicketAccrued(persons, begin, end);
    mealTicketMap.keySet().stream().forEach(person -> {
      Certification certification = new Certification();
      certification.setPerson(person);
      certification.setYear(year);
      certification.setMonth(month);
      certification.setCertificationType(CertificationType.MEAL);
      final java.util.Optional<Configuration> conf =
          person.getOffice(new LocalDate(year, month, 1)).get().getConfigurations().stream()
          .filter(configuration -> 
          configuration.getEpasParam() == EpasParam.MEAL_TICKET_BLOCK_TYPE).findFirst();
      int buoniCartacei = 0;
      int buoniElettronici = 0;
      if (conf.isPresent()) {
        BlockType blockType = null;
        try { 
          blockType = BlockType.valueOf(conf.get().getFieldValue()); 
        } catch (Exception e) {
          blockType = BlockType.electronic;
        }
        switch (blockType) {
          case electronic:
            buoniElettronici = mealTicketMap.get(person);
            break;
          case papery:
            buoniCartacei = mealTicketMap.get(person);
            break;
          default:
            //log.warn("Errore nel parsing dell'enumerato per il tipo di blocchetto. Verificare.");
            break;
        }
      }
      certification.setContent(
          String.valueOf(buoniCartacei) + ";" + String.valueOf(buoniElettronici));
      certifications.get(person).put(certification.aMapKey(), certification);
    });
    return certifications;
  }

}


