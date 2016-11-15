package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.services.absences.errors.ErrorsBox;
import manager.services.absences.model.AbsencePeriod;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class AbsenceEngineUtility {
  
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonDayManager personDayManager;
  
  private final Integer unitReplacingAmount = 1 * 100;
  private final PersonReperibilityDayDao personReperibilityDayDao;
  private final PersonShiftDayDao personShiftDayDao;

  /**
   * Constructor for injection.
   * @param absenceComponentDao injected
   * @param personDayManager injected
   * @param personReperibilityDayDao injected
   * @param personShiftDayDao injected
   */
  @Inject
  public AbsenceEngineUtility(AbsenceComponentDao absenceComponentDao, 
      PersonDayManager personDayManager, 
      PersonReperibilityDayDao personReperibilityDayDao,
      PersonShiftDayDao personShiftDayDao ) {
    this.absenceComponentDao = absenceComponentDao;
    this.personDayManager = personDayManager;
    this.personReperibilityDayDao = personReperibilityDayDao;
    this.personShiftDayDao = personShiftDayDao;
  }

  
  /**
   * Le operazioni univocamente identificabili dal justifiedType. Devo riuscire a derivare
   * l'assenza da inserire attraverso il justifiedType.
   *  Lista con priorità:<br>
   *  - se esiste un solo codice allDay  -> lo metto tra le opzioni <br>
   *  - se esiste un solo codice halfDay -> lo metto tra le opzioni <br>
   *  - se esiste: un solo codice absence_type_minutes con Xminute <br>
   *               un solo codice absence_type_minutes con Yminute <br>
   *               ... <br>
   *               un solo codice absence_type_minutes con Zminute <br>
   *               un solo codice specifiedMinutes  <br>
   *               -> metto specifiedMinutes tra le opzioni <br>
   *  TODO: decidere come gestire il quanto manca               
   *                
   * @param groupAbsenceType gruppo
   * @return entity list
   */
  public List<JustifiedType> automaticJustifiedType(GroupAbsenceType groupAbsenceType) {
    
    // TODO: gruppo ferie, riposi compensativi
    if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.vacationsCnr)) {
      return Lists.newArrayList(absenceComponentDao
          .getOrBuildJustifiedType(JustifiedTypeName.all_day));
    }
    
    List<JustifiedType> justifiedTypes = Lists.newArrayList();
    
    //TODO: Copia che mi metto da parte... ma andrebbe cachata!!
    final JustifiedType specifiedMinutesVar = 
        absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    JustifiedType allDayVar = null;
    JustifiedType halfDayVar = null;

    //Map<Integer, Integer> specificMinutesFinded = Maps.newHashMap(); //(minute, count)
    //boolean specificMinutesDenied = false;
    Integer allDayFinded = 0;
    Integer halfDayFinded = 0;
    Integer specifiedMinutesFinded = 0;

    if (groupAbsenceType.takableAbsenceBehaviour == null) {
      return justifiedTypes;
    }
    
    for (AbsenceType absenceType : groupAbsenceType.takableAbsenceBehaviour.takableCodes) {
      for (JustifiedType justifiedType : absenceType.justifiedTypesPermitted) { 
        if (justifiedType.name.equals(JustifiedTypeName.all_day)) {
          allDayFinded++;
          allDayVar = justifiedType;
        }
        if (justifiedType.name.equals(JustifiedTypeName.half_day)) {
          halfDayFinded++;
          halfDayVar = justifiedType;
        }
        if (justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
          specifiedMinutesFinded++;
        }
        if (justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
          return Lists.newArrayList();
        }
      }
    }
    
    if (allDayFinded == 1) {
      justifiedTypes.add(allDayVar);
    }
    if (halfDayFinded == 1) {
      justifiedTypes.add(halfDayVar);
    }
    if (specifiedMinutesFinded == 1) { //&& specificMinutesDenied == false) {
      justifiedTypes.add(specifiedMinutesVar);
    }
    
    return justifiedTypes;
  }
  
  /**
   * Quanto giustifica l'assenza passata.
   * Se non si riesce a stabilire il tempo giustificato si ritorna un numero negativo.
   * @param person persona
   * @param absence assenza
   * @param amountType tipo di ammontare
   * @return tempo giustificato
   */
  public int absenceJustifiedAmount(Person person, Absence absence, AmountType amountType) {
    
    int amount = 0;

    if (absence.justifiedType.name.equals(JustifiedTypeName.nothing)) {
      amount = 0;
    } else if (absence.justifiedType.name.equals(JustifiedTypeName.all_day) 
        || absence.justifiedType.name.equals(JustifiedTypeName.all_day_limit)) {
      amount = absenceWorkingTime(person, absence);
    } else if (absence.justifiedType.name.equals(JustifiedTypeName.half_day)) {
      amount = absenceWorkingTime(person, absence) / 2;
    } else if (absence.justifiedType.name.equals(JustifiedTypeName.missing_time) 
        || absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes)
        || absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes_limit)) {
      // TODO: quello che manca va implementato. Occorre persistere la dacisione di quanto manca
      // se non si vogliono fare troppi calcoli.
      if (absence.justifiedMinutes == null) {
        amount = 0;
      } else {
        amount = absence.justifiedMinutes;
      }
    } else if (absence.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      amount = absence.absenceType.justifiedTime;
    } else if (absence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) {
      amount = -1;
    }
    
    
    if (amountType.equals(AmountType.units)) {
      int work = absenceWorkingTime(person, absence);
      if (work == -1) {
        //Patch: se è festa da verificare.
        if (absence.justifiedType.name.equals(JustifiedTypeName.all_day)) {
          return 100;
        }
      }
      if (work == 0) {
        return 0;
      }
      int result = amount * 100 / work;
      return result;
    } else {
      return amount;
    }
  }
  
  /**
   * Il tempo di lavoro nel giorno dell'assenza.<br>
   * @param person persona
   * @param absence assenza
   * @return tempo a lavoro assenza, -1 in caso di giorno contrattuale festivo
   */
  private int absenceWorkingTime(Person person, Absence absence) {
    LocalDate date = absence.getAbsenceDate();
    for (Contract contract : person.contracts) {
      for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {
        if (DateUtility.isDateIntoInterval(date, cwtt.periodInterval())) {
          if (cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek() - 1).holiday) {
            return -1;
          }
          return cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek() - 1)
              .workingTime;
        }
      }
    }
    return 0;
  }
  
  /**
   * Quanto completa il rimpiazzamento.
   * Se non si riesce a stabilire il tempo di completamento si ritorna un numero negativo.
   * @param absenceType tipo assenza
   * @param amountType tipo ammontare
   * @return ammontare
   */
  public int replacingAmount(AbsenceType absenceType, AmountType amountType) {

    //TODO: studiare meglio questa parte... 
    //Casi trattati:
    // 1) tipo completamento unità -> codice completamento un giorno
    //    ex:  89, 09B, 23H7, 25H7 
    // 2) tipo completamento minuti -> codice completamento minuti specificati assenza
    //    ex:  661H1C, 18H1C, 19H1C 

    if (absenceType == null) {
      return -1;
    }
    if (amountType.equals(AmountType.units) 
        && absenceType.replacingType.name.equals(JustifiedTypeName.all_day)) {
      return unitReplacingAmount; //una unità
    } 
    if (amountType.equals(AmountType.minutes) 
        && absenceType.replacingType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      return absenceType.replacingTime;
    }

    return -1;

  }
  
  /**
   * Prova a inferire l'absenceType dell'assenza all'interno del periodo.
   * @param absencePeriod periodo
   * @param absence assenza
   * @return assenza con tipo inferito
   */
  public Absence inferAbsenceType(AbsencePeriod absencePeriod, Absence absence) {

    if (absence.justifiedType == null || !absencePeriod.isTakable()) {
      return absence;
    }
    
    // Controllo che il tipo sia inferibile
    if (!automaticJustifiedType(absencePeriod.groupAbsenceType).contains(absence.justifiedType)) {
      return absence;
    }

    //Cerco il codice
    if (absence.justifiedType.name.equals(JustifiedTypeName.all_day)) {
      for (AbsenceType absenceType : absencePeriod.takableCodes) { 
        if (absenceType.justifiedTypesPermitted.contains(absence.justifiedType)) {
          absence.absenceType = absenceType;
          return absence;
        }
      }
    }
    if (absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
      
      AbsenceType specifiedMinutes = null;
      for (AbsenceType absenceType : absencePeriod.takableCodes) {
        for (JustifiedType absenceTypeJustifiedType : absenceType.justifiedTypesPermitted) {
          if (absenceTypeJustifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
            if (absence.justifiedMinutes != null) {
              absence.absenceType = absenceType;
              return absence; 
            }
            specifiedMinutes = absenceType;
          }
          if (absenceTypeJustifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
            if (absenceType.justifiedTime.equals(absence.justifiedMinutes)) { 
              absence.absenceType = absenceType;
              absence.justifiedType = absenceTypeJustifiedType;
              return absence;
            }
          }
        }
      }
      absence.absenceType = specifiedMinutes;
      return absence; 
    }
    // TODO: quanto manca?
    return absence;
  }
 
  /**
   * I minuti... .
   * @param hours ore
   * @param minutes minuti
   * @return minuti
   */
  public Integer getMinutes(Integer hours, Integer minutes) {
    Integer selectedSpecifiedMinutes = null;
    if (hours == null) {
      hours = 0;
    }
    if (minutes == null) {
      minutes = 0;
    }
    selectedSpecifiedMinutes = (hours * 60) + minutes; 
    
    return selectedSpecifiedMinutes;
  }
  
  /**
   * Quale rimpiazzamento inserire se aggiungo il complationAmount al period nella data. 
   * @return tipo del rimpiazzamento
   */
  public Optional<AbsenceType> whichReplacingCode(AbsencePeriod absencePeriod, 
      LocalDate date, int complationAmount) {
    
    for (Integer replacingTime : absencePeriod.replacingCodesDesc.keySet()) {
      int amountToCompare = replacingTime;
      if (amountToCompare <= complationAmount) {
        return Optional.of(absencePeriod.replacingCodesDesc.get(replacingTime));
      }
    }
    
    return Optional.absent();
  }
  
  /**
   * I gruppi coinvolti nel tipo di assenza.
   * @param absenceType tipo assenza
   * @return set gruppi
   */
  public Set<GroupAbsenceType> involvedGroup(AbsenceType absenceType) {
    Set<GroupAbsenceType> involvedGroup = Sets.newHashSet();
    for (TakableAbsenceBehaviour takableAbsenceBehaviour : absenceType.takableGroup) {
      involvedGroup.addAll(takableAbsenceBehaviour.groupAbsenceTypes);
    }
    for (TakableAbsenceBehaviour takableAbsenceBehaviour : absenceType.takenGroup) {
      involvedGroup.addAll(takableAbsenceBehaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour complationAbsenceBehaviour : absenceType.complationGroup) {
      involvedGroup.addAll(complationAbsenceBehaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour complationAbsenceBehaviour : absenceType.replacingGroup) {
      involvedGroup.addAll(complationAbsenceBehaviour.groupAbsenceTypes);
    }
    return involvedGroup;
  }
  
  /**
   * I vincoli generici assenza.
   * @param genericErrors box errori
   * @param person persona
   * @param absence assenza
   * @param allCodeAbsences tutti i codici che potrebbero conflittuare.
   * @return error box
   */
  public ErrorsBox genericConstraints(ErrorsBox genericErrors, 
      Person person, Absence absence, 
      Map<LocalDate, Set<Absence>> allCodeAbsences) {
    
    log.trace("L'assenza data={}, codice={} viene processata per i vincoli generici", 
        absence.getAbsenceDate(), absence.getAbsenceType().code);
    
    final boolean isHoliday = personDayManager.isHoliday(person, absence.getAbsenceDate());
    
    //Codice non prendibile nei giorni di festa ed è festa.
    if (!absence.absenceType.consideredWeekEnd && isHoliday) {
      genericErrors.addAbsenceError(absence, AbsenceProblem.NotOnHoliday);
    } else {
      //check sulla reperibilità
      if (personReperibilityDayDao
          .getPersonReperibilityDay(person, absence.getAbsenceDate()).isPresent()) {
        genericErrors.addAbsenceWarning(absence, AbsenceProblem.InReperibility); 
      }
      if (personShiftDayDao.getPersonShiftDay(person, absence.getAbsenceDate()).isPresent()) {
        genericErrors.addAbsenceWarning(absence, AbsenceProblem.InShift); 
      }
    }
    
    //Un codice giornaliero già presente 
    Set<Absence> dayAbsences = allCodeAbsences.get(absence.getAbsenceDate());
    if (dayAbsences == null) {
      dayAbsences = Sets.newHashSet();
    }
    for (Absence oldAbsence : dayAbsences) {
      //stessa entità
      if (oldAbsence.isPersistent() && absence.isPersistent() && oldAbsence.equals(absence)) {
        continue;
      }
      //altra data
      if (!oldAbsence.getAbsenceDate().isEqual(absence.getAbsenceDate())) {
        continue;
      }
      //tempo giustificato non giornaliero
      if ((oldAbsence.justifiedType.name.equals(JustifiedTypeName.all_day) 
          || oldAbsence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) == false) {
        continue;
      }
      genericErrors.addAbsenceError(absence, AbsenceProblem.AllDayAlreadyExists, oldAbsence);
    }

  
    //TODO:
    // Strange weekend
    
    // Configuration qualification grant
    
    // DayLimitGroupCode

     
    return genericErrors;
  }
  
      
}
