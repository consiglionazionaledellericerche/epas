package manager.services.absences.model;

import com.google.common.base.Optional;

import dao.wrapper.IWrapperPerson;

import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceService;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;
import manager.services.vacations.VacationsTypeResult;

import models.Contract;
import models.Person;
import models.absences.AbsenceType;
import models.absences.AbsenceType.DefaultAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

/**
 * Una classe per il confronto fra la vecchia e la nuova implementazione delle ferie.
 * @author alessandro
 *
 */
public class VacationSituation {

  public Person person;
  public Contract contract;
  public int year;
  
  public VacationSummary lastYear;
  public VacationSummary currentYear;
  public VacationSummary permissions;
  
  public OldVacationSummary oldLastYear;
  public OldVacationSummary oldCurrentYear;
  public OldVacationSummary oldPermissions;
   
  /**
   * Costruttore.
   */
  public VacationSituation(IWrapperPerson wrPerson, Contract contract, int year, 
      GroupAbsenceType vacationGroup, AbsenceService absenceService, 
      IVacationsService vacationsService) {
    
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
    LocalDate date = LocalDate.now();           
    if (date.getYear() > year) {
      date = new LocalDate(year, 12, 31);
    }
    if (contract.calculatedEnd() != null
        && contract.calculatedEnd().getYear() == year 
        && !DateUtility.isDateIntoInterval(date, contract.periodInterval())) {
      date = contract.calculatedEnd();
    }
    PeriodChain periodChain = absenceService.residual(wrPerson.getValue(), 
        vacationGroup, date);
    this.person = wrPerson.getValue();
    if (!periodChain.vacationSupportList.get(0).isEmpty()) {
      this.lastYear = new VacationSummary(contract, periodChain.vacationSupportList.get(0).get(0));
    }
    if (!periodChain.vacationSupportList.get(1).isEmpty()) {
      this.currentYear = 
          new VacationSummary(contract, periodChain.vacationSupportList.get(1).get(0));
    }
    if (!periodChain.vacationSupportList.get(2).isEmpty()) {
      this.permissions = 
          new VacationSummary(contract, periodChain.vacationSupportList.get(2).get(0));
    }
    
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
    if (old.usable() != summary.usable()) {
      return false;
    }
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
    
    public Contract contract;
    public AbsencePeriod absencePeriod;
    
    public VacationSummary(Contract contract, AbsencePeriod absencePeriod) {
      this.contract = contract;
      this.absencePeriod = absencePeriod;
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
    
    public int usable() {
      if (absencePeriod.takableCountBehaviour.equals(TakeCountBehaviour.sumUntilPeriod)) {
        return accrued() - used(); 
      } else {
        return total() - used();
      }
    }
    
    public int usableTotal() { 
      return total() - used();
    }
    
    private int computeTotal(AbsencePeriod period) {
      return period
          .computePeriodTakableAmount(TakeCountBehaviour.sumAllPeriod, LocalDate.now()) / 100;
    }
    
    private int computeAccrued(AbsencePeriod period) {
      return period
          .computePeriodTakableAmount(TakeCountBehaviour.sumUntilPeriod, LocalDate.now()) / 100;
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
      for (AbsenceType takable : lastPeriod.takableCodes) {
        if (takable.code.equals(DefaultAbsenceType.A_37.name().substring(2))) {
          return period.subPeriods.get(period.subPeriods.size() - 2);
        }
      }
      return lastPeriod;
    }
    
  }
  
}
