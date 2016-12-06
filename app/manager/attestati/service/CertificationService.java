package manager.attestati.service;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.CertificationDao;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.attestati.dto.show.CodiceAssenza;
import manager.attestati.dto.show.RigaAssenza;
import manager.attestati.dto.show.RigaCompetenza;
import manager.attestati.dto.show.RigaFormazione;
import manager.attestati.dto.show.RispostaAttestati;
import manager.attestati.dto.show.SeatCertification;
import manager.attestati.dto.show.SeatCertification.PersonCertification;

import models.Certification;
import models.Competence;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.absences.Absence;
import models.enumerate.CertificationType;

import org.assertj.core.util.Maps;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.libs.WS.HttpResponse;
import play.mvc.Http;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Funzionalità integrazione ePAS - Nuovo Attestati.
 *
 * @author alessandro
 */
@Slf4j
public class CertificationService implements ICertificationService {

  private final CertificationsComunication certificationsComunication;

  private final PersonMonthRecapDao personMonthRecapDao;
  private final PersonDayDao personDayDao;
  private final PersonDayManager personDayManager;
  private final CompetenceDao competenceDao;
  private final AbsenceDao absenceDao;
  private final CertificationDao certificationDao;


  @Inject
  public CertificationService(CertificationsComunication certificationsComunication,
      AbsenceDao absenceDao, CompetenceDao competenceDao, PersonMonthRecapDao personMonthRecapDao,
      PersonDayManager personDayManager, PersonDayDao personDayDao,
      CertificationDao certificationDao) {
    this.certificationsComunication = certificationsComunication;
    this.absenceDao = absenceDao;
    this.competenceDao = competenceDao;
    this.personDayManager = personDayManager;
    this.personDayDao = personDayDao;
    this.personMonthRecapDao = personMonthRecapDao;
    this.certificationDao = certificationDao;
  }

  /* (non-Javadoc)
   * @see manager.attestati.service.ICertificationService#authentication(models.Office, boolean)
   */
  @Override
  public boolean authentication(Office office, boolean result) {

    // TODO: chiedere a Pagano come discriminare il caso.

    return result;
  }

  /**
   * Le certificazioni già presenti su attestati.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return null in caso di errore.
   */
  private Map<String, Certification> personAttestatiCertifications(Person person,
      int year, int month, PersonCertification personCertification) {

    Map<String, Certification> certifications = Maps.newHashMap();

    // Assenze accettate
    for (RigaAssenza rigaAssenza : personCertification.righeAssenza) {

      Certification certification = new Certification();
      certification.person = person;
      certification.year = year;
      certification.month = month;
      certification.certificationType = CertificationType.ABSENCE;
      certification.content = rigaAssenza.serializeContent();
      certification.attestatiId = rigaAssenza.id;

      certifications.put(certification.aMapKey(), certification);
    }

    // Competenze accettate
    for (RigaCompetenza rigaCompetenza : personCertification.righeCompetenza) {

      Certification certification = new Certification();
      certification.person = person;
      certification.year = year;
      certification.month = month;
      certification.certificationType = CertificationType.COMPETENCE;
      certification.content = rigaCompetenza.serializeContent();
      certification.attestatiId = rigaCompetenza.id;

      certifications.put(certification.aMapKey(), certification);
    }

    // Formazioni accettate
    for (RigaFormazione rigaFormazione : personCertification.righeFormazione) {

      Certification certification = new Certification();
      certification.person = person;
      certification.year = year;
      certification.month = month;
      certification.certificationType = CertificationType.FORMATION;
      certification.content = rigaFormazione.serializeContent();
      certification.attestatiId = rigaFormazione.id;

      certifications.put(certification.aMapKey(), certification);
    }

    // Buoni pasto
    Certification certification = new Certification();
    certification.person = person;
    certification.year = year;
    certification.month = month;
    certification.certificationType = CertificationType.MEAL;
    certification.content = personCertification.numBuoniPasto + "";

    certifications.put(certification.aMapKey(), certification);

    return certifications;
  }

