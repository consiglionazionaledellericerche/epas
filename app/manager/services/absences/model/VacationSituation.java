package manager.services.absences.model;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

import manager.services.absences.model.VacationSituation.VacationSummary.TypeSummary;
import manager.services.vacations.VacationsTypeResult;

import models.Contract;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;
import models.absences.definitions.DefaultAbsenceType;

import org.joda.time.LocalDate;

/**
 * Contiene lo stato ferie annuale di un contratto.
 * @author alessandro
 *
 */
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
   * I giorni rimanenti totali.
   */
  public int sumUsableTotal() {
    int totalRemaining = 0;
    if (lastYearCached.exists) {
      totalRemaining += lastYearCached.usableTotal;
    } 
    if (currentYearCached.exists) {
      totalRemaining += currentYearCached.usableTotal;
    }
    if (permissionsCached != null) {
      totalRemaining += permissionsCached.usableTotal;
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
