package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateUtility;

import java.io.Serializable;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation.VacationSummary.TypeSummary;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;
import manager.services.vacations.VacationsTypeResult;

import models.Contract;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AbsenceType.DefaultAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import play.cache.Cache;

/**
 * Contiene lo stato ferie annuale di un contratto.
 * @author alessandro
 *
 */
@Slf4j
public class VacationSituation {

  public Person person;
  public Contract contract;
  public int year;
  public LocalDate date;
  
  public VacationSummary lastYear;
  public VacationSummary currentYear;
  public VacationSummary permissions;
  
  public VacationSummaryCached lastYearCached;
  public VacationSummaryCached currentYearCached;
  public VacationSummaryCached permissionsCached;
  
  public OldVacationSummary oldLastYear;
  public OldVacationSummary oldCurrentYear;
  public OldVacationSummary oldPermissions;
   
  /**
   * Costruttore.
   */
  public VacationSituation(Person person, Contract contract, int year, 
      GroupAbsenceType vacationGroup, Optional<LocalDate> residualDate, boolean cache, 
      AbsenceService absenceService, IVacationsService vacationsService) {
    
    this.person = person;
    this.contract = contract;
    this.year = year;
    
    if (vacationsService != null) { 
      Optional<VacationsRecap> vr = vacationsService.create(year, contract);
      if (vr.isPresent()) {
        this.oldLastYear = new OldVacationSummary(vr.get().getVacationsLastYear());
        this.oldCurrentYear = new OldVacationSummary(vr.get().getVacationsCurrentYear());
        this.oldPermissions = new OldVacationSummary(vr.get().getPermissions());
      }
    }

    //La data target per il riepilogo contrattuale
    LocalDate date = residualDate(residualDate, year);
    if (date == null) {
      return;
    }
    this.date = date;
    
    final String lastYearKey = contract.id + "-" + (year - 1) + "-" + TypeSummary.VACATION.name();
    final String currentYearKey = contract.id + "-" + year + "-" + TypeSummary.VACATION.name();
    final String permissionsKey = contract.id + "-" + year + "-" + TypeSummary.PERMISSION.name();
    
    //Provo a prelevare la situazione dalla cache
    if (cache) {
      this.lastYearCached = (VacationSummaryCached)Cache.get(lastYearKey);
      this.currentYearCached = (VacationSummaryCached)Cache.get(currentYearKey);
      this.permissionsCached = (VacationSummaryCached)Cache.get(permissionsKey);
      if (this.lastYearCached != null && this.lastYearCached.date.isEqual(date)
          && this.currentYearCached != null && this.currentYearCached.date.isEqual(date)
          && this.permissionsCached != null && this.permissionsCached.date.isEqual(date)) {
        //Tutto correttamente cachato.
        return;
      } else {
        log.info("La situazione di {} non era cachata", contract.person.fullName());
      }
    }
    PeriodChain periodChain = absenceService.residual(person, vacationGroup, date);
    if (!periodChain.vacationSupportList.get(0).isEmpty()) {
      this.lastYear = new VacationSummary(contract, periodChain.vacationSupportList.get(0).get(0), 
          year - 1, date, TypeSummary.VACATION);
    }
    if (!periodChain.vacationSupportList.get(1).isEmpty()) {
      this.currentYear = new VacationSummary(contract, 
          periodChain.vacationSupportList.get(1).get(0), year, date, TypeSummary.VACATION);
    }
    if (!periodChain.vacationSupportList.get(2).isEmpty()) {
      this.permissions = new VacationSummary(contract, 
          periodChain.vacationSupportList.get(2).get(0), year, date, TypeSummary.PERMISSION);
    }

    this.lastYearCached = new VacationSummaryCached(this.lastYear, 
        contract, year - 1, date, TypeSummary.VACATION);
    this.currentYearCached = new VacationSummaryCached(this.currentYear,
        contract, year, date, TypeSummary.VACATION);
    this.permissionsCached = new VacationSummaryCached(this.permissions,
        contract, year, date, TypeSummary.PERMISSION);
    
    Cache.set(lastYearKey, this.lastYearCached);
    Cache.set(currentYearKey, this.currentYearCached);
    Cache.set(permissionsKey, this.permissionsCached);
    
    //    try {
    //      CruscottoDipendente cruscottoDipendente = certService
    //        .getCruscottoDipendente(person, year);
    //      CertificationYearSituation yearSituation = 
    //          new CertificationYearSituation(absenceComponentDao, person, cruscottoDipendente);
    //      comparedVacation.certLastYear = yearSituation
    //          .getAbsenceSituation(AbsenceImportType.FERIE_ANNO_PRECEDENTE);
    //      comparedVacation.certCurrentYear = yearSituation
    //          .getAbsenceSituation(AbsenceImportType.FERIE_ANNO_CORRENTE);
    //      comparedVacation.certPermission = yearSituation
    //          .getAbsenceSituation(AbsenceImportType.PERMESSI_LEGGE);
    //      
    //    } catch (Exception e) {
    //      log.info("Impossibile scaricare l'informazione da attestati di {}", person.fullName());
    //    }
  }
  
