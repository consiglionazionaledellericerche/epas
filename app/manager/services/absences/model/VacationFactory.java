package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.services.absences.model.YearProgression.YearPortion;

import models.Contract;
import models.Office;
import models.Person;
import models.VacationPeriod;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.InitializationGroup;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

@Slf4j
public class VacationFactory {
  
  private final ConfigurationManager configurationManager;
  private final AbsenceComponentDao absenceComponentDao;

  /**
   * Costruttore. 
   */
  @Inject
  public VacationFactory(ConfigurationManager configurationManager, 
      AbsenceComponentDao absenceComponentDao) {
    this.configurationManager = configurationManager;
    this.absenceComponentDao = absenceComponentDao;
  }
  
  /**
   * La periodChain che riduce il problema delle ferie e permessi alla prendibilità di assenze.
   * @param person persona 
   * @param group gruppo
   * @param fetchedContract i contratti
   * @param date la data di maturazione??
   * @return la periodChain.
   */
  public PeriodChain buildVacationChain(Person person, GroupAbsenceType group, 
      List<Contract> fetchedContract, LocalDate date) {
    
    Contract contract = null;
    //creare i period dai contract.vacationperiod
    for (Contract con : fetchedContract) {
      if (DateUtility.isDateIntoInterval(date, con.periodInterval())) {
        contract = con;
      }
    }
    
    int year = date.getYear();
    
    int initializationLastYear = 0;
    int initializationCurrentYear = 0;
    int initializationPermission = 0;
    if (contract.sourceDateResidual != null) {
      if (contract.sourceDateResidual.getYear() == year) {
        initializationLastYear = contract.sourceVacationLastYearUsed;
        initializationCurrentYear = contract.sourceVacationCurrentYearUsed;
        initializationPermission = contract.sourcePermissionUsed;
      } else if (contract.sourceDateResidual.getYear() == year - 1) {
        initializationLastYear = contract.sourceVacationCurrentYearUsed; 
      } else if (contract.sourceDateResidual.getYear() == year + 1) {
        initializationCurrentYear = contract.sourceVacationLastYearUsed;
      }
    }
    
    List<AbsencePeriod> vacationLastYear = null;
    List<AbsencePeriod> permission  = null;
    List<AbsencePeriod> vacationCurrentYear = null;
    
    if (group.name.equals(DefaultGroup.FERIE_CNR.name())) {
      //se il gruppo è vacation i codici posso anche prenderli.
      vacationLastYear = vacationPeriodPerYear(person, group, year - 1, contract, 
          initializationLastYear, false);
      permission = permissionPeriodPerYear(person, group, year, contract,  
          initializationPermission);
      vacationCurrentYear = vacationPeriodPerYear(person, group, year, contract,
          initializationCurrentYear, false);
    } else {
      //se il gruppo è il prorogation i codici non posso prenderli.
      vacationLastYear = vacationPeriodPerYear(person, group, year - 1, contract,
          initializationLastYear, true);
      permission = permissionPeriodPerYear(person, group, year, contract, 
          initializationPermission);
      vacationCurrentYear = vacationPeriodPerYear(person, group, year, contract,
          initializationCurrentYear, true);      
    }

    
    List<AbsencePeriod> periods = Lists.newArrayList();
    periods.addAll(vacationLastYear);
    periods.addAll(permission);
    periods.addAll(vacationCurrentYear);
    PeriodChain periodChain = new PeriodChain(person, group, date);
    periodChain.periods = periods;
    
    periodChain.vacationSupportList = Lists.newArrayList();
    periodChain.vacationSupportList.add(vacationLastYear);
    periodChain.vacationSupportList.add(vacationCurrentYear);
    periodChain.vacationSupportList.add(permission);
    
    
    for (AbsencePeriod period : vacationLastYear) {
      period.subPeriods = vacationLastYear;
    }
    for (AbsencePeriod period : permission) {  
      period.subPeriods = permission;
    }
    for (AbsencePeriod period : vacationCurrentYear) {
      period.subPeriods = vacationCurrentYear;
    }
    return periodChain;
  }
  