  /* (non-Javadoc)
   * @see manager.attestati.service.ICertificationService#buildPersonStaticStatus(models.Person, int, int)
   */
  @Override
  public PersonCertData buildPersonStaticStatus(Person person, int year, int month)
      throws ExecutionException {

    PersonCertData personCertData = new PersonCertData();
    personCertData.person = person;
    personCertData.year = year;
    personCertData.month = month;

    // Le certificazioni in attestati e lo stato di validazione ...
    Map<String, Certification> attestatiCertifications = Maps.newHashMap();

    Optional<SeatCertification> seatCertification = certificationsComunication
        .getPersonSeatCertification(person, month, year);
    if (seatCertification.isPresent()) {
      PersonCertification personCertification = seatCertification.get().dipendenti.get(0);
      attestatiCertifications =
          personAttestatiCertifications(person, year, month, personCertification);
      if (attestatiCertifications == null) {
        log.info("Impossibile scaricare le informazioni da attestati per {}", person.getFullname());
        //attestatiCertifications = Maps.newHashMap(); TODO: da segnalare in qualche modo all'user 
      }
      personCertData.validate = personCertification.validato;
    }

    // Le certificazioni in epas
    Map<String, Certification> epasCertifications = Maps.newHashMap();
    for (Certification certification : certificationDao.personCertifications(person, year, month)) {
      epasCertifications.put(certification.aMapKey(), certification);
    }

    // Lo stato attuale epas
    Map<String, Certification> actualCertifications = Maps.newHashMap();
    actualCertifications = trainingHours(person, year, month, actualCertifications);
    actualCertifications = absences(person, year, month, actualCertifications);
    actualCertifications = competences(person, year, month, actualCertifications);
    actualCertifications = mealTicket(person, year, month, actualCertifications);

    if (attestatiCertifications != null) {
      // Riesco a scaricare gli attestati della persona
      if (certificationsEquivalent(attestatiCertifications, actualCertifications)) {
        // Stato attuale equivalente ad attestati
        epasCertifications = updateEpasCertifications(epasCertifications, attestatiCertifications);
        personCertData.okProcessable = true;
        personCertData.epasCertifications = epasCertifications;
      } else {
        // Stato attuale non equivalente ad attestati        
        personCertData.incompleteProcessable = true;
        personCertData.actualCertifications = actualCertifications;
        personCertData.epasCertifications = epasCertifications;
        personCertData.attestatiCertifications = attestatiCertifications;
      }
    } else {
      // Non Riesco a scaricare gli attestati della persona
      if (certificationsEquivalent(actualCertifications, epasCertifications)) {
        // Ultimo invio corretto
        personCertData.okNotProcessable = true;
        personCertData.epasCertifications = epasCertifications;
      } else {
        // Ultimo invio con problemi o obsoleto        
        personCertData.incompleteNotProcessable = true;
        personCertData.actualCertifications = actualCertifications;
        personCertData.epasCertifications = epasCertifications;
      }
    }

    personCertData.computeStaticStatus();
    return personCertData;
  }

  private Map<String, Certification> updateEpasCertifications(
      Map<String, Certification> epasCertifications,
      Map<String, Certification> attestatiCertifications) {

    Set<String> allKey = Sets.newHashSet();
    allKey.addAll(epasCertifications.keySet());
    allKey.addAll(attestatiCertifications.keySet());


    for (String key : allKey) {

      Certification epasCertification = epasCertifications.get(key);
      Certification attestatiCertification = attestatiCertifications.get(key);

      if (epasCertification == null) {
        attestatiCertification.warnings = "Master Attestati";
        attestatiCertification.save();
        epasCertifications.put(key, attestatiCertification);
        continue;
      }

      if (attestatiCertification == null) {
        log.info("Rimossa certifications obsoleta. {}", epasCertification);
        epasCertification.delete();
        epasCertifications.remove(key);
        continue;
      }

      if (epasCertification.containProblems()) {
        epasCertification.problems = null;
        epasCertification.warnings = "Problems fixed by Attestati";
        epasCertification.save();
      }

      if (epasCertification.attestatiId == null
          || !epasCertification.attestatiId.equals(attestatiCertification.attestatiId)) {
        epasCertification.attestatiId = attestatiCertification.attestatiId;
        epasCertification.save();
      }

    }

    return epasCertifications;

  }