  /**
   * La data per cui fornire il residuo. Se non l'ho fornita ritorno un default.
   */
  private LocalDate residualDate(Optional<LocalDate> residualDate, int year) {
    if (!residualDate.isPresent()) {
      LocalDate date = LocalDate.now();           
      if (date.getYear() > year) {
        date = new LocalDate(year, 12, 31);
      }
      if (contract.calculatedEnd() != null
          && contract.calculatedEnd().getYear() == year 
          && !DateUtility.isDateIntoInterval(date, contract.periodInterval())) {
        date = contract.calculatedEnd();
      }
      return date;
    } else {
      //La data che passo deve essere una data contenuta nell'anno.
      if (residualDate.get().getYear() != year) {
        log.info("VacationSummary: anno={} data={}: la data deve appartenere all'anno.");
        return null;
      }
      return residualDate.get();
    }
  }
  
  /**
   * I giorni rimanenti totali.
   */
  public int sumUsableTotal() {
    int totalRemaining = 0;
    if (lastYear != null) {
      totalRemaining += lastYear.usableTotal();
    } 
    if (currentYear != null) {
      totalRemaining += currentYear.usableTotal();
    }
    if (permissions != null) {
      totalRemaining += permissions.usableTotal();
    }
    return totalRemaining;
  }
 
  /**
   * Confronto anno passato.
   */
  public boolean epasEquivalent(OldVacationSummary old, VacationSummary summary) {

    if (old.result == null || summary == null) {
      return true;
    }
    if (old.total() != summary.total()) {
      return false;
    }
    if (old.postPartum() != summary.postPartum()) {
      return false;
    }
    if (old.accrued() != summary.accrued()) {
      return false;
    }
    if (!old.lowerLimit().isEqual(summary.lowerLimit())) {
      return false;
    }
    if (!old.upperLimit().isEqual(summary.upperLimit())) {
      return false;
    }
    if (old.used() != summary.used()) {
      return false;
    }
    if (old.usableTotal() != summary.usableTotal()) {
      return false;
    }
    //    if (old.usable() != summary.usable()) {
    //     return false;
    //    }
    return true;
  }
  
  public boolean epasLastYearEquivalent() {
    return epasEquivalent(oldLastYear, lastYear);
  }
  
  public boolean epasCurrentYearEquivalent() {
    return epasEquivalent(oldCurrentYear, currentYear);
  }
  
  public boolean epasPermissionEquivalent() {
    return epasEquivalent(oldPermissions, permissions);
  }
  
  //att
  
  //  /**
  //   * Totali attestati, anno passato. 
  //   */
  //  public int attestatiLastYearTotal() {
  //    if (certLastYear == null) {
  //      return -1;
  //    }
  //    return certLastYear.totalUsable;
  //  }
  //
  //  /**
  //   * Totali attestati, anno corrente. 
  //   */
  //  public int attestatiCurrentYearTotal() {
  //    if (certCurrentYear == null) {
  //      return -1;
  //    }
  //    return certCurrentYear.totalUsable;
  //  }
  //
  //  /**
  //   * Totali attestati, permessi. 
  //   */
  //  public int attestatiPermissionTotal() {
  //    if (certPermission == null) {
  //      return -1;
  //    }
  //    return certPermission.totalUsable;
  //  }
  
