package manager.attestati.service;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import manager.PersonDayManager;
import models.CertificatedData;
import models.Certification;
import models.Competence;
import models.CompetenceCodeGroup;
import models.Person;
import models.PersonMonthRecap;
import models.absences.Absence;
import models.enumerate.CertificationType;

public class PersonMonthlySituationData {

  private final PersonDayManager personDayManager;
  private final PersonMonthRecapDao personMonthRecapDao;
  private final AbsenceDao absenceDao;
  private final CompetenceDao competenceDao;
  private final PersonDayDao personDayDao;
  
  public PersonMonthlySituationData(PersonDayManager personDayManager,
      PersonMonthRecapDao personMonthRecapDao, AbsenceDao absenceDao,
      CompetenceDao competenceDao, PersonDayDao personDayDao) {
    this.personDayManager = personDayManager;
    this.personMonthRecapDao = personMonthRecapDao;
    this.absenceDao = absenceDao;
    this.competenceDao = competenceDao;
    this.personDayDao = personDayDao;
  }
  
  public Map<String, Certification> getCertification(Person person, int year, int month) {
        
    Map<String, Certification> actualCertifications = Maps.newHashMap();
    actualCertifications = trainingHours(person, year, month, actualCertifications);
    actualCertifications = absences(person, year, month, actualCertifications);
    actualCertifications = competences(person, year, month, actualCertifications);
    actualCertifications = mealTicket(person, year, month, actualCertifications);
    
    return actualCertifications;
  }
  
  
  /**
   * Produce le certification delle ore di formazione per la persona.
   *
   * @param person persona
   * @param year anno
   * @param month mese
   * @return certificazioni (sotto forma di mappa)
   */
  private Map<String, Certification> trainingHours(Person person, int year, int month,
      Map<String, Certification> certifications) {

    CertificatedData data = personMonthRecapDao.getPersonCertificatedData(person, month, year);
    if (data == null) {
      System.out.println("porcodio");
    }
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
   * @param year anno
   * @param month mese
   * @return certificazioni (sotto forma di mappa)
   */
  private Map<String, Certification> absences(Person person, int year, int month,
      Map<String, Certification> certifications) {

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

  /**
   * Metodo che compone la certificazione aggiungendo le competenze.
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
   * @param year anno
   * @param month mese
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

    certification.content = String.valueOf(mealTicket);

    certifications.put(certification.aMapKey(), certification);

    return certifications;
  }
}
