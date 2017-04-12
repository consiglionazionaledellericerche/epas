package db.h2support.base;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import db.h2support.base.AbsenceDefinitions.AbsenceTypeDefinition;
import db.h2support.base.AbsenceDefinitions.ComplationBehaviourDefinition;
import db.h2support.base.AbsenceDefinitions.GroupAbsenceTypeDefinition;
import db.h2support.base.AbsenceDefinitions.TakableBehaviourDefinition;

import java.util.Set;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;

import org.joda.time.LocalDate;

public class H2AbsenceSupport {
  
  private final AbsenceComponentDao absenceComponentDao;
  
  @Inject
  public H2AbsenceSupport(AbsenceComponentDao absenceComponentDao) {
    this.absenceComponentDao = absenceComponentDao;
  }

  /**
   * Costruisce e persiste una istanza del tipo di assenza secondo definizione.
   * @param absenceTypeDefinition definizione 
   * @return persisted entity
   */
  public AbsenceType createAbsenceType(AbsenceTypeDefinition absenceTypeDefinition) {

    AbsenceType absenceType = new AbsenceType();
    absenceType.code = absenceTypeDefinition.name();
    absenceType.justifiedTime = absenceTypeDefinition.justifiedTime;
    absenceType.consideredWeekEnd = absenceTypeDefinition.consideredWeekEnd;
    absenceType.timeForMealTicket = absenceTypeDefinition.timeForMealTicket;

    absenceTypeDefinition.justifiedTypeNamesPermitted.forEach(name -> 
        absenceType.justifiedTypesPermitted.add(absenceComponentDao.getOrBuildJustifiedType(name)));

    absenceType.replacingType = absenceComponentDao
        .getOrBuildJustifiedType(absenceTypeDefinition.replacingType);
    absenceType.replacingTime = absenceTypeDefinition.replacingTime;
    absenceType.save();
    return absenceType;
  }
  
  /**
   * Preleva dal db l'istanza con quella definizione.
   * @param definition definizione
   * @return entity
   */
  public AbsenceType getAbsenceType(AbsenceTypeDefinition definition) {
    Optional<AbsenceType> absenceType = absenceComponentDao.absenceTypeByCode(definition.name()); 
    if (absenceType.isPresent()) {
      return absenceType.get();
    } 
    return createAbsenceType(definition);
  }
  
  /**
   * Preleva dal db le istanze con quelle definizioni.
   * @param absenceTypeDefinitions definizioni
   * @return entities
   */
  public Set<AbsenceType> getAbsenceTypes(Set<AbsenceTypeDefinition> absenceTypeDefinitions) {
    
    Set<AbsenceType> absenceTypes = Sets.newHashSet();
    for (AbsenceTypeDefinition definition : absenceTypeDefinitions) {
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
      TakableBehaviourDefinition takableDefinition) {

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
      ComplationBehaviourDefinition complationDefinition) {

    ComplationAbsenceBehaviour behaviour = new ComplationAbsenceBehaviour();
    behaviour.name = complationDefinition.name();
    behaviour.amountType = complationDefinition.amountType;
    behaviour.complationCodes = getAbsenceTypes(complationDefinition.complationCodes);
    behaviour.replacingCodes = getAbsenceTypes(complationDefinition.replacingCodes);
    return behaviour;
  }

  /**
   * Preleva dal db l'istanza con quella definizione.
   * @param groupDefinition definizione
   * @return entity
   */
  public GroupAbsenceType getGroupAbsenceType(GroupAbsenceTypeDefinition groupDefinition) {

    if (groupDefinition == null) {
      return null;
    }
    
    GroupAbsenceType group = new GroupAbsenceType();
    group.name = groupDefinition.name();
    group.pattern = groupDefinition.pattern;
    group.periodType = groupDefinition.periodType;
    group.takableAbsenceBehaviour = 
        getTakableAbsenceBehaviour(groupDefinition.takableAbsenceBehaviour);
    group.complationAbsenceBehaviour = 
        getComplationAbsenceBehaviour(groupDefinition.complationAbsenceBehaviour);
    group.nextGroupToCheck = getGroupAbsenceType(groupDefinition.next);
    
    return group;
  }
  
  /**
   * Istanza di una assenza. Per adesso non persistita perchè ai fini dei test non mandatoria 
   * (ma lo sarà presto). Serve il personDay.
   *
   * @param absenceTypeDefinition absenceType assenza
   * @param date                  data
   * @param justifiedTypeName     tipo giustificativo
   * @param justifiedMinutes      minuti giustificati
   * @return istanza non persistente
   */
  public Absence absenceInstance(AbsenceTypeDefinition absenceTypeDefinition,
      LocalDate date, Optional<JustifiedTypeName> justifiedTypeName,
      Integer justifiedMinutes) {

    AbsenceType absenceType = getAbsenceType(absenceTypeDefinition);
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

}
