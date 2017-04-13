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

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation;
import manager.services.absences.model.VacationSituation.VacationSummary;
import manager.services.absences.model.VacationSituation.VacationSummary.TypeSummary;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;

import models.Contract;
import models.Office;
import models.Person;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.DefaultGroup;

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
          VacationSituation vacationSituation = new VacationSituation(person, 
              contract, year, vacationGroup, 
              absenceService, null);
          vacationSituations.add(vacationSituation);
        } catch (Exception ex) {
          log.info("");
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

    render(vacationSituations, isVacationLastYearExpired, isVacationCurrentYearExpired,
        isPermissionCurrentYearExpired, year, office);
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
      vacationSummary = new VacationSituation(contract.person, 
          contract, year, vacationGroup, absenceService, null).permissions;
    } else {
      vacationSummary = new VacationSituation(contract.person, 
          contract, year, vacationGroup, absenceService, null).currentYear;
    }
    
    renderTemplate("Vacations/vacationSummary.html", vacationSummary);
  }
  
  /**
   * Confronto vecchio/nuovo algoritmo.
   */
  public static void compareVacations(Integer year, Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate beginYear = new LocalDate(year, 1, 1);
    LocalDate endYear = new LocalDate(year, 12, 31);
    DateInterval yearInterval = new DateInterval(beginYear, endYear);
    
    List<Person> personList = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false, beginYear, endYear, true).list();
    
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
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
          VacationSituation vacationSituation = new VacationSituation(person, 
              contract, year, vacationGroup, 
              absenceService, vacationsService);
          vacationSituations.add(vacationSituation);
        } catch (Exception ex) {
          log.info("");
        }
      }
    }
    
    render(year, vacationSituations);
  }

}
