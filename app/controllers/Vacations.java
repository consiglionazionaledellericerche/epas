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
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation;
import manager.services.absences.model.VacationSituation.VacationSummary;
import manager.services.absences.model.VacationSituation.VacationSummary.TypeSummary;
import models.Contract;
import models.Office;
import models.Person;
import models.User;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@With({Resecure.class})
@Slf4j
public class Vacations extends Controller {

  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static ContractDao contractDao;

  /**
   * Vista riepiloghi ferie per l'employee.
   *
   * @param year anno.
   */
  public static void show(Integer year) {

    Optional<User> currentUser = Security.getUser();
    Preconditions.checkState(currentUser.isPresent());
    Preconditions.checkNotNull(currentUser.get().person);

    IWrapperPerson person = wrapperFactory.create(currentUser.get().person);

    if (year == null) {
      year = LocalDate.now().getYear();
    }

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
    List<VacationSituation> vacationSituations = Lists.newArrayList();

    for (Contract contract : person.orderedYearContracts(year)) {
      
      VacationSituation vacationSituation = absenceService.buildVacationSituation(contract, year, 
          vacationGroup, Optional.absent(), false);
      vacationSituations.add(vacationSituation);
    }
    boolean showVacationPeriods = true;
    render(vacationSituations, year, showVacationPeriods);
  }
  
  /**
   * Situazione di riepilogo contratto per il dipendente.
   * @param contractId contratto
   * @param year anno
   * @param type VACATION/PERMISSION
   */
  public static void personVacationSummary(Long contractId, Integer year, TypeSummary type) {
    
    Contract contract = contractDao.getContractById(contractId);
    Optional<User> currentUser = Security.getUser();
    if (contract == null || type == null 
        || !currentUser.isPresent() || currentUser.get().person == null 
        || !contract.person.equals(currentUser.get().person)) {
      forbidden();
    }
    
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    VacationSummary vacationSummary;
    if (type.equals(TypeSummary.PERMISSION)) {
      vacationSummary = absenceService.buildVacationSituation(contract, year, vacationGroup, 
          Optional.absent(), false).permissions;
    } else {
      vacationSummary = absenceService.buildVacationSituation(contract, year, vacationGroup, 
          Optional.absent(), false).currentYear;
    }
    
    renderTemplate("Vacations/vacationSummary.html", vacationSummary);
  }


  /**
   * Riepiloghi ferie della sede.
   *
   * @param year     anno
   * @param officeId sede
   */
  public static void list(Integer year, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate beginYear = new LocalDate(year, 1, 1);
    LocalDate endYear = new LocalDate(year, 12, 31);
    DateInterval yearInterval = new DateInterval(beginYear, endYear);
    
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();

    List<Person> personList = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false, beginYear, endYear, true).list();

    List<VacationSituation> vacationSituations = Lists.newArrayList();
    
    for (Person person : personList) {
      for (Contract contract : person.contracts) {

        IWrapperContract cwrContract = wrapperFactory.create(contract);
        if (DateUtility.intervalIntersection(cwrContract.getContractDateInterval(),
            yearInterval) == null) {

          //Questo evento andrebbe segnalato... la list dovrebbe caricare
          // nello heap solo i contratti attivi nel periodo specificato.
          continue;
        }
        
        try {
          VacationSituation vacationSituation = absenceService.buildVacationSituation(contract, 
              year, vacationGroup, Optional.absent(), true);
          vacationSituations.add(vacationSituation);
        } catch (Exception ex) {
          log.info("Impossibile creare la situazione delle ferie di {}", person.getFullname());
        }

      }
    }

    boolean isVacationLastYearExpired = absenceService.isVacationsLastYearExpired(year, office);

    boolean isVacationCurrentYearExpired = 
        absenceService.isVacationsLastYearExpired(year + 1, office);

    boolean isPermissionCurrentYearExpired = false;
    if (new LocalDate(year, 12, 31).isBefore(LocalDate.now())) {
      isPermissionCurrentYearExpired = true;
    }

    render(vacationSituations, isVacationLastYearExpired, isVacationCurrentYearExpired,
        isPermissionCurrentYearExpired, year, office);
  }
  
  /**
   * Situazione di riepilogo contratto.
   * @param contractId contratto
   * @param year anno
   * @param type VACATION/PERMISSION
   */
  public static void vacationSummary(Long contractId, Integer year, TypeSummary type) {
    
    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    notFoundIfNull(type);
    rules.checkIfPermitted(contract.person.office);
    
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    VacationSummary vacationSummary;
    if (type.equals(TypeSummary.PERMISSION)) {
      vacationSummary = absenceService.buildVacationSituation(contract, year, vacationGroup, 
          Optional.absent(), false).permissions;
    } else {
      vacationSummary = absenceService.buildVacationSituation(contract, year, vacationGroup, 
          Optional.absent(), false).currentYear;
    }
    
    render(vacationSummary);
  }
 

}