  public static class OldVacationSummary {
    public VacationsTypeResult result;
    
    public OldVacationSummary(VacationsTypeResult result) {
      this.result = result;
    }
    
    public int total() {
      return result.getTotal();
    }
    
    public int postPartum() {
      return result.getTotalResult().getPostPartum().size();
    }
    
    public int accrued() {
      return result.getAccrued();
    }
    
    public boolean isContractLowerLimit() {
      return result.isContractLowerLimit();
    }
    
    public LocalDate lowerLimit() {
      return result.getLowerLimit();
    }
    
    public boolean isContractUpperLimit() {
      return result.isContractUpperLimit();
    }
    
    public LocalDate upperLimit() {
      return result.getUpperLimit();
    }
    
    public int used() {
      return result.getUsed();
    }
    
    public int usable() { 
      return result.getNotYetUsedTakeable();
    }
    
    public int usableTotal() { 
      return result.getNotYetUsedTotal();
    }
    
  }

  
  public static class VacationSummary {
    
    public TypeSummary type;
    public int year;
    public LocalDate date;  //data situazione. Tipicamenete oggi. Determina maturate e scadute.
    public Contract contract;
    public AbsencePeriod absencePeriod;
    
    /**
     * Constructor.
     */
    public VacationSummary(Contract contract, AbsencePeriod absencePeriod, 
        int year, LocalDate date, TypeSummary type) {
      this.year = year;
      this.type = type;
      this.date = date;
      this.contract = contract;
      this.absencePeriod = absencePeriod;
    }
    
    public static enum TypeSummary {
      VACATION, PERMISSION;
    }
    
    public int total() {
      return computeTotal(absencePeriod);
    }
    
    /**
     * Nuova implementazione: posPartum anno passato.
     */
    public int postPartum() {
      if (absencePeriod == null) {
        return 0;
      }
      return absencePeriod.reducingAbsences.size();
    }
    
    public int accrued() {
      return computeAccrued(absencePeriod);
    }
    
    public boolean isContractLowerLimit() {
      return isContractLowerLimit(absencePeriod.from);
    }
    
    public LocalDate lowerLimit() {
      return absencePeriod.from;
    }
    
    public boolean isContractUpperLimit() {
      return isContractUpperLimit(lastNaturalPeriod(absencePeriod).to);
    }
    
    public LocalDate upperLimit() {
      return lastNaturalPeriod(absencePeriod).to;
    }
    
    public int used() {
      return computeUsed(absencePeriod);
    }
    
    /**
     * I giorni usabili. (Usufruibili, per i det solo le maturate).
     */
    public int usable() {
      if (expired()) {
        return 0;
      }
      if (absencePeriod.takableCountBehaviour.equals(TakeCountBehaviour.sumUntilPeriod)) {
        return accrued() - used(); 
      } else {
        return total() - used();
      }
    }
    
    /**
     * Se il periodo è scaduto alla data dateToCheck. Se absent considera oggi.
     */
    public boolean expired() {
      if (lastNaturalPeriod(absencePeriod).to.isAfter(date)) {
        return false;
      }
      return true;
    }
    
    public int usableTotal() { 
      return total() - used();
    }
    
    private int computeTotal(AbsencePeriod period) {
      return period
          .computePeriodTakableAmount(TakeCountBehaviour.sumAllPeriod, date) / 100;
    }
    
    private int computeAccrued(AbsencePeriod period) {
      return period
          .computePeriodTakableAmount(TakeCountBehaviour.sumUntilPeriod, date) / 100;
    }
    
    private int computeUsed(AbsencePeriod period) {
      return period.getPeriodTakenAmount(true) / 100;
    }
    
    private boolean isContractUpperLimit(LocalDate date) {
      if (contract.calculatedEnd() != null 
          && contract.calculatedEnd().isEqual(date)) {
        return true;
      }
      return false;
    }

