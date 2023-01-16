/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;
import dao.AbsenceTypeDao;
import dao.absences.AbsenceComponentDao;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
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


/**
 * Factory per le ferie.
 */
@Slf4j
public class VacationFactory {
  
  private final ConfigurationManager configurationManager;
  private final AbsenceComponentDao absenceComponentDao;
  private final AbsenceTypeDao absenceTypeDao;
  private final PersonDayManager personDayManager;

  /**
   * Costruttore. 
   */
  @Inject
  public VacationFactory(ConfigurationManager configurationManager, 
      AbsenceComponentDao absenceComponentDao, AbsenceTypeDao absenceTypeDao,
      PersonDayManager personDayManager) {
    this.configurationManager = configurationManager;
    this.absenceComponentDao = absenceComponentDao;
    this.absenceTypeDao = absenceTypeDao;
    this.personDayManager = personDayManager;
  }
  
  /**
   * La periodChain che riduce il problema delle ferie e permessi alla prendibilità di assenze.
   *
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
    if (contract.getSourceDateVacation() != null) {
      if (contract.getSourceDateVacation().getYear() == year) {
        initializationLastYear = contract.getSourceVacationLastYearUsed();
        initializationCurrentYear = contract.getSourceVacationCurrentYearUsed();
        initializationPermission = contract.getSourcePermissionUsed();
      } else if (contract.getSourceDateVacation().getYear() == year - 1) {
        initializationLastYear = contract.getSourceVacationCurrentYearUsed(); 
      } else if (contract.getSourceDateVacation().getYear() == year + 1) {
        initializationCurrentYear = contract.getSourceVacationLastYearUsed();
      }
    }
    
    List<AbsencePeriod> vacationLastYear = null;
    List<AbsencePeriod> permission  = null;
    List<AbsencePeriod> vacationCurrentYear = null;
    
    if (group.getName().equals(DefaultGroup.FERIE_CNR.name()) 
        || group.getName().equals(DefaultGroup.FERIE_CNR_DIPENDENTI.name())) {
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
    Set<AbsenceType> codes = group.getTakableAbsenceBehaviour().getTakenCodes(); // === 31-32-94-37
    Set<AbsenceType> takableCodes = subSetCode(codes, DefaultAbsenceType.A_32);
    Set<AbsenceType> takenCodes = subSetCode(codes, DefaultAbsenceType.A_32);
    if (prorogation) {
      takableCodes = Sets.newHashSet();
    }
    
    //Ferie maturate nell'anno
    DateInterval yearInterval = DateUtility.getYearInterval(year);
    List<Integer> limits = Lists.newArrayList();
    for (VacationPeriod vacationPeriod : contract.getExtendedVacationPeriods()) {
      if (DateUtility
          .intervalIntersection(vacationPeriod.periodInterval(), yearInterval) == null) {
        continue;
      }
      LocalDate beginDate = yearInterval.getBegin();
      periods.addAll(periodsFromProgression(person, contract, group, beginDate, 
          YearProgression.whichVacationProgression(vacationPeriod.getVacationCode()), 
          vacationPeriod, takableCodes, takenCodes));
      limits.add(vacationPeriod.getVacationCode().vacations);
    }
    
    //Fix del caso sfortunato
    periods = fixUnlucky(periods, limits, year);
    
    //Fix del caso fortunato
    periods = fixTooLucky(periods, limits, year);
    
    //Fix dei giorni post partum
    periods = fixPostPartum(periods, person, year);
    
    //Split del primo anno di contratto
    periods = handleAccruedFirstYear(person, group, contract, periods);
    
    if (periods.isEmpty()) {
      return periods;
    }
    
    //Ferie usabili entro 31/8
    Set<AbsenceType> takable = subSetCode(codes, DefaultAbsenceType.A_31);
    Set<AbsenceType> taken = subSetCode(codes, DefaultAbsenceType.A_31);
    LocalDate beginNextYear = new LocalDate(year + 1, 1, 1);
    LocalDate endUsableNextYear = vacationsExpireDate(year, person.getOffice());
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
    handleInitialization(periods, initializationDays, contract.getSourceDateVacation(), group);
    
    return periods.stream().distinct().collect(Collectors.toList());
  }
  
  private List<AbsencePeriod> permissionPeriodPerYear(Person person, GroupAbsenceType group, 
      int year, Contract contract, int initializationDays) {
    List<AbsencePeriod> periods = Lists.newArrayList();

    Set<AbsenceType> codes = group.getTakableAbsenceBehaviour().getTakenCodes(); // === 31-32-94-37
    Set<AbsenceType> takableCodes = subSetCode(codes, DefaultAbsenceType.A_94);
    Set<AbsenceType> takenCodes = subSetCode(codes, DefaultAbsenceType.A_94);
    
    DateInterval yearInterval = DateUtility.getYearInterval(year);
    List<Integer> limits = Lists.newArrayList();
    //Permessi nell'anno
    for (VacationPeriod vacationPeriod : contract.getExtendedVacationPeriods()) {
      if (DateUtility
          .intervalIntersection(vacationPeriod.periodInterval(), yearInterval) == null) {
        continue;
      }
      LocalDate beginDate = yearInterval.getBegin();
      periods.addAll(periodsFromProgression(person, contract, group, beginDate, 
          YearProgression.whichPermissionProgression(vacationPeriod.getVacationCode()),
          vacationPeriod, takableCodes, takenCodes));
      limits.add(vacationPeriod.getVacationCode().permissions);
    }

    //Fix del caso sfortunato
    periods = fixUnlucky(periods, limits, year);
    
    //Fix del caso fortunato
    periods = fixTooLucky(periods, limits, year);
    
    //Fix dei giorni post partum
    periods = fixPostPartum(periods, person, year);

    //Split del primo anno di contratto
    periods = handleAccruedFirstYear(person, group, contract, periods);
    
    //Collapse initialization days
    handleInitialization(periods, initializationDays, contract.getSourceDateVacation(), group);
    
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
      return periods.stream().distinct().collect(Collectors.toList());
    }
    if (!endYear.isEqual(periods.get(periods.size() - 1).to)) {
      return periods.stream().distinct().collect(Collectors.toList());
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
      int newAmount = previousFixed + lowerLimitSelected - totalTakable;
      periods.get(0).setFixedPeriodTakableAmount(newAmount);
      periods.get(0).vacationAmountBeforeFixPostPartum = newAmount;
      periods.get(0).vacationAmountBeforeInitializationPatch = newAmount;
    }
    
    return periods.stream().distinct().collect(Collectors.toList());
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
      int newAmount = previousFixed - (totalTakable - upperLimitSelected);
      periodSelected.setFixedPeriodTakableAmount(newAmount);
      periods.get(0).vacationAmountBeforeFixPostPartum = newAmount;
      periods.get(0).vacationAmountBeforeInitializationPatch = newAmount;
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
    // FIXME a volte il ritorno del dao è absent e schianta la get()
    GroupAbsenceType reducingGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.RIDUCE_FERIE_CNR.name()).get();
    periods.get(0).reducingAbsences = absenceComponentDao.orderedAbsences(person, 
        beginPostPartum, endPostPartum, reducingGroup.getTakableAbsenceBehaviour().getTakableCodes());
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
        int previousAmount = lastPeriod
            .computePeriodTakableAmount(TakeCountBehaviour.period, null) / 100;
        lastPeriod.vacationAmountBeforeFixPostPartum = previousAmount;
        lastPeriod.vacationAmountBeforeInitializationPatch = 0;
        lastPeriod.setFixedPeriodTakableAmount(0);
      } else {
        break;
      }
    }

    return periods;

  }
  
  /**
   * Splitta i periodi che ricadono a cavallo del primo anno di contratto.
   * E' importante applicare questo gestore dopo i fix ma prima della inizializzazione!
   */
  private List<AbsencePeriod> handleAccruedFirstYear(Person person, GroupAbsenceType group, 
      Contract contract, List<AbsencePeriod> periods) {
    List<AbsencePeriod> fixed = Lists.newArrayList();
    
    LocalDate secondYearStart = contract.getPreviousContract() != null
        ? contract.getPreviousContract().getBeginDate().plusYears(1)
            : contract.getBeginDate().plusYears(1);
    for (AbsencePeriod period : periods) {

      if (!period.from.isBefore(secondYearStart)) {
        fixed.add(period);
        continue;
      }

      // split 
      if (DateUtility.isDateIntoInterval(secondYearStart.minusDays(1), 
          new DateInterval(period.from, period.to))) {
        
        // creo il period aggiuntivo con amount 0 (default)
        AbsencePeriod splitted = new AbsencePeriod(contract.getPerson(), group, 
            personDayManager, absenceTypeDao);
        splitted.from = secondYearStart;
        splitted.to = period.to;

        // chiudo il periodo all'ultimo giorno del primo anno.
        period.to = secondYearStart.minusDays(1);
        period.takableCountBehaviour = TakeCountBehaviour.sumUntilPeriod;
        period.takenCountBehaviour = TakeCountBehaviour.sumUntilPeriod;
        fixed.add(period);
        
        if (secondYearStart.isAfter(splitted.to)) {
          continue;
        }
        
        splitted.takeAmountType = AmountType.units;
        splitted.takableCountBehaviour = TakeCountBehaviour.sumAllPeriod;
        splitted.takenCountBehaviour = TakeCountBehaviour.sumAllPeriod;
        splitted.takableCodes = period.takableCodes;
        splitted.takenCodes = period.takenCodes;
        
        fixed.add(splitted);
        period.splittedWith = splitted;
        continue;
      } 
        
      // completamente precedente
      period.takableCountBehaviour = TakeCountBehaviour.sumUntilPeriod;
      period.takenCountBehaviour = TakeCountBehaviour.sumUntilPeriod;
      fixed.add(period);
      
    }
    
    return fixed;
  }
  