  /* (non-Javadoc)
   * @see manager.attestati.service.ICertificationService#certificationsEquivalent(java.util.Map, java.util.Map)
   */
  @Override
  public boolean certificationsEquivalent(Map<String, Certification> map1,
      Map<String, Certification> map2) {

    Set<String> allKey = Sets.newHashSet();
    allKey.addAll(map1.keySet());
    allKey.addAll(map2.keySet());

    for (String key : allKey) {

      Certification certification1 = map1.get(key);
      Certification certification2 = map2.get(key);
      if (certification1 == null || certification2 == null) {
        return false;
      }
      if (certification1.problems != null && !certification1.problems.isEmpty()) {
        return false;
      }
      if (certification2.problems != null && !certification2.problems.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /* (non-Javadoc)
   * @see manager.attestati.service.ICertificationService#process(manager.attestati.service.PersonCertData)
   */
  // TODO Questa parte andrebbe resa più semplice perchè per trasmettere le informazioni
  // ad attestati sono costretto ad avere un PersonCertData che è il risultato
  // ottenuto dal metodo buildPersonStaticStatus il quale a sua volta effettua una richiesta
  // ad attestati per il recupero delle informazioni della persona
  @Override
  public PersonCertData process(PersonCertData personCertData)
      throws ExecutionException, NoSuchFieldException {

    personCertData.staticView = false;

    // Da cancellare
    Map<String, Certification> notErasable = Maps.newHashMap();
    for (Certification certification : personCertData.toDeleteCertifications.values()) {
      if (!removeAttestati(certification)) {
        notErasable.put(certification.aMapKey(), certification);
      }
    }
    personCertData.toDeleteCertifications = notErasable;

    // Le certificaioni che avevano problemi provo a reinviarle.
    List<Certification> sended = Lists.newArrayList();
    Map<String, Certification> containProblemCertifications = Maps.newHashMap();
    for (Certification certification : personCertData.problemCertifications.values()) {
      if (sendCertification(certification) == null) {
        //Quando non riesco ad inviare la certificazione rimane dovè.
        containProblemCertifications.put(certification.aMapKey(), certification);
      } else {
        if (certification.containProblems()) {
          containProblemCertifications.put(certification.aMapKey(), certification);
          certification.save();
        } else {
          sended.add(certification);
          certification.save();
        }
      }
    }

    // Da inviare
    Map<String, Certification> notSended = Maps.newHashMap();
    for (Certification certification : personCertData.toSendCertifications.values()) {
      if (sendCertification(certification) == null) {
        // Quando non riesco ad inviare la certificazione rimane dovè.
        notSended.put(certification.aMapKey(), certification);
      } else {
        if (certification.containProblems()) {
          containProblemCertifications.put(certification.aMapKey(), certification);
        } else {
          sended.add(certification);
        }
        certification.save();
      }
    }

    personCertData.problemCertifications = containProblemCertifications;
    personCertData.toSendCertifications = notSended;

    for (Certification certification : sended) {
      personCertData.correctCertifications.put(certification.aMapKey(), certification);
    }

    personCertData.computeProcessStatus();

    return personCertData;

  }


  /* (non-Javadoc)
   * @see manager.attestati.service.ICertificationService#sendCertification(models.Certification)
   */
  @Override
  public Certification sendCertification(Certification certification) {

    try {
      HttpResponse httpResponse;
      Optional<RispostaAttestati> rispostaAttestati;

      if (certification.certificationType == CertificationType.ABSENCE) {
        httpResponse = certificationsComunication.sendRigaAssenza(certification);
        rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

      } else if (certification.certificationType == CertificationType.FORMATION) {
        httpResponse = certificationsComunication.sendRigaFormazione(certification);
        rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

      } else if (certification.certificationType == CertificationType.MEAL) {
        httpResponse = certificationsComunication.sendRigaBuoniPasto(certification, false);
        rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

        if (rispostaAttestati.isPresent()
            && rispostaAttestati.get().message.contains("attestato_buoni_pasto_ukey")) {
          httpResponse = certificationsComunication.sendRigaBuoniPasto(certification, true);
          rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
        }

      } else if (certification.certificationType == CertificationType.COMPETENCE) {
        httpResponse = certificationsComunication.sendRigaCompetenza(certification);
        rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

      } else {
        return null;

      }

      // Esito 
      if (httpResponse.getStatus() == 200) {
        certification.problems = "";
      } else if (httpResponse.getStatus() == 500) {

        if (rispostaAttestati.isPresent()) {
          certification.problems = rispostaAttestati.get().message;
        } else {
          certification.problems = "Errore interno al server";
        }
      } else {
        if (rispostaAttestati.isPresent()) {
          certification.problems = rispostaAttestati.get().message;
        } else {
          certification.problems = "Impossibile prelevare l'esito dell'invio. Riprovare.";
        }
      }

      return certification;

    } catch (Exception ex) {
      log.error(ex.toString());
      return null;
    }
  }

  /* (non-Javadoc)
   * @see manager.attestati.service.ICertificationService#removeAttestati(models.Certification)
   */
  @Override
  public boolean removeAttestati(Certification certification)
      throws ExecutionException, NoSuchFieldException {

    HttpResponse httpResponse;
    Optional<RispostaAttestati> rispostaAttestati;

    if (certification.certificationType == CertificationType.ABSENCE) {
      httpResponse = certificationsComunication.deleteRigaAssenza(certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

    } else if (certification.certificationType == CertificationType.FORMATION) {
      httpResponse = certificationsComunication.deleteRigaFormazione(certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

    } else if (certification.certificationType == CertificationType.COMPETENCE) {
      httpResponse = certificationsComunication.deleteRigaCompetenza(certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

    } else if (certification.certificationType == CertificationType.MEAL) {
      certification.content = "0";
      httpResponse = certificationsComunication.sendRigaBuoniPasto(certification, false);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
      if (rispostaAttestati.isPresent()
          && rispostaAttestati.get().message.contains("attestato_buoni_pasto_ukey")) {
        httpResponse = certificationsComunication.sendRigaBuoniPasto(certification, true);
        rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
      }
    } else {
      return false;
    }

    // Esito 
    return httpResponse.getStatus() == Http.StatusCode.OK;

  }

  /**
   * Produce le certification delle ore di formazione per la persona.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return certificazioni (sotto forma di mappa)
   */
  private Map<String, Certification> trainingHours(Person person, int year, int month,
      Map<String, Certification> certifications) {

    List<PersonMonthRecap> trainingHoursList = personMonthRecapDao
        .getPersonMonthRecapInYearOrWithMoreDetails(person, year,
            Optional.fromNullable(month), Optional.absent());
    for (PersonMonthRecap personMonthRecap : trainingHoursList) {

      // Nuova certificazione
      Certification certification = new Certification();
      certification.person = person;
      certification.year = year;
      certification.month = month;
      certification.certificationType = CertificationType.FORMATION;
      certification.content = Certification
          .serializeTrainingHours(personMonthRecap.fromDate.getDayOfMonth(),
              personMonthRecap.toDate.getDayOfMonth(), personMonthRecap.trainingHours);

      certifications.put(certification.aMapKey(), certification);
    }

    return certifications;
  }


  /**
   * Produce le certification delle assenze per la persona.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return certificazioni (sotto forma di mappa)
   */
  private Map<String, Certification> absences(Person person, int year, int month,
      Map<String, Certification> certifications) {

//    log.info("Persona {}", person);

    List<Absence> absences = absenceDao
        .getAbsencesNotInternalUseInMonth(person, year, month);
    if (absences.isEmpty()) {
      return certifications;
    }

    Certification certification = null;
    LocalDate previousDate = null;
    String previousAbsenceCode = null;
    Integer dayBegin = null;

    for (Absence absence : absences) {

      //codici a uso interno li salto
      if (absence.absenceType.internalUse) {
        continue;
      }

      //Codice per attestati
      String absenceCodeToSend = absence.absenceType.code.toUpperCase();
      if (absence.absenceType.certificateCode != null
          && !absence.absenceType.certificateCode.trim().isEmpty()) {
        absenceCodeToSend = absence.absenceType.certificateCode.toUpperCase();
      }

      // 1) Continua Assenza più giorni
      Integer dayEnd;
      if (previousDate != null && previousDate.plusDays(1).equals(absence.personDay.date)
          && previousAbsenceCode.equals(absenceCodeToSend)) {
        dayEnd = absence.personDay.date.getDayOfMonth();
        previousDate = absence.personDay.date;
        certification.content = absenceCodeToSend + ";" + dayBegin + ";" + dayEnd;
        continue;
      }

      // 2) Fine Assenza più giorni
      if (previousDate != null) {

        certifications.put(certification.aMapKey(), certification);
        previousDate = null;
      }

      // 3) Nuova Assenza  
      dayBegin = absence.personDay.date.getDayOfMonth();
      dayEnd = absence.personDay.date.getDayOfMonth();
      previousDate = absence.personDay.date;
      previousAbsenceCode = absenceCodeToSend;

      certification = new Certification();
      certification.person = person;
      certification.year = year;
      certification.month = month;
      certification.certificationType = CertificationType.ABSENCE;
      certification.content = Certification.serializeAbsences(absenceCodeToSend,
          dayBegin, dayEnd);
    }

    certifications.put(certification.aMapKey(), certification);

    return certifications;
  }

  private Map<String, Certification> competences(Person person, int year, int month,
      Map<String, Certification> certifications) {

    List<Competence> competences = competenceDao
        .getCompetenceInMonthForUploadSituation(person, year, month);

    for (Competence competence : competences) {
      Certification certification = new Certification();
      certification.person = person;
      certification.year = year;
      certification.month = month;
      certification.certificationType = CertificationType.COMPETENCE;
      certification.content = Certification.serializeCompetences(competence.competenceCode.code,
          competence.valueApproved);

      certifications.put(certification.aMapKey(), certification);
    }

    return certifications;
  }

  /**
   * Produce la certificazione buoni pasto della persona.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return certification (sotto forma di mappa)
   */
  private Map<String, Certification> mealTicket(Person person, int year, int month,
      Map<String, Certification> certifications) {

    Certification certification = new Certification();
    certification.person = person;
    certification.year = year;
    certification.month = month;
    certification.certificationType = CertificationType.MEAL;

    Integer mealTicket = personDayManager.numberOfMealTicketToUse(personDayDao
        .getPersonDayInMonth(person, new YearMonth(year, month)));

    certification.content = mealTicket + "";

    certifications.put(certification.aMapKey(), certification);

    return certifications;
  }

  /* (non-Javadoc)
   * @see manager.attestati.service.ICertificationService#emptyAttestati(manager.attestati.service.PersonCertData)
   */
  @Override
  public PersonCertData emptyAttestati(
      PersonCertData personCertData)
      throws ExecutionException, NoSuchFieldException {

    if (personCertData.attestatiCertifications != null) {
      for (Certification certification :
          personCertData.attestatiCertifications.values()) {
        if (certification.attestatiId != null
            || certification.certificationType == CertificationType.MEAL) {
          removeAttestati(certification);
        }
      }
    }

    if (personCertData.epasCertifications != null) {
      for (Certification certification : personCertData.epasCertifications.values()) {
        if (certification.attestatiId != null
            || certification.certificationType == CertificationType.MEAL) {
          removeAttestati(certification);
        }
      }
    }

    if (personCertData.actualCertifications != null) {
      for (Certification certification : personCertData.actualCertifications.values()) {
        if (certification.attestatiId != null
            || certification.certificationType == CertificationType.MEAL) {
          removeAttestati(certification);
        }
      }
    }

    return personCertData;
  }

  /* (non-Javadoc)
   * @see manager.attestati.service.ICertificationService#absenceCodes()
   */
  @Override
  public Map<String, CodiceAssenza> absenceCodes() throws ExecutionException {

    List<CodiceAssenza> codiciAssenza = certificationsComunication.getAbsencesList();
    Map<String, CodiceAssenza> map = Maps.newConcurrentHashMap();
    for (CodiceAssenza codiceAssenza : codiciAssenza) {
      map.put(codiceAssenza.codice.trim().toUpperCase(), codiceAssenza);
    }
    return map;
  }


}
