package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.SecureManager;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;

import models.Contract;
import models.Office;
import models.Person;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

@With({Resecure.class})
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
  private static SecurityRules rules;
  @Inject
  private static ContractDao contractDao;

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
