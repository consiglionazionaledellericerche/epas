package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import manager.cache.AbsenceTypeManager;
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
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

public class VacationFactory {
  
  private final ConfigurationManager configurationManager;
  private final AbsenceTypeManager absenceTypeManager;
  private final AbsenceComponentDao absenceComponentDao;

  /**
   * Costruttore. 
   */
  @Inject
  public VacationFactory(ConfigurationManager configurationManager, 
      AbsenceTypeManager absenceTypeManager,
      AbsenceComponentDao absenceComponentDao) {
    this.configurationManager = configurationManager;
    this.absenceTypeManager = absenceTypeManager;
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
    
    AbsenceType code32 = absenceTypeManager.getAbsenceType("32");
    AbsenceType code31 = absenceTypeManager.getAbsenceType("31");
    AbsenceType code37 = absenceTypeManager.getAbsenceType("37");
    AbsenceType code94 = absenceTypeManager.getAbsenceType("94");
    
    List<AbsencePeriod> vacationLastYear = 
        vacationPeriodPerYear(person, group, date.getYear() - 1, contract, code32, code31, code37);
    List<AbsencePeriod> permission = 
        permissionPeriodPerYear(person, group, date.getYear(), contract, code94);
    List<AbsencePeriod> vacationCurrentYear = 
        vacationPeriodPerYear(person, group, date.getYear(), contract, code32, code31, code37);
    
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
      period.periods = vacationLastYear;
    }
    for (AbsencePeriod period : permission) {  
      period.periods = permission;
    }
    for (AbsencePeriod period : vacationCurrentYear) {
      period.periods = vacationCurrentYear;
    }
    return periodChain;
  }
  
  private List<AbsencePeriod> vacationPeriodPerYear(Person person, GroupAbsenceType group, 
      int year, Contract contract, 
      final AbsenceType code32, 
      final AbsenceType code31, 
      final AbsenceType code37) {
    
    List<AbsencePeriod> periods = Lists.newArrayList();
    Set<AbsenceType> takable = Sets.newHashSet(code32);
    
    //Ferie maturate nell'anno
    DateInterval yearInterval = DateUtility.getYearInterval(year);
    for (VacationPeriod vacationPeriod : contract.getVacationPeriods()) {
      if (DateUtility
          .intervalIntersection(vacationPeriod.periodInterval(), yearInterval) == null) {
        continue;
      }
      LocalDate beginDate = yearInterval.getBegin();
      periods.addAll(periodsFromProgression(person, contract, group, beginDate, 
          YearProgression.whichVacationProgression(vacationPeriod.vacationCode), 
          vacationPeriod, takable));
    }
    
    //Fix dei giorni post partum
    fixPostPartum(periods, person, year);
    
    //Ferie usabili entro 31/8
    takable = Sets.newHashSet(code31);
    LocalDate beginNextYear = new LocalDate(year + 1, 1, 1);
    LocalDate endUsableNextYear = vacationsExpireDate(year, person.office);
    if (contract.calculatedEnd() != null 
        && contract.calculatedEnd().isBefore(endUsableNextYear)) {
      endUsableNextYear = contract.calculatedEnd();
    }
    if (!endUsableNextYear.isBefore(beginNextYear)) {
      periods.add(period(person, contract, group, beginNextYear, takable, DateUtility
          .daysInInterval(new DateInterval(beginNextYear, endUsableNextYear)) - 1, 0));
    }
    //Ferie usabili entro 31/12 codice 37
    takable = Sets.newHashSet(code37);
    LocalDate beginUsableExtra = endUsableNextYear.plusDays(1);
    LocalDate endUsableNextYearExtra = new LocalDate(year + 1, 12, 31);
    if (contract.calculatedEnd() != null 
        && contract.calculatedEnd().isBefore(endUsableNextYearExtra)) {
      endUsableNextYearExtra = contract.calculatedEnd();
    }
    if (!endUsableNextYearExtra.isBefore(beginUsableExtra)) {
      periods.add(period(person, contract, group, beginUsableExtra, takable, 
          DateUtility.daysInInterval(new DateInterval(beginUsableExtra, 
              endUsableNextYearExtra)) - 1, 0));
    }
    
    return periods;
  }
  
  private List<AbsencePeriod> fixPostPartum(List<AbsencePeriod> periods, Person person, int year) {
    
    if (periods.isEmpty()) { 
      return periods;
    }
    
    //Un algoritmo elegante...
    //Contare i giorni di postPartum appartenenti ai periods, e rimuovere (partendo dal fondo)
    //i periods di dimensione completamente contenuta nel numero di giorni calcolati.

    LocalDate beginPostPartum = periods.get(0).from;
    LocalDate endPostPartum = periods.get(periods.size() - 1).to;
    periods.get(0).reducingAbsences = absenceComponentDao.orderedAbsences(person, 
        beginPostPartum, endPostPartum, absenceTypeManager.reducingCodes());
    int postPartum = periods.get(0).reducingAbsences.size();
    if (postPartum == 0) {
      return periods;
    }
    while (postPartum > 0) {
      AbsencePeriod lastPeriod = periods.get(periods.size() - 1);
      int days = DateUtility.daysInInterval(lastPeriod.periodInterval());
      if (postPartum >= days) {
        postPartum = postPartum - days;
        periods.remove(lastPeriod);
      } else {
        break;
      }
    }

    return periods;

  }
  
  private List<AbsencePeriod> permissionPeriodPerYear(Person person, GroupAbsenceType group, 
      int year, Contract contract, final AbsenceType code94) {
    List<AbsencePeriod> periods = Lists.newArrayList();

    Set<AbsenceType> takable = Sets.newHashSet(code94);
    
    DateInterval yearInterval = DateUtility.getYearInterval(year);
    //Permessi nell'anno
    for (VacationPeriod vacationPeriod : contract.getVacationPeriods()) {
      if (DateUtility
          .intervalIntersection(vacationPeriod.periodInterval(), yearInterval) == null) {
        continue;
      }
      LocalDate beginDate = yearInterval.getBegin();
      periods.addAll(periodsFromProgression(person, contract, group, beginDate, 
          YearProgression.whichPermissionProgression(vacationPeriod.vacationCode),
          vacationPeriod, takable));
    }
    
    //Fix dei giorni post partum
    fixPostPartum(periods, person, year);
    
    return periods;
  }


  private List<AbsencePeriod> periodsFromProgression(Person person, Contract contract, 
      GroupAbsenceType group,  LocalDate beginDate, YearProgression yearProgression,
      VacationPeriod vacationPeriod, Set<AbsenceType> takableCodes) {
    
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
      AbsencePeriod absencePeriod = period(person, contract, group, date, takableCodes, 
          yearPortion.days, yearPortion.amount);
      periods.add(absencePeriod);
      date = absencePeriod.to.plusDays(1);
      if (date.isAfter(endYear)) {
        break;
      }
    }
    return periods;
  }
  
  private AbsencePeriod period(Person person, Contract contract,
      GroupAbsenceType group, LocalDate begin,
      Set<AbsenceType> takableCodes, int days, int amount) {
    
    LocalDate endYear = new LocalDate(begin.getYear(), 12, 31);
    LocalDate end = begin.plusDays(days - 1);
    if (end.isAfter(endYear)) {
      end = endYear;
    }
    
    AbsencePeriod absencePeriod = new AbsencePeriod(person, group);
    absencePeriod.takeAmountType = AmountType.units;
    if (contract.endDate == null) {
      absencePeriod.takableCountBehaviour = TakeCountBehaviour.sumAllPeriod;  
    } else {
      absencePeriod.takableCountBehaviour = TakeCountBehaviour.sumUntilPeriod; //TD
    }
    absencePeriod.takenCountBehaviour = TakeCountBehaviour.sumAllPeriod;    
    absencePeriod.from = begin;
    absencePeriod.to = end;
    absencePeriod.setFixedPeriodTakableAmount(amount);
    absencePeriod.takableCodes = takableCodes;
    absencePeriod.takenCodes = takableCodes;

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
  
}