  private List<AbsencePeriod> vacationPeriodPerYear(Person person, GroupAbsenceType group, 
      int year, Contract contract, Integer initializationDays, boolean prorogation) {
    
    List<AbsencePeriod> periods = Lists.newArrayList();
    
    //TODO: questo deve essere un require nella modellazione, altrimenti dopo schianta.
    Set<AbsenceType> codes = group.takableAbsenceBehaviour.takenCodes; // === 31-32-94-37
    Set<AbsenceType> takableCodes = subSetCode(codes, DefaultAbsenceType.A_32);
    Set<AbsenceType> takenCodes = subSetCode(codes, DefaultAbsenceType.A_32);
    if (prorogation) {
      takableCodes = Sets.newHashSet();
    }
    
    //Ferie maturate nell'anno
    DateInterval yearInterval = DateUtility.getYearInterval(year);
    List<Integer> limits = Lists.newArrayList();
    for (VacationPeriod vacationPeriod : contract.getVacationPeriods()) {
      if (DateUtility
          .intervalIntersection(vacationPeriod.periodInterval(), yearInterval) == null) {
        continue;
      }
      LocalDate beginDate = yearInterval.getBegin();
      periods.addAll(periodsFromProgression(person, contract, group, beginDate, 
          YearProgression.whichVacationProgression(vacationPeriod.vacationCode), 
          vacationPeriod, takableCodes, takenCodes));
      limits.add(vacationPeriod.vacationCode.vacations);
    }
    
    //Fix del caso sfortunato
    periods = fixUnlucky(periods, limits, year);
    
    //Fix del caso fortunato
    periods = fixTooLucky(periods, limits, year);
    
    //Fix dei giorni post partum
    periods = fixPostPartum(periods, person, year);
    
    if (periods.isEmpty()) {
      return periods;
    }
    


    //Ferie usabili entro 31/8
    Set<AbsenceType> takable = subSetCode(codes, DefaultAbsenceType.A_31);
    Set<AbsenceType> taken = subSetCode(codes, DefaultAbsenceType.A_31);
    LocalDate beginNextYear = new LocalDate(year + 1, 1, 1);
    LocalDate endUsableNextYear = vacationsExpireDate(year, person.office);
    if (contract.calculatedEnd() != null 
        && contract.calculatedEnd().isBefore(endUsableNextYear)) {
      endUsableNextYear = contract.calculatedEnd();
    }
    if (!endUsableNextYear.isBefore(beginNextYear)) {
      periods.add(period(person, contract, group, beginNextYear, endUsableNextYear, takable, taken,
          DateUtility.daysInInterval(new DateInterval(beginNextYear, endUsableNextYear)), 0));
    }
    
    //Ferie usabili entro 31/12 codice 37
    if (!prorogation) {
      takable = Sets.newHashSet();
      taken = subSetCode(codes, DefaultAbsenceType.A_37);
    } else {
      takable = subSetCode(codes, DefaultAbsenceType.A_37);
      taken = subSetCode(codes, DefaultAbsenceType.A_37);
    }
    
    LocalDate beginUsableExtra = endUsableNextYear.plusDays(1);
    LocalDate endUsableNextYearExtra = new LocalDate(year + 1, 12, 31);
    if (contract.calculatedEnd() != null 
        && contract.calculatedEnd().isBefore(endUsableNextYearExtra)) {
      endUsableNextYearExtra = contract.calculatedEnd();
    }
    if (!endUsableNextYearExtra.isBefore(beginUsableExtra)) {
      periods.add(period(person, contract, group, beginUsableExtra, endUsableNextYearExtra,
          takable, taken,
          DateUtility.daysInInterval(new DateInterval(beginUsableExtra, endUsableNextYearExtra)), 
          0));
    }
    
    //Collapse initialization days
    handleInitialization(periods, initializationDays, contract.sourceDateResidual, group);
    
    return periods;
  }
  
