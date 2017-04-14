package db.h2support.base;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.PersonDayDao;
import dao.absences.AbsenceComponentDao;

import java.util.Set;

import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AbsenceType.DefaultAbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.ComplationAbsenceBehaviour.DefaultComplation;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.DefaultGroup;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.DefaultTakable;

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
   * Costruisce e persiste una istanza del tipo di assenza secondo definizione.
   * @param defaultAbsenceType definizione 
   * @return persisted entity
   */
  public AbsenceType createAbsenceType(DefaultAbsenceType defaultAbsenceType) {

    AbsenceType absenceType = new AbsenceType();
    absenceType.code = defaultAbsenceType.name().substring(2);
    absenceType.justifiedTime = defaultAbsenceType.justifiedTime;
    absenceType.consideredWeekEnd = defaultAbsenceType.consideredWeekEnd;
    absenceType.timeForMealTicket = defaultAbsenceType.timeForMealTicket;

    defaultAbsenceType.justifiedTypeNamesPermitted.forEach(name -> 
        absenceType.justifiedTypesPermitted.add(absenceComponentDao.getOrBuildJustifiedType(name)));

    if (defaultAbsenceType.replacingType != null) {
      absenceType.replacingType = absenceComponentDao
          .getOrBuildJustifiedType(defaultAbsenceType.replacingType);
    }
    absenceType.replacingTime = defaultAbsenceType.replacingTime;
    absenceType.save();
    return absenceType;
  }
  
  /**
   * Preleva dal db l'istanza con quella definizione.
   * @param defaultAbsenceType definizione
   * @return entity
   */
  public AbsenceType getAbsenceType(DefaultAbsenceType defaultAbsenceType) {
    Optional<AbsenceType> absenceType = absenceComponentDao
        .absenceTypeByCode(defaultAbsenceType.name().substring(2)); 
    if (absenceType.isPresent()) {
      return absenceType.get();
    } 
    return createAbsenceType(defaultAbsenceType);
  }
  
  /**
   * Preleva dal db le istanze con quelle definizioni.
   * @param absenceTypeDefinitions definizioni
   * @return entities
   */
  public Set<AbsenceType> getAbsenceTypes(Set<DefaultAbsenceType> absenceTypeDefinitions) {
    
    Set<AbsenceType> absenceTypes = Sets.newHashSet();
    for (DefaultAbsenceType definition : absenceTypeDefinitions) {
      Optional<AbsenceType> absenceType = absenceComponentDao.absenceTypeByCode(definition.name()); 
      if (absenceType.isPresent()) {
        absenceTypes.add(absenceType.get());
      } else {
        absenceTypes.add(createAbsenceType(definition));
      }
    }
    return absenceTypes;
  }

  /**
   * Preleva dal db l'istanza con quella definizione.
   * @param takableDefinition definizione
   * @return entity
   */
  public TakableAbsenceBehaviour getTakableAbsenceBehaviour(
      DefaultTakable takableDefinition) {

    TakableAbsenceBehaviour behaviour = new TakableAbsenceBehaviour();
    behaviour.name = takableDefinition.name();
    behaviour.amountType = takableDefinition.amountType;
    behaviour.takenCodes = getAbsenceTypes(takableDefinition.takenCodes);
    behaviour.takableCodes = getAbsenceTypes(takableDefinition.takableCodes);
    behaviour.fixedLimit = takableDefinition.fixedLimit;
    behaviour.takableAmountAdjustment = takableDefinition.takableAmountAdjustment;
    behaviour.save();
    return behaviour;
  }

  /**
   * Preleva dal db l'istanza con quella definizione.
   * @param complationDefinition definizione
   * @return entity
   */
  public ComplationAbsenceBehaviour getComplationAbsenceBehaviour(
      DefaultComplation complationDefinition) {

    if (complationDefinition == null) {
      return null;
    }
    ComplationAbsenceBehaviour behaviour = new ComplationAbsenceBehaviour();
    behaviour.name = complationDefinition.name();
    behaviour.amountType = complationDefinition.amountType;
    behaviour.complationCodes = getAbsenceTypes(complationDefinition.complationCodes);
    behaviour.replacingCodes = getAbsenceTypes(complationDefinition.replacingCodes);
    return behaviour;
  }

  /**
   * Preleva dal db l'istanza con quella definizione.
   * @param defaultGroup definizione
   * @return entity
   */
  public GroupAbsenceType getGroupAbsenceType(DefaultGroup defaultGroup) {

    if (defaultGroup == null) {
      return null;
    }
    
    GroupAbsenceType group = new GroupAbsenceType();
    group.name = defaultGroup.name();
    group.pattern = defaultGroup.pattern;
    group.periodType = defaultGroup.periodType;
    group.takableAbsenceBehaviour = 
        getTakableAbsenceBehaviour(defaultGroup.takable);
    group.complationAbsenceBehaviour = 
        getComplationAbsenceBehaviour(defaultGroup.complation);
    group.nextGroupToCheck = getGroupAbsenceType(defaultGroup.nextGroupToCheck);
    
    return group;
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

    AbsenceType absenceType = getAbsenceType(defaultAbsenceType);
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
   * Istanza di una assenza. Per adesso non persistita perchè ai fini dei test non mandatoria 
   * (ma lo sarà presto). Serve il personDay.
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
  

}
