package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.SecureManager;
import manager.attestati.dto.internal.CruscottoDipendente;
import manager.attestati.service.CertificationService;
import manager.services.absences.AbsenceService;
import manager.services.absences.certifications.CertificationYearSituation;
import manager.services.absences.certifications.CertificationYearSituation.AbsenceImportType;
import manager.services.absences.certifications.CertificationYearSituation.AbsenceSituation;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.PeriodChain;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;

import models.Contract;
import models.Office;
import models.Person;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.DefaultGroup;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

@With({Resecure.class})
@Slf4j
public class VacationsAdmin extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecureManager secureManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static IVacationsService vacationsService;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static ContractDao contractDao;
  @Inject
  private static CertificationService certService;

  /**
   * Riepiloghi ferie della sede.
   *
   * @param year     anno
   * @param officeId sede
   */
  public static void list(Integer year, Long officeId) {

    Set<Office> offices = secureManager.officesReadAllowed(Security.getUser().get());
    if (offices.isEmpty()) {
      forbidden();
    }
    if (officeId == null) {
      officeId = offices.iterator().next().id;
    }
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate beginYear = new LocalDate(year, 1, 1);
    LocalDate endYear = new LocalDate(year, 12, 31);
    DateInterval yearInterval = new DateInterval(beginYear, endYear);

    List<Person> personList = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false, beginYear, endYear, true).list();

    //List<Person> personList = simpleResults.paginated(page).getResults();

    List<VacationsRecap> vacationsList = Lists.newArrayList();

    List<Contract> contractsWithVacationsProblems = Lists.newArrayList();

    for (Person person : personList) {

      for (Contract contract : person.contracts) {

        IWrapperContract cwrContract = wrapperFactory.create(contract);
        if (DateUtility.intervalIntersection(cwrContract.getContractDateInterval(),
            yearInterval) == null) {

          //Questo evento andrebbe segnalato... la list dovrebbe caricare
          // nello heap solo i contratti attivi nel periodo specificato.
          continue;
        }

        Optional<VacationsRecap> vr = vacationsService.create(year, contract);

        if (vr.isPresent()) {
          vacationsList.add(vr.get());

        } else {
          contractsWithVacationsProblems.add(contract);
        }
      }
    }

    boolean isVacationLastYearExpired = vacationsService.isVacationsLastYearExpired(year,
        vacationsService.vacationsLastYearExpireDate(year, office));

    boolean isVacationCurrentYearExpired = vacationsService.isVacationsLastYearExpired(year + 1,
        vacationsService.vacationsLastYearExpireDate(year + 1, office));

    boolean isPermissionCurrentYearExpired = false;
    if (new LocalDate(year, 12, 31).isBefore(LocalDate.now())) {
      isPermissionCurrentYearExpired = true;
    }

    render(vacationsList, isVacationLastYearExpired, isVacationCurrentYearExpired,
        isPermissionCurrentYearExpired, contractsWithVacationsProblems, year, offices, office);
  }
  
  public static void compareVacations(Integer year, Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    List<Person> personList = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false, 
        new LocalDate(year, 1, 1), new LocalDate(year, 12, 31), true).list();
    
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
    List<ComparedVacation> comparedVacationList = Lists.newArrayList();
    
    for (Person person : personList) {

//      if (comparedVacationList.size() >= 10) { 
//        break;
//      }
      
      IWrapperPerson wrPerson = wrapperFactory.create(person);
//      if (!isDefined(wrPerson)) {
//        continue;
//      }
      if (!isActive(wrPerson)) {
        continue;
      }
            
      
      log.info("person {}", person.fullName());
      ComparedVacation comparedVacation = new ComparedVacation();

      Optional<VacationsRecap> vr = vacationsService.create(year, 
          wrPerson.getCurrentContract().get());
      if (vr.isPresent()) {
        comparedVacation.vacationRecap = vr.get();
      } else {
        continue;
      }

//      if (vr.get().getVacationsCurrentYear().getTotal() == 28) {
//        continue;
//      }
      
      if (person.surname.equals("Moretti")) {
        System.out.println();
      }
      
      PeriodChain periodChain = absenceService.residual(person, vacationGroup, LocalDate.now());
      comparedVacation.person = person;
      comparedVacation.periodsLastYear = periodChain.vacationSupportList.get(0).get(0);
      comparedVacation.periodsCurrentYear = periodChain.vacationSupportList.get(1).get(0);
      comparedVacation.periodsPermissions = periodChain.vacationSupportList.get(2).get(0);
      
//      try {
//        CruscottoDipendente cruscottoDipendente = certService.getCruscottoDipendente(person, year);
//        CertificationYearSituation yearSituation = 
//            new CertificationYearSituation(absenceComponentDao, person, cruscottoDipendente);
//        comparedVacation.certLastYear = yearSituation
//            .getAbsenceSituation(AbsenceImportType.FERIE_ANNO_PRECEDENTE);
//        comparedVacation.certCurrentYear = yearSituation
//            .getAbsenceSituation(AbsenceImportType.FERIE_ANNO_CORRENTE);
//        comparedVacation.certPermission = yearSituation
//            .getAbsenceSituation(AbsenceImportType.PERMESSI_LEGGE);
//        
//      } catch (Exception e) {
//        log.info("Impossibile scaricare l'informazione da attestati di {}", person.fullName());
//      }
      comparedVacationList.add(comparedVacation);
    }
    
    render(year, office, comparedVacationList);
  }
  
  private static boolean isActive(IWrapperPerson person) {
    if (!person.getCurrentContract().isPresent()) {
      return false;
    }
    return true;
  }
  
  private static boolean isUndefined(IWrapperPerson person) {
    
    if (person.getValue().surname.equals("Boni")) {
      return false;
    }
    
    if (!person.getCurrentContract().isPresent()) {
      return false;
    }
    
    if (person.getCurrentContract().get().calculatedEnd() == null) {
      return true; //OK
    }

    return false;
  }
  
  private static boolean isDefined(IWrapperPerson person) {
    
//    if (!person.getValue().surname.equals("Cresci")) {
//      return false;
//    }
    
    if (person.getValue().surname.equals("Boni")) {
      return false;
    }
    
    if (!person.getCurrentContract().isPresent()) {
      return false;
    }
    
    if (person.getCurrentContract().get().calculatedEnd() != null) {
      return true; //OK
    }

    return false;
  }
  
  public static class ComparedVacation {
    
    public Person person;
    public Contract contract;
    
    public AbsencePeriod periodsLastYear;
    public AbsencePeriod periodsCurrentYear;
    public AbsencePeriod periodsPermissions;
    public VacationsRecap vacationRecap;
    
    public AbsenceSituation certLastYear;
    public AbsenceSituation certCurrentYear;
    public AbsenceSituation certPermission;
    
    //Old alg.
    
    public int oldLastYearTotal() {
      return vacationRecap.getVacationsLastYear().getTotal();
    }
    
    public int oldLastYearPostPartum() {
      return vacationRecap.getVacationsLastYear().getTotalResult().getPostPartum().size();
    }
    
    public int oldLastYearAccrued() {
      return vacationRecap.getVacationsLastYear().getAccrued();
    }
    
    public int oldCurrentYearTotal() {
      return vacationRecap.getVacationsCurrentYear().getTotal();
    }
    
    public int oldCurrentYearPostPartum() {
      return vacationRecap.getVacationsCurrentYear().getTotalResult().getPostPartum().size();
    }
    
    public int oldCurrentYearAccrued() {
      return vacationRecap.getVacationsCurrentYear().getAccrued();
    }
    
    public int oldPermissionTotal() {
      return vacationRecap.getPermissions().getTotal();
    }
    
    public int oldPermissionPostPartum() {
      return vacationRecap.getPermissions().getTotalResult().getPostPartum().size();
    }
    
    public int oldPermissionAccrued() {
      return vacationRecap.getPermissions().getAccrued();
    }
    
    //New alg
    
    public int newLastYearTotal() {
      return computeTotal(periodsLastYear);
    }
    
    public int newLastYearAccrued() {
      return computeAccrued(periodsLastYear);
    }
    
    public int newCurrentYearTotal() {
      return computeTotal(periodsCurrentYear);
    }
    
    public int newCurrentYearAccrued() {
      return computeAccrued(periodsCurrentYear);
    }
    
    public int newPermissionTotal() {
      return computeTotal(periodsPermissions);
    }
    
    public int newPermissionAccrued() {
      return computeAccrued(periodsPermissions);
    }
    
    public boolean epasEquivalent() {
      if (oldLastYearTotal() != newLastYearTotal()) {
        return false;
      }
      if (oldCurrentYearTotal() != newCurrentYearTotal()) {
        return false;
      }
      if (oldPermissionTotal() != newPermissionTotal()) {
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
    
    public int attestatiLastYearTotal() {
      if (certLastYear == null) {
        return -1;
      }
      return certLastYear.totalUsable;
    }
    
    public int attestatiCurrentYearTotal() {
      if (certCurrentYear == null) {
        return -1;
      }
      return certCurrentYear.totalUsable;
    }
    
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


  }

  /**
   * Riepilogo ferie anno corrente.
   *
   * @param contractId contratto
   * @param anno       anno
   */
  public static void vacationsCurrentYear(Long contractId, Integer anno) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    Optional<VacationsRecap> vr = vacationsService.create(anno, contract);

    Preconditions.checkState(vr.isPresent());

    VacationsRecap vacationsRecap = vr.get();

    boolean activeVacationCurrentYear = true;

    renderTemplate("Vacations/recapVacation.html", vacationsRecap, activeVacationCurrentYear);
  }

  /**
   * Riepilogo ferie anno passato.
   *
   * @param contractId contratto
   * @param anno       anno
   */
  public static void vacationsLastYear(Long contractId, Integer anno) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    Optional<VacationsRecap> vr = vacationsService.create(anno, contract);

    Preconditions.checkState(vr.isPresent());

    VacationsRecap vacationsRecap = vr.get();

    boolean activeVacationLastYear = true;

    renderTemplate("Vacations/recapVacation.html", vacationsRecap, activeVacationLastYear);
  }


  /**
   * Riepilogo permessi anno corrente.
   *
   * @param contractId contratto
   * @param anno       anno
   */
  public static void permissionCurrentYear(Long contractId, Integer anno) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    Optional<VacationsRecap> vr = vacationsService.create(anno, contract);

    Preconditions.checkState(vr.isPresent());

    VacationsRecap vacationsRecap = vr.get();

    boolean activePermission = true;

    renderTemplate("Vacations/recapVacation.html", vacationsRecap, activePermission);
  }

}