    private boolean isContractLowerLimit(LocalDate date) {
      if (contract.beginDate.isEqual(date)) {
        return true;
      }
      return false;
    }
    
    /**
     * L'ultimo periodo (per la data fine). Se l'ultimo periodo è l'estensione 37 seleziono quello
     * ancora precedente.
     */
    private AbsencePeriod lastNaturalPeriod(AbsencePeriod period) {
      AbsencePeriod lastPeriod = period.subPeriods.get(period.subPeriods.size() - 1);
      for (AbsenceType taken : lastPeriod.takenCodes) {
        if (taken.code.equals(DefaultAbsenceType.A_37.getCode())) {
          return period.subPeriods.get(period.subPeriods.size() - 2);
        }
      }
      return lastPeriod;
    }
    
    /**
     * I giorni da inizializzazione.
     */
    public int sourced() {
      int sourced = 0;
      for (AbsencePeriod period : absencePeriod.subPeriods) {
        if (period.initialization != null) {
          sourced += period.initialization.unitsInput;
        }
      }
      return sourced;
    }
    
    /**
     * Le assenze utilizzate (post inizializzazione).
     */
    public List<Absence> absencesUsed() {
      List<Absence> absencesUsed = Lists.newArrayList();
      for (AbsencePeriod period : absencePeriod.subPeriods) {
        for (DayInPeriod day : period.daysInPeriod.values()) {
          for (TakenAbsence takenAbsence : day.getTakenAbsences()) {
            if (!takenAbsence.beforeInitialization) {
              absencesUsed.add(takenAbsence.absence);
            }
          }
        }
      }
      return absencesUsed;
    }
    
    /**
     * I giorni maturati nel periodo per il riepilogo. 
     */
    public int periodAmountAccrued(AbsencePeriod period) {
      if (date.isBefore(period.from)) {
        return 0;
      }
      return period.vacationAmountBeforeInitialization;
    }
    
    /**
     * Una label che da il titolo.
     * TODO: spostare su message.
     */
    public String title() {
      if (this.type.equals(TypeSummary.VACATION)) {
        return this.contract.person.fullName() + " - " + "Riepilogo Ferie " + this.year;  
      } else {
        return this.contract.person.fullName() + " - " + "Riepilogo Permessi Legge " + this.year;
      }
    }
  }
  
  /**
   * Versione cachata del riepilogo.
   * @author alessandro
   *
   */
  public static class VacationSummaryCached implements Serializable {
    
    public boolean exists = true;
    public TypeSummary type;
    public int year;
    public LocalDate date; 
    public Contract contract;
    
    public int total;
    public int postPartum;
    public int accrued;
    public int used;
    public int usable;
    public boolean expired;
    public int usableTotal;
    public boolean isContractLowerLimit;
    public LocalDate lowerLimit;
    public boolean isContractUpperLimit;
    public LocalDate upperLimit;
    
    /**
     * Costruttore. Se il vacationSummary è null significa che il riepilogo non esiste:
     * Setto exists = false;
     */
    public VacationSummaryCached(VacationSummary vacationSummary, Contract contract, int year, 
        LocalDate date, TypeSummary type) {
      
      if (vacationSummary == null) {
        this.exists = false;
        this.type = type;
        this.year = year;
        this.date = date;
        this.contract = contract;
        this.contract.merge();
      } else {
        this.type = vacationSummary.type;
        this.year = vacationSummary.year;
        this.date = vacationSummary.date;
        this.contract = vacationSummary.contract;
        this.contract.merge();
        this.total = vacationSummary.total();
        this.postPartum = vacationSummary.postPartum();
        this.accrued = vacationSummary.accrued();
        this.used = vacationSummary.used();
        this.usable = vacationSummary.usable();
        this.expired = vacationSummary.expired();
        this.usableTotal = vacationSummary.usableTotal();
        this.isContractLowerLimit = vacationSummary.isContractLowerLimit();
        this.lowerLimit = vacationSummary.lowerLimit();
        this.isContractUpperLimit = vacationSummary.isContractUpperLimit();
        this.upperLimit = vacationSummary.upperLimit();
      }
      
    }
  }
  
}
