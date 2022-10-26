/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package controllers.rest.v2;

import cnr.sync.dto.v2.CertificationAbsenceDto;
import cnr.sync.dto.v2.CertificationCompetencesDto;
import cnr.sync.dto.v2.CertificationDto;
import cnr.sync.dto.v2.CertificationMealTicketDto;
import cnr.sync.dto.v2.CertificationTrainingHoursDto;
import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v3.OfficeMonthValidationStatusDto;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.AbsenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.CertificationManager;
import manager.attestati.service.PersonMonthlySituationData;
import models.Certification;
import models.Office;
import models.Person;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per consultazione via REST dei dati dei riepiloghi mensili.
 */
@Slf4j
@With(Resecure.class)
public class Certifications extends Controller {

  @Inject
  static PersonMonthlySituationData monthData;
  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static WorkingTimeTypeDao workingTimeTypeDao;
  @Inject
  static CertificationManager certificationManager;
  @Inject
  static IWrapperFactory wrapperFactory;

  /**
   * Metodo rest che ritorna la lista dello stato di invio al sistema
   * di gestione degli attestati mensili (attestati per il CNR).
   */
  public static void getMonthValidationStatusByOffice(
      String sedeId, Integer year, Integer month) {
    log.debug("getMonthValidationStatus -> sedeId={}, year={}, month={}", sedeId, year, month);
    if (year == null || month == null || sedeId == null) {
      JsonResponse.badRequest("I parametri sedeId, year e month sono tutti obbligatori");
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      JsonResponse.notFound("Office non trovato con il sedeId passato per parametro");
    }
    rules.checkIfPermitted(office.get());
    
    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    final List<Person> people = personDao.list(Optional.absent(),
        Sets.newHashSet(office.get()), false, monthBegin, monthEnd, true).list();
    val validationStatus = new OfficeMonthValidationStatusDto();
    people.stream().forEach(person -> {
      val certData = 
          certificationManager.getPersonCertData(person, year, month);
      if (certData.validate) {
        validationStatus.getValidatedPersons().add(PersonShowTerseDto.build(person));
      } else {
        validationStatus.getNotValidatedPersons().add(PersonShowTerseDto.build(person));
      }
    });
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(validationStatus));  
  }

  /**
   * Metodo rest che ritorna le informazioni di validazione degli attestati mensili 
   * di una dipendente nell'anno/mese passati come parametro.
   */
  public static void getMonthValidationStatusByPerson(Long id, String email, String eppn, 
      Long personPerseoId, String fiscalCode, String number, Integer year, Integer month) {
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    if (year == null || month == null) {
      JsonResponse.badRequest("I parametri year e month sono entrambi obbligatori");
    }
    rules.checkIfPermitted(person.getOffice());
    val certData = certificationManager.getPersonCertData(person, year, month);
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(certData.validate));
  }

  /**
   * Metodo rest che permette di ritornare una lista contenente le informazioni mensili
   * del dipendente (assenze, competenze, ore di formazione, buoni pasto).
   *
   * @param email l'indirizzo mail della persona
   * @param eppn il campo eppn della persona
   * @param personPersoId l'identificativo anagrafico della persona
   * @param year l'anno
   * @param month il mese
   */
  @BasicAuth
  public static void getMonthSituation(
      Long id, String email, String eppn, Long personPersoId, String fiscalCode,
      String number, int year, int month) {

    log.debug("Richieste informazioni mensili da applicazione esterna");
    Optional<Person> person = 
        personDao.byIdOrEppnOrEmailOrPerseoIdOrFiscalCodeOrNumber(
            id, eppn, email, personPersoId, fiscalCode, number);

    if (!person.isPresent()) {
      log.info("Non trovata la persona in base ai parametri passati: "
          + "email = {}, eppn = {}, personPersoId = {}", email, eppn, personPersoId);
      
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente in ePAS la "
          + "mail che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.get().getOffice());

    Map<String, Certification> 
      map = monthData.getCertification(person.get(), year, month);
    CertificationDto dto = generateCertDto(map, year, month, person.get());

    val wrapperPerson = wrapperFactory.create(person.get());
    List<IWrapperContractMonthRecap> contractMonthRecaps = wrapperPerson.getWrapperContractMonthRecaps(new YearMonth(year, month));
    dto.setMealTicketsPreviousMonth(
        contractMonthRecaps.stream().mapToInt(
            cm -> cm.getValue().getBuoniPastoDalMesePrecedente()).reduce(0, Integer::sum));
    dto.setRemainingMealTickets(
        contractMonthRecaps.stream().mapToInt(
            cm -> cm.getValue().getRemainingMealTickets()).reduce(0, Integer::sum));

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(dto));

  }

  /**
   * Metodo che ritorna la lista degli oggetti contenenti le info mensili per la generazione
   * delle buste paga verso sistemi esterni per tutti i dipendenti della sede identificata
   * da sedeId nell'anno year e nel mese month.
   *
   * @param sedeId l'identificativo della sede
   * @param year l'anno
   * @param month il mese
   */
  public static void getMonthSituationByOffice(String sedeId, int year, int month) {
    log.debug("Richieste informazioni mensili da applicazione esterna per sedeId={} {}/{}",
        sedeId, year, month);
    if (sedeId == null) {
      JsonResponse.badRequest("Il parametro sedeId e' obbligatorio");
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      JsonResponse.notFound(
          String.format("Ufficio con sedeId = %s non trovato", sedeId));
    }
    rules.checkIfPermitted(office.get()); 
    Set<Office> offices = Sets.newHashSet();
    offices.add(office.get());
    LocalDate start = new LocalDate(year, month, 1);
    LocalDate end = start.dayOfMonth().withMaximumValue();
    List<CertificationDto> list = Lists.newArrayList();
    List<Person> personList = personDao
        .listFetched(Optional.<String>absent(), offices, false, start, end, true).list();
    for (Person person : personList) {
      log.debug("analizzo la situazione mensile di {}...", person.getFullname());
      Map<String, Certification> map = 
          monthData.getCertification(person, year, month);
      CertificationDto dto = generateCertDto(map, year, month, person);
      list.add(dto);
      log.debug("...analizzata la situazione mensile di {}", person.getFullname());
    }
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(list));
  }

  /**
   * Metodo privato che permette la generazione di un dto contenente informazioni
   * mensili del dipendente.
   *
   * @param map la mappa contenente le informazioni mensili da rielaborare
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param person la persona per cui cercare le informazioni
   * @return il dto contenente le informazioni da inviare al chiamante del servizio rest.
   */   
  private static CertificationDto generateCertDto(Map<String, Certification> map, 
      int year, int month, Person person) {
    
    List<CertificationCompetencesDto> competences = Lists.newArrayList();
    List<CertificationMealTicketDto> mealTickets = Lists.newArrayList();
    List<CertificationTrainingHoursDto> trainingHours = Lists.newArrayList();

    LocalDate from;
    LocalDate to;
    String[] places;
    List<CertificationAbsenceDto> absences = searchAbsences(person, year, month);

    for (Map.Entry<String, Certification> entry : map.entrySet()) {
      switch (entry.getValue().getCertificationType()) {
        case ABSENCE:
          break;
        case COMPETENCE:
          places = entry.getValue().getContent().split(";");
          CertificationCompetencesDto competence = CertificationCompetencesDto.builder()
              .code(places[0])
              .quantity(Integer.parseInt(places[1]))
              .build();
          competences.add(competence);
          break;
        case MEAL:
          CertificationMealTicketDto meal = CertificationMealTicketDto.builder()
              .quantity(Integer.parseInt(entry.getValue().getContent().split(";")[0]) 
                  + Integer.parseInt(entry.getValue().getContent().split(";")[1]))
              .paperyTickets(Integer.parseInt(entry.getValue().getContent().split(";")[0]))
              .electronicTickets(Integer.parseInt(entry.getValue().getContent().split(";")[1]))
              .build();
          mealTickets.add(meal);
          break;
        case FORMATION:
          places = entry.getValue().getContent().split(";");
          from = new LocalDate(year, month, Integer.parseInt(places[0]));
          to = new LocalDate(year, month, Integer.parseInt(places[1]));
          CertificationTrainingHoursDto trainingHour = CertificationTrainingHoursDto.builder()
              .from(from)
              .to(to)
              .quantity(Integer.parseInt(places[2]))
              .build();
          trainingHours.add(trainingHour);
          break;
        default:
          break;
      }
    }
    CertificationDto obj = CertificationDto.builder()
        .fullName(person.getFullname())
        .number(person.getNumber())
        .year(year)
        .month(month)
        .absences(absences)
        .competences(competences)
        .mealTickets(mealTickets)
        .trainingHours(trainingHours)
        .build();
    return obj;
  }

  /**
   * Metodo privato per la ricerca delle assenze.
   *
   * @param person la persona di cui cercare le assenze
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return la lista di dto contenente la informazioni sulle assenze nell'anno/mese fatte 
   *     dalla persona.
   */
  private static List<CertificationAbsenceDto> searchAbsences(Person person, int year, int month) {
    List<CertificationAbsenceDto> absences = Lists.newArrayList();
    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate end = begin.dayOfMonth().withMaximumValue();

    List<Absence> absencesPlus = absenceDao
        .getAbsenceWithNoHInMonth(person, begin, end);
    LocalDate previousDate = null;
    String previousAbsenceCode = null;
    Integer dayBegin = null;
    Integer dayEnd = null;
    Integer timeToJustify = null;
    String justifiedType = "";
    for (Absence abs : absencesPlus) {

      String absenceCodeToSend = abs.getAbsenceType().getCode().toUpperCase();      
      if (previousDate != null && previousDate.plusDays(1).equals(abs.getPersonDay().getDate())
          && previousAbsenceCode.equals(absenceCodeToSend)) {
        dayEnd = abs.getPersonDay().getDate().getDayOfMonth();
        previousDate = abs.getPersonDay().getDate();        
        continue;
      }
      // 2) Fine Assenza più giorni
      if (previousDate != null) {
        
        CertificationAbsenceDto absence = CertificationAbsenceDto.builder()
            .code(previousAbsenceCode)
            .justifiedTime(timeToJustify)
            .justifiedType(justifiedType)
            .from(new LocalDate(year, month, dayBegin))
            .to(new LocalDate(year, month, dayEnd))
            .build();
        absences.add(absence);
        previousDate = null;
      }

      // 3) Nuova Assenza
      dayBegin = abs.getPersonDay().getDate().getDayOfMonth();
      dayEnd = abs.getPersonDay().getDate().getDayOfMonth();
      previousDate = abs.getPersonDay().getDate();
      previousAbsenceCode = absenceCodeToSend;
      timeToJustify = abs.getJustifiedMinutes();

      Optional<WorkingTimeTypeDay> workingTimeTypeDay = 
          workingTimeTypeDao.getWorkingTimeTypeDay(abs.getPersonDay().getDate(), person);

      if (workingTimeTypeDay.isPresent()) {
        if (abs.getJustifiedType().getName().equals(JustifiedTypeName.all_day) 
            || abs.getJustifiedType().getName().equals(JustifiedTypeName.assign_all_day)) {
          timeToJustify = workingTimeTypeDay.get().getWorkingTime();
        }
        if (abs.getJustifiedType().getName().equals(JustifiedTypeName.complete_day_and_add_overtime)) {
          timeToJustify = workingTimeTypeDay.get().getWorkingTime() - abs.getPersonDay().getStampingsTime();
        }
      } else {
        log.warn("Il workingTimeTypeDay per il giorno {} non è presente ma è "
            + "presente l'assenza {}", abs.getPersonDay().getDate(), abs, person.getFullname());
      }

      if (abs.getJustifiedType().getName().equals(JustifiedTypeName.absence_type_minutes)) {
        timeToJustify = abs.getAbsenceType().getJustifiedTime();
      }
      justifiedType = abs.getJustifiedType().getLabel();

    }
    if (!absencesPlus.isEmpty()) {
      CertificationAbsenceDto absence = CertificationAbsenceDto.builder()
          .code(previousAbsenceCode)
          .justifiedTime(timeToJustify)
          .justifiedType(justifiedType)
          .from(new LocalDate(year, month, dayBegin))
          .to(new LocalDate(year, month, dayEnd))
          .build();
      absences.add(absence);   
    }

    return absences;
  }
}