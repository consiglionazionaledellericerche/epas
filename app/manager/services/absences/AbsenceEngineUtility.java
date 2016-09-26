package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import manager.PersonDayManager;
import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsenceEngineRequest;
import manager.services.absences.model.AbsenceEngineScan;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.AbsencesReport;
import manager.services.absences.model.AbsencesReport.ReportImplementationProblem;
import manager.services.absences.model.AbsencesReport.ReportRequestProblem;
import manager.services.absences.model.DayStatus;
import manager.services.absences.model.TakenAbsence;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceTrouble;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceTrouble.ImplementationProblem;
import models.absences.AbsenceTrouble.RequestProblem;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;
import java.util.Map;
import java.util.Set;


public class AbsenceEngineUtility {
  
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonChildrenDao personChildrenDao;
  private final PersonDayManager personDayManager;
  
  private final Integer UNIT_REPLACING_AMOUNT = 1 * 100;

  @Inject
  public AbsenceEngineUtility(AbsenceComponentDao absenceComponentDao, 
      PersonChildrenDao personChildrenDao, PersonDayManager personDayManager) {
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.personDayManager = personDayManager;
  }
  
  public AbsenceEngine buildInsertAbsenceEngine(Person person, GroupAbsenceType groupAbsenceType,
      LocalDate from, LocalDate to) {
    
    AbsenceEngine absenceEngine = new AbsenceEngine(person, absenceComponentDao, this, personChildrenDao);

    AbsenceEngineRequest request = new AbsenceEngineRequest();
    request.absenceEngine = absenceEngine;
    request.absenceEngineUtility = this;
    request.group = groupAbsenceType;
    request.from = from;
    request.to = to;
    absenceEngine.request = request;
    
    absenceEngine.report = new AbsencesReport();
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {
      
      absenceEngine.report.addImplementationProblem(ReportImplementationProblem.builder()
          .implementationProblem(ImplementationProblem.UnimplementedTakableComplationGroup)
          .build());
      return absenceEngine;
    }
    
    absenceEngine.request.configureNextInsert();
   
    return absenceEngine;
  }
  
