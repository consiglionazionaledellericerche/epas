package db.h2support.base;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Lists;

import dao.PersonDayDao;
import dao.absences.AbsenceComponentDao;

import java.util.List;
import java.util.Set;

import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;

import org.joda.time.LocalDate;

public class H2AbsenceSupport {
  
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonDayDao personDayDao;
  
  @Inject
  public H2AbsenceSupport(AbsenceComponentDao absenceComponentDao, PersonDayDao personDayDao) {
    this.absenceComponentDao = absenceComponentDao;
    this.personDayDao = personDayDao;
  }

  /**
   * Istanza di una assenza. Per adesso non persistita perchè ai fini dei test non mandatoria 
   * (ma lo sarà presto). Serve il personDay.
   *
   * @param defaultAbsenceType absenceType assenza
   * @param date                  data
   * @param justifiedTypeName     tipo giustificativo
   * @param justifiedMinutes      minuti giustificati
   * @return istanza non persistente
   */
  public Absence absenceInstance(DefaultAbsenceType defaultAbsenceType,
      LocalDate date, Optional<JustifiedTypeName> justifiedTypeName,
      Integer justifiedMinutes) {

    AbsenceType absenceType = absenceComponentDao
        .absenceTypeByCode(defaultAbsenceType.getCode()).get();
    JustifiedType justifiedType = null;
    if (justifiedTypeName.isPresent()) {
      justifiedType = absenceComponentDao.getOrBuildJustifiedType(justifiedTypeName.get());
    } else {
      Verify.verify(absenceType.getJustifiedTypesPermitted().size() == 1);
      justifiedType = absenceType.justifiedTypesPermitted.iterator().next();
    }
    Absence absence = new Absence();
    absence.date = date;
    absence.absenceType = absenceType;
    absence.justifiedType = justifiedType;
    absence.justifiedMinutes = justifiedMinutes;
    
    return absence;
  }
  
  /**
   * Istanza di una assenza. 
   *
   * @param defaultAbsenceType absenceType assenza
   * @param date                  data
   * @param justifiedTypeName     tipo giustificativo
   * @param justifiedMinutes      minuti giustificati
   * @return istanza non persistente
   */
  public Absence absence(DefaultAbsenceType defaultAbsenceType,
      LocalDate date, Optional<JustifiedTypeName> justifiedTypeName,
      Integer justifiedMinutes, Person person) {

    Absence absence = 
        absenceInstance(defaultAbsenceType, date, justifiedTypeName, justifiedMinutes);
    
    absence.personDay = getPersonDay(person, date);
    absence.personDay.refresh();
    absence.save();
    
    return absence;
  }

  /**
   * Il personDay della persona a quella data.
   * @param person persona
   * @param date data
   * @return personDay
   */
  public PersonDay getPersonDay(Person person, LocalDate date) {
    Optional<PersonDay> personDay = personDayDao.getPersonDay(person, date);
    if (personDay.isPresent()) {
      return personDay.get();
    }
    
    PersonDay newPersonDay = new PersonDay(person, date);
    newPersonDay.save();
    return newPersonDay;
  }
  
  /**
   * Costruzione multipla di assenze all day.
   * @param person persona
   * @param defaultAbsenceType tipo essere all day permitted
   * @param dates date
   * @return list
   */
  public List<Absence> multipleAllDayInstances(Person person, DefaultAbsenceType defaultAbsenceType,
      Set<LocalDate> dates) {
    
    List<Absence> absences = Lists.newArrayList();
    for (LocalDate date : dates) {
      absences
      .add(absence(defaultAbsenceType, date, Optional.of(JustifiedTypeName.all_day), 0, person));
    }
    
    return absences;
  }
  

}
