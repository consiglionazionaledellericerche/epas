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
import manager.attestati.dto.show.RigaAssenza;
import manager.attestati.dto.show.RigaCompetenza;
import manager.attestati.dto.show.RigaFormazione;
import manager.attestati.dto.show.RispostaAttestati;
import manager.attestati.dto.show.SeatCertification;
import manager.attestati.dto.show.SeatCertification.PersonCertification;

import models.Absence;
import models.Certification;
import models.Competence;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.enumerate.CertificationType;

import org.assertj.core.util.Maps;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.libs.WS.HttpResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Funzionalità integrazione ePAS - Nuovo Attestati.
 * @author alessandro
 *
 */
@Slf4j
public class CertificationService {
  
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
      PersonDayManager personDayManager, PersonDayDao personDayDao, CertificationDao certificationDao) {
    this.certificationsComunication = certificationsComunication;
    this.absenceDao = absenceDao;
    this.competenceDao = competenceDao;
    this.personDayManager = personDayManager;
    this.personDayDao = personDayDao;
    this.personMonthRecapDao = personMonthRecapDao;
    this.certificationDao = certificationDao;
  }
  
  /**
   * Ritorna il token di comunicazione.
   * @return
   */
  public Optional<String> buildToken() {
    return certificationsComunication.getToken();
  }
  
  /**
   * Se il token è abilitato alla sede.
   * @param token
   * @return
   */
  public boolean authentication(Office office, Optional<String> token, boolean result) {
    
    // TODO: chiedere a Pagano come discriminare il caso.
    
    return result;
  }
  
  /**
   * Le matricole abilitate all'invio attestati per la sede nel mese.
   * Nota bene: se la lista è vuota significa che non è stato effettuato lo stralcio oppure
   * un errore nel protocollo di comunicazione con attestati. 
   * Es. Periodo 201603 non presente per la sede 224500. 
   * @param office
   * @param year
   * @param month
   * @param token
   * @return
   */
  public Set<Integer> peopleList(Office office, int year, int month, Optional<String> token) {
    
    return certificationsComunication.getPeopleList(office, year, month, token);
    
  }

  /**
   * Le certificazioni già presenti su attestati. 
   * @param person
   * @param year
   * @param month
   * @param token
   * @return null in caso di errore.
   */
  private Map<String, Certification> personAttestatiCertifications(Person person, 
      int year, int month, Optional<String> token) {
    
    Optional<SeatCertification> seatCertification = certificationsComunication
        .getPersonSeatCertification(person, month, year, token);
    
    if (!seatCertification.isPresent()) {
      return null;
    }
    PersonCertification personCertification = seatCertification.get().dipendenti.get(0);
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
  
  /**
   * 
   * @param person
   * @param year
   * @param month
   * @param numbers
   * @param token
   * @return
   */
  public PersonCertificationStatus buildPersonStaticStatus(Person person, int year, int month,
      Set<Integer> numbers, Optional<String> token) {
    
    PersonCertificationStatus personCertificationStatus = new PersonCertificationStatus();
    personCertificationStatus.person = person;
    personCertificationStatus.year = year;
    personCertificationStatus.month = month;

    // Esco perchè finchè non sistemo la matricola non ha senso fare altro.
    if (person.number == null) {
      personCertificationStatus.notInAttestati = true;
      return personCertificationStatus;
    } else {
      if (!numbers.contains(person.number)) {
        personCertificationStatus.notInAttestati = true;;
        return personCertificationStatus;
      }
    }

    // Le certificazioni in attestati.
    Map<String, Certification> attestatiCertifications = 
        personAttestatiCertifications(person, year, month, token);
    if (attestatiCertifications == null) {
      log.info("Impossibile scaricare le informazioni da attestati per {}", person.getFullname());
      //attestatiCertifications = Maps.newHashMap(); //TODO: da segnalare in qualche modo all'user 
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
        personCertificationStatus.okProcessable = true;
        personCertificationStatus.epasCertifications = epasCertifications;
      } else {
        // Stato attuale non equivalente ad attestati        
        personCertificationStatus.incompleteProcessable = true;
        personCertificationStatus.actualCertifications = actualCertifications;
        personCertificationStatus.epasCertifications = epasCertifications;
        personCertificationStatus.attestatiCertifications = attestatiCertifications;
      }
    } else {
      // Non Riesco a scaricare gli attestati della persona
      if (certificationsEquivalent(actualCertifications, epasCertifications)) {
        // Ultimo invio corretto
        personCertificationStatus.okNotProcessable = true;
        personCertificationStatus.epasCertifications = epasCertifications;
      } else {
        // Ultimo invio con problemi o obsoleto        
        personCertificationStatus.incompleteNotProcessable = true;
        personCertificationStatus.actualCertifications = actualCertifications;
        personCertificationStatus.epasCertifications = epasCertifications;
      }
    }
    
    personCertificationStatus.computeStaticStatus();
    return personCertificationStatus;
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
        log.info("Rimossa certifications obsoleta. {}", epasCertification.toString());
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
          || epasCertification.attestatiId != attestatiCertification.attestatiId)
      epasCertification.attestatiId = attestatiCertification.attestatiId;
      epasCertification.save();

    }
    
    return epasCertifications;
    
  }

  /**
   * Se le due mappe contententi certificazioni sono equivalenti e non contengono errori.
   * @param map1
   * @param map2
   * @return
   */
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
  
  public PersonCertificationStatus process(PersonCertificationStatus personCertificationStatus, 
      Optional<String> token) {
    
    personCertificationStatus.staticView = false;
    
    // Da cancellare
    Map<String, Certification> notErasable = Maps.newHashMap();
    for (Certification certification : personCertificationStatus.toDeleteCertifications.values()) {
      if (!removeAttestati(certification, token)) {
        notErasable.put(certification.aMapKey(), certification);
      }
    }
    personCertificationStatus.toDeleteCertifications = notErasable;
    
    // Le certificaioni che avevano problemi provo a reinviarle.
    List<Certification> sended = Lists.newArrayList();
    Map<String, Certification> containProblemCertifications = Maps.newHashMap();
    for (Certification certification : personCertificationStatus.problemCertifications.values()) {
      if (sendCertification(certification, token) == null) {
        //Quando non riesco ad inviare la certificazione rimane dovè.
        containProblemCertifications.put(certification.aMapKey(), certification);
      } else {
        if (!certification.containProblems()) {
          sended.add(certification);
          certification.save();  
        } else {
          containProblemCertifications.put(certification.aMapKey(), certification);
          certification.save();  
        }
      }
    }
    
    // Da inviare
    Map<String, Certification> notSended = Maps.newHashMap();
    for (Certification certification : personCertificationStatus.toSendCertifications.values()) {
      if (sendCertification(certification, token) == null) {
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
    
    personCertificationStatus.problemCertifications = containProblemCertifications;
    personCertificationStatus.toSendCertifications = notSended;
    
    for (Certification certification : sended) {
      personCertificationStatus.correctCertifications.put(certification.aMapKey(), certification);
    }
    
    personCertificationStatus.computeProcessStatus();
    
    return personCertificationStatus;
    
  }


  /**
   * Invia la certificazione ad attestati.
   * @param certification
   * @param token
   * @return
   */
  public Certification sendCertification(Certification certification, Optional<String> token) {

    try {
    HttpResponse httpResponse;
    RispostaAttestati rispostaAttestati;

    if (certification.certificationType.equals(CertificationType.ABSENCE)) {
      httpResponse = certificationsComunication.sendRigaAssenza(token, certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
      
    } else if (certification.certificationType.equals(CertificationType.FORMATION)) {
      httpResponse = certificationsComunication.sendRigaFormazione(token, certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
      
    } else if (certification.certificationType.equals(CertificationType.MEAL)) {
      httpResponse = certificationsComunication.sendRigaBuoniPasto(token, certification, false);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
      if (rispostaAttestati.message.contains("attestato_buoni_pasto_ukey")) {
        httpResponse = certificationsComunication.sendRigaBuoniPasto(token, certification, true);
        rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
      }
      
    } else if (certification.certificationType.equals(CertificationType.COMPETENCE)) {
      httpResponse = certificationsComunication.sendRigaCompetenza(token, certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

    } else {
      return null;

    }

    // Esito 
    if (httpResponse.getStatus() == 200) {
      certification.problems = "";
    } else if (httpResponse.getStatus() == 500) {
      //certification.problems = "Errore interno al server";
      certification.problems = rispostaAttestati.message;
    } else {
      certification.problems = rispostaAttestati.message;
    }

    return certification;
    
    } catch (Exception e) {
      log.error(e.toString());
      return null;
    }
  }

  /**
   * Rimuove il record in attestati. (Non usare per buoni pasto).
   * @param certification
   * @param token
   * @return
   */
  public boolean removeAttestati(Certification certification, Optional<String> token) {
    
    HttpResponse httpResponse;
    RispostaAttestati rispostaAttestati;

    if (certification.certificationType.equals(CertificationType.ABSENCE)) {
      httpResponse = certificationsComunication.deleteRigaAssenza(token, certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
      
    } else if (certification.certificationType.equals(CertificationType.FORMATION)) {
      httpResponse = certificationsComunication.deleteRigaFormazione(token, certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);
      
    } else if (certification.certificationType.equals(CertificationType.COMPETENCE)) {
      httpResponse = certificationsComunication.deleteRigaCompetenza(token, certification);
      rispostaAttestati = certificationsComunication.parseRispostaAttestati(httpResponse);

    } else {
      throw new IllegalStateException("I record buoni pasto non si eliminano.");
    }

    // Esito 
    if (httpResponse.getStatus() == 200) {
      return true;
    }

    return false;
  }
  
  /**
   * Produce le certification delle ore di formazione per la persona.
   * @param person
   * @param year
   * @param month
   * @return
   */
  private Map<String, Certification> trainingHours(Person person, int year, int month, 
      Map<String, Certification> certifications) {
 
    List<PersonMonthRecap> trainingHoursList = personMonthRecapDao
        .getPersonMonthRecapInYearOrWithMoreDetails(person, year, 
            Optional.fromNullable(month), Optional.<Boolean>absent());
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
   * @param person
   * @param year
   * @param month
   * @return
   */
  private Map<String, Certification> absences(Person person, int year, int month,
      Map<String, Certification> certifications) {
    
    log.info("Persona {}", person);

    List<Absence> absences = absenceDao
        .getAbsencesNotInternalUseInMonth(person, year, month);
    if (absences.isEmpty()) {
      return certifications;
    }

    Certification certification = null;
    LocalDate previousDate = null;
    String previousAbsenceCode = null;
    Integer dayBegin = null;
    Integer dayEnd = null;

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
      dayBegin =  absence.personDay.date.getDayOfMonth();
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
   * @param person
   * @param year
   * @param month
   * @return
   */
  private Map<String, Certification> mealTicket(Person person, int year, int month,
      Map<String, Certification> certifications) {
    
    Integer mealTicket = personDayManager.numberOfMealTicketToUse(personDayDao
        .getPersonDayInMonth(person, new YearMonth(year, month)));
    
    Certification certification = new Certification();
    certification.person = person;
    certification.year = year;
    certification.month = month;
    certification.certificationType = CertificationType.MEAL;
    
    certification.content = mealTicket + "";
 
    certifications.put(certification.aMapKey(), certification);
    
    return certifications;
  }



}