  private List<AbsencePeriod> permissionPeriodPerYear(Person person, GroupAbsenceType group, 
      int year, Contract contract, int initializationDays) {
    List<AbsencePeriod> periods = Lists.newArrayList();

    Set<AbsenceType> codes = group.takableAbsenceBehaviour.takenCodes; // === 31-32-94-37
    Set<AbsenceType> takableCodes = subSetCode(codes, DefaultAbsenceType.A_94);
    Set<AbsenceType> takenCodes = subSetCode(codes, DefaultAbsenceType.A_94);
    
    DateInterval yearInterval = DateUtility.getYearInterval(year);
    List<Integer> limits = Lists.newArrayList();
    //Permessi nell'anno
    for (VacationPeriod vacationPeriod : contract.getVacationPeriods()) {
      if (DateUtility
          .intervalIntersection(vacationPeriod.periodInterval(), yearInterval) == null) {
        continue;
      }
      LocalDate beginDate = yearInterval.getBegin();
      periods.addAll(periodsFromProgression(person, contract, group, beginDate, 
          YearProgression.whichPermissionProgression(vacationPeriod.vacationCode),
          vacationPeriod, takableCodes, takenCodes));
      limits.add(vacationPeriod.vacationCode.permissions);
    }

    //Fix del caso sfortunato
    periods = fixUnlucky(periods, limits, year);
    
    //Fix del caso fortunato
    periods = fixTooLucky(periods, limits, year);
    
    //Fix dei giorni post partum
    periods = fixPostPartum(periods, person, year);
    
    //Collapse initialization days
    handleInitialization(periods, initializationDays, contract.sourceDateResidual, group);
    
    return periods;
  }
  
  private List<AbsencePeriod> fixUnlucky(List<AbsencePeriod> periods, List<Integer> lowerLimits, 
      int year) {
    
    if (periods.isEmpty()) {
      return periods;
    }
    
    //deve essere coperto tutto l'anno     
    LocalDate beginYear = new LocalDate(year, 1, 1);
    LocalDate endYear = new LocalDate(year, 12, 31);
    
    if (!beginYear.isEqual(periods.get(0).from)) {
      return periods;
    }
    if (!endYear.isEqual(periods.get(periods.size() - 1).to)) {
      return periods;
    }
    
    int lowerLimitSelected = lowerLimits.get(0);
    for (Integer lowerLimit : lowerLimits) {
      if (lowerLimitSelected > lowerLimit) {
        lowerLimitSelected = lowerLimit;
      }
    }
    
    //per il compute sumAll il period deve avere accesso a tutti i subPeriods fino a quel momento.
    //glieli faccio conoscere.
    periods.get(0).subPeriods = periods; 
    int totalTakable = periods.get(0)
        .computePeriodTakableAmount(TakeCountBehaviour.sumAllPeriod, null) / 100; 
    if (lowerLimitSelected > totalTakable) {
      //Aggiungo la quantità fixed al primo period (decidere.. dal momento che è zero potrebbe
      //essere aggiunto al secondo. Oppure all'ultimo ma potrebbe essere rimosso se attuassi
      //il fixedPostPartum successivamente). //dove imputarlo impatta sul test taverniti.
      
      int previousFixed = periods.get(0)
          .computePeriodTakableAmount(TakeCountBehaviour.period, null) / 100; //ex: 300 / 100
      periods.get(0).setFixedPeriodTakableAmount(previousFixed + lowerLimitSelected - totalTakable);
    }
    
    return periods;
  }
  