  /**
   * 
   * @param person
   * @param groupAbsenceType
   * @param from
   * @return
   */
  public AbsenceEngine buildResidualAbsenceEngine(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {
    
    AbsenceEngine absenceEngine = buildInsertAbsenceEngine(person, groupAbsenceType, date, null);
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {

      absenceEngine.report.addImplementationProblem(ReportImplementationProblem.builder()
          .implementationProblem(ImplementationProblem.UnimplementedTakableComplationGroup)
          .build());
      return absenceEngine;
    }

    absenceEngine.buildPeriodChain(groupAbsenceType, date);
    
    return absenceEngine;
    
  }

  /**
   * Costruttore per richiesta di scan.
   * @param absenceComponentDao
   * @param personChildrenDao
   * @param absenceEngineUtility
   * @param person
   * @param scanFrom
   * @param absencesToScan
   */
  public AbsenceEngine buildScanAbsenceEngine(AbsenceComponentDao absenceComponentDao, PersonChildrenDao personChildrenDao,
      AbsenceEngineUtility absenceEngineUtility, Person person, LocalDate scanFrom, List<Absence> absencesToScan) {
    
    AbsenceEngine absenceEngine = new AbsenceEngine(person, absenceComponentDao, this, personChildrenDao);
    
    absenceEngine.scan = new AbsenceEngineScan();
    absenceEngine.scan.absenceEngine = absenceEngine;
    absenceEngine.scan.absenceEngineUtility = absenceEngineUtility;
    absenceEngine.scan.scanFrom = scanFrom;
    absenceEngine.scan.absencesToScan = absencesToScan;
    for (Absence absence : absenceEngine.scan.absencesToScan) {
      Set<GroupAbsenceType> groupsToScan = absenceEngineUtility.involvedGroup(absence.absenceType); 
      absenceEngine.scan.absencesGroupsToScan.put(absence, groupsToScan);
    }
    
    absenceEngine.report = new AbsencesReport();
    
    return absenceEngine;
  }
  
  /**
   * Le operazioni univocamente identificabili dal justifiedType. Devo riuscire a derivare
   * l'assenza da inserire attraverso il justifiedType.
   *  Lista con priorità:
   *  - se esiste un solo codice allDay  -> lo metto tra le opzioni
   *  - se esiste un solo codice halfDay -> lo metto tra le opzioni
   *  - se esiste: un solo codice absence_type_minutes con Xminute
   *               un solo codice absence_type_minutes con Yminute
   *               ...
   *               un solo codice absence_type_minutes con Zminute
   *               un solo codice specifiedMinutes 
   *               -> metto specifiedMinutes tra le opzioni
   *  
   *  TODO: decidere come gestire il quanto manca               
   *                
   * @param groupAbsenceType
   * @return
   */
  public List<JustifiedType> automaticJustifiedType(GroupAbsenceType groupAbsenceType) {
    
    // TODO: gruppo ferie, riposi compensativi
    
    List<JustifiedType> justifiedTypes = Lists.newArrayList();
    
    //TODO: Copia che mi metto da parte... ma andrebbe cachata!!
    final JustifiedType specifiedMinutesVar = 
        absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    JustifiedType allDayVar = null;
    JustifiedType halfDayVar = null;

    Map<Integer, Integer> specificMinutesFinded = Maps.newHashMap(); //(minute, count)
    boolean specificMinutesDenied = false;
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
          Integer minuteKey = absenceType.justifiedTime;
          Integer count = specificMinutesFinded.get(minuteKey);
          if (count == null) {
            specificMinutesFinded.put(minuteKey, 1);
          } else {
            count++;
            if (count > 1) {
              specificMinutesDenied = true;
            }
            specificMinutesFinded.put(minuteKey, count);
          }
        }
      }
    }
    
    if (allDayFinded == 1) {
      justifiedTypes.add(allDayVar);
    }
    if (halfDayFinded == 1) {
      justifiedTypes.add(halfDayVar);
    }
    if (specifiedMinutesFinded == 1 && specificMinutesDenied == false) {
      justifiedTypes.add(specifiedMinutesVar);
    }
    
    return justifiedTypes;
  }

  /**
   * Quanto giustifica l'assenza passata.
   * Se non si riesce a stabilire il tempo giustificato si ritorna un numero negativo.
   * @param person
   * @param date
   * @param absence
   * @param amountType
   * @return
   */
  public int absenceJustifiedAmount(AbsenceEngine absenceEngine, Absence absence, 
      AmountType amountType) {
    
    int amount = 0;

    if (absence.justifiedType.name.equals(JustifiedTypeName.nothing)) {
      amount = 0;
    } 
    else if (absence.justifiedType.name.equals(JustifiedTypeName.all_day)) {
      amount = absenceEngine.workingTime(absence.getAbsenceDate());
    } 
    else if (absence.justifiedType.name.equals(JustifiedTypeName.half_day)) {
      amount = absenceEngine.workingTime(absence.getAbsenceDate()) / 2;
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.missing_time) ||
        absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
      // TODO: quello che manca va implementato. Occorre persistere la dacisione di quanto manca
      // se non si vogliono fare troppi calcoli.
      if (absence.justifiedMinutes == null) {
        amount = 0;
      } else {
        amount = absence.justifiedMinutes;
      }
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      amount = absence.absenceType.justifiedTime;
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) {
      amount = -1;
    }
    
    if (amountType.equals(AmountType.units)) {
      int work = absenceEngine.workingTime(absence.getAbsenceDate());
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
   * Quanto completa l'assenza passata.
   * Se non si riesce a stabilire il tempo di completamento si ritorna un numero negativo.
   * @param person
   * @param date
   * @param absence
   * @param amountType
   * @return
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
        return UNIT_REPLACING_AMOUNT; //una unità
    } 
    if (amountType.equals(AmountType.minutes) 
        && absenceType.replacingType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      return absenceType.replacingTime;
    }
    
    return -1;

  }
  
  /**
   * Prova a inferire l'absenceType dal gruppo e dal justifiedTime e specifiedMinutes
   * @param absencePeriod
   * @param absence
   * @return
   */
  public Absence inferAbsenceType(AbsencePeriod absencePeriod, Absence absence, 
      JustifiedType requestedJustifiedType) {

    if (requestedJustifiedType == null || !absencePeriod.isTakable()) {
      return absence;
    }
    
    // Controllo che il tipo sia inferibile
    if (!automaticJustifiedType(absencePeriod.groupAbsenceType).contains(requestedJustifiedType)) {
      return absence;
    }

    //Cerco il codice
    if (requestedJustifiedType.name.equals(JustifiedTypeName.all_day)) {
      for (AbsenceType absenceType : absencePeriod.takableCodes) { 
        if (absenceType.justifiedTypesPermitted.contains(requestedJustifiedType)) {
          absence.absenceType = absenceType;
          return absence;
        }
      }
    }
    if (requestedJustifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
      
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
   * 
   * @param hours
   * @param minutes
   * @return
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
   * 
   * @return
   */
  public Optional<AbsenceType> whichReplacingCode(AbsenceEngine absenceEngine, 
      AbsencePeriod absencePeriod, LocalDate date, int complationAmount) {
    
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
   * @param absenceType
   * @return
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
   * I vincoli generici.
   * @param absenceEngine
   * @param absence
   * @param allCodeAbsences
   * @return
   */
  public AbsenceEngine genericConstraints(AbsenceEngine absenceEngine, Absence absence, 
      List<Absence> allCodeAbsences) {
    
    //Codice non prendibile nei giorni di festa ed è festa.
    if (!absence.absenceType.consideredWeekEnd && personDayManager.isHoliday(absenceEngine.person,
        absence.getAbsenceDate())) {
      absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
          .trouble(AbsenceProblem.NotOnHoliday)
          .absence(absence).build());
    }
    
    //Un codice giornaliero già presente 
    for (Absence oldAbsence : allCodeAbsences) {
      //altra data
      if (!oldAbsence.getAbsenceDate().isEqual(absence.getAbsenceDate())) {
        continue;
      }
      //tempo giustificato non giornaliero
      if ((oldAbsence.justifiedType.name.equals(JustifiedTypeName.all_day) 
          || oldAbsence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) == false) {
        continue;
      }

      if (absenceEngine.isRequestEngine()) {
        DayStatus insertDayStatus = DayStatus.builder().date(absence.getAbsenceDate()).build();
        insertDayStatus.takenAbsences = Lists.newArrayList(TakenAbsence.builder()
            .absence(absence)
            .absenceProblem(AbsenceProblem.AllDayAlreadyExists).build());
        absenceEngine.report.addInsertDayStatus(insertDayStatus);
      }
      else if (absenceEngine.isScanEngine()) {
        if (oldAbsence.isPersistent() && absence.isPersistent() && oldAbsence.equals(absence)) {
          continue;
        }
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.AllDayAlreadyExists)
            .absence(absence).build());
      }

    }

     
    return absenceEngine;
  }
  
  /**
   * Verifica di errori che non dovrebbero mai accadere.
   * @param absenceEngine
   * @param absencePeriod
   * @param absence
   * @return
   */
  public AbsenceEngine requestConstraints(AbsenceEngine absenceEngine, AbsencePeriod absencePeriod, 
      Absence absence) {
    
    //Controllo integrità absenceType - justifiedType
    if (!absence.absenceType.justifiedTypesPermitted.contains(absence.justifiedType)) {
      absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
          .requestProblem(RequestProblem.WrongJustifiedType)
          .date(absenceEngine.request.currentDate)
          .build());
      return absenceEngine;
    }
    
    //Se è presente takableComponent allora deve essere un codice takable
    if (absencePeriod.isTakable()) {
      if (!absencePeriod.takableCodes.contains(absence.absenceType)) {
        absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
            .requestProblem(RequestProblem.CodeNotAllowedInGroup)
            .date(absence.getAbsenceDate()).build());
        return absenceEngine;
      }
    }
    
    //Se è presente solo complationComponent allora deve essere un codice complation
    if (!absencePeriod.isTakable() && absencePeriod.isComplation()) {
      
      if (!absencePeriod.complationCodes.contains(absence.absenceType)) { 
        absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
            .requestProblem(RequestProblem.CodeNotAllowedInGroup)
            .date(absence.getAbsenceDate())
            .build());
        return absenceEngine;
      }
    }
    
    return absenceEngine;
  }
  
  /**
   * 
   * FIXME: questi controlli si potrebbero eliminare rieseguendo la populate
   * con la nuova assenza!!! 
   * 
   * @param absenceEngine
   * @param absencePeriod
   * @param absence
   * @return
   */
  public AbsenceEngine groupConstraints(AbsenceEngine absenceEngine, 
      AbsencePeriod absencePeriod, Absence absence) {
    
    //Un codice di completamento e ne esiste già uno
    if (absencePeriod.isComplation()) {
      if (absencePeriod.complationCodes.contains(absence.absenceType) 
          && absencePeriod.complationAbsencesByDay.get(absence.getAbsenceDate()) != null) {
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.TwoComplationSameDay)
            .absence(absence)
            .build());
        return absenceEngine;
      }
    }

    //Takable limit
    if (absencePeriod.isTakable()) {
      
      int takenAmount = absenceJustifiedAmount(absenceEngine, absence, 
          absencePeriod.takeAmountType);
      
      if (!absencePeriod.canAddTakenAmount(takenAmount)) {
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.LimitExceeded)
            .absence(absence)
           .build());
      }
    }
    return absenceEngine;
  }
  
  
      
}