  /**
   * Quando ho completato le computazioni potrò rimuovere il period fittizio.
   * Motivo: il VacationSituation non è compatibile con la presenza del periodo fittizio.
   */
  public void removeAccruedFirstYear(PeriodChain chain) {
    for (List<AbsencePeriod> periods : chain.vacationSupportList) {
      AbsencePeriod splittedWith = null;

      for (AbsencePeriod subPeriod : periods) {
        if (subPeriod.splittedWith == null) {
          continue;
        }
        splittedWith = subPeriod.splittedWith;

        // ripristino la validità
        subPeriod.to = splittedWith.to;

        // ricopio i dayInPeriod
        for (DayInPeriod dayInPeriod : splittedWith.daysInPeriod.values()) {
          subPeriod.daysInPeriod.put(dayInPeriod.getDate(), dayInPeriod);
        }

        // assegno l'inizializzazione se è ricaduta proprio nel periodo splitted
        // (trasferendo l'intero postPonedAmount)
        if (splittedWith.initialization != null && DateUtility
            .isDateIntoInterval(splittedWith.initialization.getDate(), splittedWith.periodInterval())) {

          // qualche verifica per assicurarmi che non perdo nessuna informazione ...
          Preconditions.checkState(subPeriod.initialization.getUnitsInput() == 0);
          Preconditions.checkState(subPeriod.getFixedPeriodTakableAmount() == 0);
          Preconditions.checkState(splittedWith.vacationAmountBeforeFixPostPartum == 0);
          Preconditions.checkState(splittedWith.vacationAmountBeforeInitializationPatch == 0);

          subPeriod.initialization = subPeriod.splittedWith.initialization;
          //I valori dei giorni di assenza nel periodo sono già stati moltiplicati per
          //100 quindi è necessario diverli prima di ripassarli al metodo che li 
          //imposta nel subPeriod.
          subPeriod.setFixedPeriodTakableAmount(splittedWith.getFixedPeriodTakableAmount() / 100);
        }
      }
      if (splittedWith != null) {
        periods.remove(splittedWith);
        splittedWith = null;
      }
    }
  }

