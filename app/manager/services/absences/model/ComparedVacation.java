package manager.services.absences.model;

import com.google.common.base.Optional;

import dao.wrapper.IWrapperPerson;

import manager.services.absences.AbsenceService;
import manager.services.absences.certifications.CertificationYearSituation.AbsenceSituation;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;

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
public class ComparedVacation {

  public Person person;
  public Contract contract;
  
  public AbsencePeriod periodsLastYear;
  public AbsencePeriod periodsCurrentYear;
  public AbsencePeriod periodsPermissions;
  public VacationsRecap vacationRecap;
  
  public AbsenceSituation certLastYear;
  public AbsenceSituation certCurrentYear;
  public AbsenceSituation certPermission;
  
  /**
   * Costruttore.
   */
  public ComparedVacation(IWrapperPerson wrPerson, Contract contract, int year, 
      GroupAbsenceType vacationGroup,
      AbsenceService absenceService, IVacationsService vacationsService) {
    
    this.contract = contract;
    
    Optional<VacationsRecap> vr = vacationsService.create(year, 
        wrPerson.getCurrentContract().get());
    if (vr.isPresent()) {
      this.vacationRecap = vr.get();
    }

    LocalDate date = LocalDate.now();
    if (date.getYear() > year) {
      date = new LocalDate(year, 12, 31);
    }
    PeriodChain periodChain = absenceService.residual(wrPerson.getValue(), 
        vacationGroup, date);
    this.person = wrPerson.getValue();
    this.periodsLastYear = periodChain.vacationSupportList.get(0).get(0);
    this.periodsCurrentYear = periodChain.vacationSupportList.get(1).get(0);
    this.periodsPermissions = periodChain.vacationSupportList.get(2).get(0);
    
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
  
  //Old alg.
  //2016
  public int oldLastYearTotal() {
    return vacationRecap.getVacationsLastYear().getTotal();
  }
  
  public int oldLastYearPostPartum() {
    return vacationRecap.getVacationsLastYear().getTotalResult().getPostPartum().size();
  }
  
  public int oldLastYearAccrued() {
    return vacationRecap.getVacationsLastYear().getAccrued();
  }
  
  public boolean oldLastYearContractLowerLimit() {
    return vacationRecap.getVacationsLastYear().isContractLowerLimit();
  }
  
  public LocalDate oldLastYearLowerLimit() {
    return vacationRecap.getVacationsLastYear().getLowerLimit();
  }
  
  public boolean oldLastYearContractUpperLimit() {
    return vacationRecap.getVacationsLastYear().isContractUpperLimit();
  }
  
  public LocalDate oldLastYearUpperLimit() {
    return vacationRecap.getVacationsLastYear().getUpperLimit();
  }
  
  //2017
  public int oldCurrentYearTotal() {
    return vacationRecap.getVacationsCurrentYear().getTotal();
  }
  
  public int oldCurrentYearPostPartum() {
    return vacationRecap.getVacationsCurrentYear().getTotalResult().getPostPartum().size();
  }
  
  public int oldCurrentYearAccrued() {
    return vacationRecap.getVacationsCurrentYear().getAccrued();
  }
  
  //Permission
  public int oldPermissionTotal() {
    return vacationRecap.getPermissions().getTotal();
  }
  
  public int oldPermissionPostPartum() {
    return vacationRecap.getPermissions().getTotalResult().getPostPartum().size();
  }
  
  public int oldPermissionAccrued() {
    return vacationRecap.getPermissions().getAccrued();
  }
  
  //NEW 2016/////////////////////////////////////////////
  public int newLastYearTotal() {
    return computeTotal(periodsLastYear);
  }
  
  /**
   * Nuova implementazione: posPartum anno passato.
   */
  public int newLastYearPostPartum() {
    if (periodsLastYear == null) {
      return 0;
    }
    return periodsLastYear.reducingAbsences.size();
  }
  
  public int newLastYearAccrued() {
    return computeAccrued(periodsLastYear);
  }
  
  public boolean newLastYearContractLowerLimit() {
    return isContractLowerLimit(periodsLastYear.from);
  }
  
  public LocalDate newLastYearLowerLimit() {
    return periodsLastYear.from;
  }
  
  public boolean newLastYearContractUpperLimit() {
    return isContractUpperLimit(lastNaturalPeriod(periodsLastYear).to);
  }
  
  public LocalDate newLastYearUpperLimit() {
    return lastNaturalPeriod(periodsLastYear).to;
  }
  
  //NEW 2017/////////////////////////////////////////////
  public int newCurrentYearTotal() {
    return computeTotal(periodsCurrentYear);
  }

  /**
   * Nuova implementazione: posPartum anno corrente.
   */
  public int newCurrentYearPostPartum() {
    if (periodsCurrentYear == null) {
      return 0;
    }
    return periodsCurrentYear.reducingAbsences.size();
  }
  
  public int newCurrentYearAccrued() {
    return computeAccrued(periodsCurrentYear);
  }
  
  public boolean newCurrentYearContractLowerLimit() {
    return isContractLowerLimit(periodsCurrentYear.from);
  }
  
  public LocalDate newCurrentYearLowerLimit() {
    return periodsCurrentYear.from;
  }
  
  public boolean newCurrentYearContractUpperLimit() {
    return isContractUpperLimit(lastNaturalPeriod(periodsCurrentYear).to);
  }
  
  public LocalDate newCurrentYearUpperLimit() {
    return lastNaturalPeriod(periodsCurrentYear).to;
  }
  
  
  //NEW PERMESSI /////////////////////////////////////////////
  
  public int newPermissionTotal() {
    return computeTotal(periodsPermissions);
  }
  
  /**
   * Nuova implementazione: posPartum permessi.
   */
  public int newPermissionPostPartum() {
    if (periodsPermissions == null) {
      return 0;
    }
    return periodsPermissions.reducingAbsences.size();
  }
  
  public int newPermissionAccrued() {
    return computeAccrued(periodsPermissions);
  }
  
  public boolean newPermissionContractLowerLimit() {
    return isContractLowerLimit(periodsPermissions.from);
  }
  
  public LocalDate newPermissionLowerLimit() {
    return periodsPermissions.from;
  }
  
  public boolean newPermissionContractUpperLimit() {
    return isContractUpperLimit(lastNaturalPeriod(periodsPermissions).to);
  }
  
  public LocalDate newPermissionUpperLimit() {
    return lastNaturalPeriod(periodsPermissions).to;
  }
  
  //NEW OLD COMPARE /////////////////////////////////////////
  /**
   * Confronto.
   */
  public boolean epasEquivalent() {
    if (vacationRecap == null) {
      return true;
    }
    if (oldLastYearTotal() != newLastYearTotal()) {
      return false;
    }
    if (oldCurrentYearTotal() != newCurrentYearTotal()) {
      return false;
    }
    if (oldPermissionTotal() != newPermissionTotal()) {
      return false;
    }
    if (oldLastYearPostPartum() != newLastYearPostPartum()) {
      return false;
    }
    if (oldCurrentYearPostPartum() != newCurrentYearPostPartum()) {
      return false;
    }
    if (oldPermissionPostPartum() != newPermissionPostPartum()) {
      return false;
    }
    if (oldLastYearAccrued() != newLastYearAccrued()) {
      return false;
    }
    if (oldCurrentYearAccrued() != newCurrentYearAccrued()) {
      return false;
    }
    if (oldPermissionAccrued() != newPermissionAccrued()) {
      return false;
    }
    return true;
  }
  
  //att

  /**
   * Totali attestati, anno passato. 
   */
  public int attestatiLastYearTotal() {
    if (certLastYear == null) {
      return -1;
    }
    return certLastYear.totalUsable;
  }

  /**
   * Totali attestati, anno corrente. 
   */
  public int attestatiCurrentYearTotal() {
    if (certCurrentYear == null) {
      return -1;
    }
    return certCurrentYear.totalUsable;
  }

  /**
   * Totali attestati, permessi. 
   */
  public int attestatiPermissionTotal() {
    if (certPermission == null) {
      return -1;
    }
    return certPermission.totalUsable;
  }
  
  private int computeTotal(AbsencePeriod period) {
    return period
        .computePeriodTakableAmount(TakeCountBehaviour.sumAllPeriod, LocalDate.now()) / 100;
  }
  
  private int computeAccrued(AbsencePeriod period) {
    return period
        .computePeriodTakableAmount(TakeCountBehaviour.sumUntilPeriod, LocalDate.now()) / 100;
  }
  
  /**
   * L'ultimo periodo (per la data fine). Se l'ultimo periodo è l'estensione 37 seleziono quello
   * ancora precedente.
   */
  private AbsencePeriod lastNaturalPeriod(AbsencePeriod period) {
    AbsencePeriod lastPeriod = period.subPeriods.get(period.subPeriods.size() - 1);
    boolean finded37 = false;
    for (AbsenceType takable : lastPeriod.takableCodes) {
      if (takable.code.equals(DefaultAbsenceType.A_37.name().substring(2))) {
        return period.subPeriods.get(period.subPeriods.size() - 2);
      }
    }
    return lastPeriod;
  }
  
  private boolean isContractUpperLimit(LocalDate date) {
    if (contract.calculatedEnd() != null 
        && contract.calculatedEnd().isBefore(date)) {
      return true;
    }
    return false;
  }
  
  private boolean isContractLowerLimit(LocalDate date) {
    if (contract.beginDate.isAfter(date)) {
      return true;
    }
    return false;
  }
  
}