  private List<AbsencePeriod> fixTooLucky(List<AbsencePeriod> periods, List<Integer> upperLimits, 
      int year) {
    
    if (periods.isEmpty()) {
      return periods;
    }
    
    int upperLimitSelected = upperLimits.get(0);
    for (Integer upperLimit : upperLimits) {
      if (upperLimitSelected < upperLimit) {
        upperLimitSelected = upperLimit;
      }
    }
    
    //per il compute sumAll il period deve avere accesso a tutti i subPeriods fino a quel momento.
    //glieli faccio conoscere.
    periods.get(0).subPeriods = periods; 
    int totalTakable = periods.get(0)
        .computePeriodTakableAmount(TakeCountBehaviour.sumAllPeriod, null) / 100; 
    if (upperLimitSelected < totalTakable) {
      //Rimuovo la quantità dall'ultimo period con amount valorizzato
      AbsencePeriod periodSelected = null;
      for (AbsencePeriod period : periods) {
        if (period.computePeriodTakableAmount(TakeCountBehaviour.period, null) > 0) {
          periodSelected = period;
        }
      }
      int previousFixed = periodSelected
          .computePeriodTakableAmount(TakeCountBehaviour.period, null) / 100; //ex: 300 / 100
      periodSelected.setFixedPeriodTakableAmount(previousFixed 
          - (totalTakable - upperLimitSelected));
    }
    
    return periods;
  }

  private List<AbsencePeriod> fixPostPartum(List<AbsencePeriod> periods, Person person, int year) {
    
    if (periods.isEmpty()) { 
      return periods;
    }
    
    //Un algoritmo elegante...
    //Contare i giorni di postPartum appartenenti ai periods, e svuotare (partendo dal fondo)
    //i periods di dimensione completamente contenuta nel numero di giorni calcolati.

    LocalDate beginPostPartum = periods.get(0).from;
    LocalDate endPostPartum = periods.get(periods.size() - 1).to;
    GroupAbsenceType reducingGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.RIDUCE_FERIE_CNR.name()).get();
    periods.get(0).reducingAbsences = absenceComponentDao.orderedAbsences(person, 
        beginPostPartum, endPostPartum, reducingGroup.takableAbsenceBehaviour.takableCodes);
    int postPartum = periods.get(0).reducingAbsences.size();
    if (postPartum == 0) {
      return periods;
    }
    int index = periods.size() - 1; //a way to navigate the periods desc... 
    while (postPartum > 0 && index >= 0) {
      AbsencePeriod lastPeriod = periods.get(index);
      index--;
      int days = DateUtility.daysInInterval(lastPeriod.periodInterval());
      if (postPartum >= days) {
        postPartum = postPartum - days;
        lastPeriod.setFixedPeriodTakableAmount(0);
      } else {
        break;
      }
    }