  private List<AbsencePeriod> periodsFromProgression(Person person, Contract contract, 
      GroupAbsenceType group,  LocalDate beginDate, YearProgression yearProgression,
      VacationPeriod vacationPeriod, Set<AbsenceType> takableCodes, Set<AbsenceType> takenCodes) {
    
    if (yearProgression == null) {
      log.debug("La yearProgression è null...");
      return Lists.newArrayList();
    }
    
    LocalDate endYear = new LocalDate(beginDate.getYear(), 12, 31);
    List<AbsencePeriod> periods = Lists.newArrayList();
    
    if (beginDate.isBefore(vacationPeriod.getBeginDate())) {
      beginDate = vacationPeriod.getBeginDate();
    }
    if (vacationPeriod.getEndDate() != null && endYear.isAfter(vacationPeriod.getEndDate())) {
      endYear = vacationPeriod.getEndDate();
    }
    
    LocalDate date = beginDate;
    
    for (YearPortion yearPortion : yearProgression.yearPortions) {

      AbsencePeriod absencePeriod = period(person, contract, group, date, endYear, 
          takableCodes, takenCodes, yearPortion.days, yearPortion.amount);
      absencePeriod.vacationCode = vacationPeriod.getVacationCode();
      periods.add(absencePeriod);
      date = absencePeriod.to.plusDays(1);
      if (date.isAfter(endYear)) {
        break;
      }
    }
    return periods.stream().distinct().collect(Collectors.toList());
  }
  
  /**
   * Gestore dell'inizializzazione.
   *
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
        initialization.setUnitsInput(0);
        absencePeriod.initialization = initialization;
        continue;
      } 

      InitializationGroup initialization = 
          new InitializationGroup(absencePeriod.person, group, initializationDate);
      initialization.setUnitsInput(initializationDays);
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
    
    AbsencePeriod absencePeriod = new AbsencePeriod(person, group, 
        personDayManager, absenceTypeDao);
    absencePeriod.takeAmountType = AmountType.units;
    absencePeriod.takableCountBehaviour = TakeCountBehaviour.sumAllPeriod;
    absencePeriod.takenCountBehaviour = TakeCountBehaviour.sumAllPeriod;
       
    absencePeriod.from = begin;
    absencePeriod.to = end;
    //mi assicuro di non eccedere in ogni caso la lunghezza del contratto.
    if (contract.calculatedEnd() != null && absencePeriod.to.isAfter(contract.calculatedEnd())) {
      absencePeriod.to = contract.calculatedEnd();
    }
    absencePeriod.setFixedPeriodTakableAmount(amount);
    absencePeriod.vacationAmountBeforeFixPostPartum = amount;
    absencePeriod.vacationAmountBeforeInitializationPatch = amount;
    absencePeriod.takableCodes = takableCodes;
    absencePeriod.takenCodes = takenCodes;

    return absencePeriod;
  }
  
  private LocalDate vacationsExpireDate(int year, Office office) {

    MonthDay monthDay = (MonthDay) configurationManager
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
      if (type.getCode().equals(defaultType.getCode())) {
        return Sets.newHashSet(type);
      }
    }
    //Se chiamo questo metodo, il codice ci deve essere.
    throw new IllegalStateException();
  }
}