    return periods;

  }

  private List<AbsencePeriod> periodsFromProgression(Person person, Contract contract, 
      GroupAbsenceType group,  LocalDate beginDate, YearProgression yearProgression,
      VacationPeriod vacationPeriod, Set<AbsenceType> takableCodes, Set<AbsenceType> takenCodes) {
    
    if (yearProgression == null) {
      log.info("La yearProgression è null...");
      return Lists.newArrayList();
    }
    
    LocalDate endYear = new LocalDate(beginDate.getYear(), 12, 31);
    List<AbsencePeriod> periods = Lists.newArrayList();
    
    if (beginDate.isBefore(vacationPeriod.getBeginDate())) {
      beginDate = vacationPeriod.getBeginDate();
    }
    if (vacationPeriod.endDate != null && endYear.isAfter(vacationPeriod.endDate)) {
      endYear = vacationPeriod.endDate;
    }
    
    LocalDate date = beginDate;
    
    for (YearPortion yearPortion : yearProgression.yearPortions) {

      AbsencePeriod absencePeriod = period(person, contract, group, date, endYear, 
          takableCodes, takenCodes, yearPortion.days, yearPortion.amount);
      absencePeriod.vacationCode = vacationPeriod.vacationCode;
      periods.add(absencePeriod);
      date = absencePeriod.to.plusDays(1);
      if (date.isAfter(endYear)) {
        break;
      }
    }
    return periods;
  }
  
  /**
   * Gestore dell'inizializzazione
   * @return l'ammontare da attribuire ai periodi successivi perchè precedente l'inizializzazione.
   */
  private List<AbsencePeriod> handleInitialization(List<AbsencePeriod> periods, 
      Integer initializationDays, LocalDate initializationDate, GroupAbsenceType group) {
    if (initializationDate == null) {
      return periods;
    }
    
    int postPonedAmount = 0;
    for (AbsencePeriod absencePeriod : periods) {
      
      if (initializationDate.isBefore(absencePeriod.from)) {
        return periods; //i periodi successivi l'inizializzazione non devono essere gestiti.
      }
      postPonedAmount = postPonedAmount 
          + absencePeriod.computePeriodTakableAmount(TakeCountBehaviour.period, null) / 100;

      if (initializationDate.isAfter(absencePeriod.to)) {
        absencePeriod.setFixedPeriodTakableAmount(0);
        InitializationGroup initialization = 
            new InitializationGroup(absencePeriod.person, group, initializationDate);
        initialization.unitsInput = 0;
        absencePeriod.initialization = initialization;
        continue;
      } 

      InitializationGroup initialization = 
          new InitializationGroup(absencePeriod.person, group, initializationDate);
      initialization.unitsInput = initializationDays;
      absencePeriod.initialization = initialization;
      absencePeriod.setFixedPeriodTakableAmount(postPonedAmount);
      
      return periods; //il periodo del fix è l'ultimo da gestire.
    }
    return periods;
  }
  
  private AbsencePeriod period(Person person, Contract contract,
      GroupAbsenceType group, LocalDate begin, LocalDate endYear,
      Set<AbsenceType> takableCodes, Set<AbsenceType> takenCodes, int days, int amount) {
    
    LocalDate end = begin.plusDays(days - 1);
    if (end.isAfter(endYear)) {
      end = endYear;
    }
    
    AbsencePeriod absencePeriod = new AbsencePeriod(person, group);
    absencePeriod.takeAmountType = AmountType.units;
    if (contract.endDate == null) {
      absencePeriod.takableCountBehaviour = TakeCountBehaviour.sumAllPeriod;
      absencePeriod.takenCountBehaviour = TakeCountBehaviour.sumAllPeriod;
    } else {
      absencePeriod.takableCountBehaviour = TakeCountBehaviour.sumUntilPeriod; //TD
      absencePeriod.takenCountBehaviour = TakeCountBehaviour.sumUntilPeriod;
    }
        
    absencePeriod.from = begin;
    absencePeriod.to = end;
    //mi assicuro di non eccedere in ogni caso la lunghezza del contratto.
    if (contract.calculatedEnd() != null && absencePeriod.to.isAfter(contract.calculatedEnd())) {
      absencePeriod.to = contract.calculatedEnd();
    }
    absencePeriod.setFixedPeriodTakableAmount(amount);
    absencePeriod.vacationAmountBeforeInitialization = amount;
    absencePeriod.takableCodes = takableCodes;
    absencePeriod.takenCodes = takenCodes;

    return absencePeriod;
  }
  
  private LocalDate vacationsExpireDate(int year, Office office) {

    MonthDay monthDay = (MonthDay)configurationManager
        .configValue(office, EpasParam.EXPIRY_VACATION_PAST_YEAR, year); 

    LocalDate expireDate = LocalDate.now()
        .withYear(year + 1)
        .withMonthOfYear(monthDay.getMonthOfYear())
        .withDayOfMonth(monthDay.getDayOfMonth());

    return expireDate;
  }
  
  /**
   * TODO: Per rendere generico questo algoritmo converrebbe enumerare il ruolo che hanno i codici
   * ferie.
   */
  private Set<AbsenceType> subSetCode(Set<AbsenceType> set, DefaultAbsenceType defaultType) {
    for (AbsenceType type : set) {
      if (type.code.equals(defaultType.getCode())) {
        return Sets.newHashSet(type);
      }
    }
    //Se chiamo questo metodo, il codice ci deve essere.
    throw new IllegalStateException();
  }
}
