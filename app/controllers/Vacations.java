/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;
import common.security.SecurityRules;
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
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.model.PeriodChain;
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

/**
 * Controller per la gestione delle ferie.
 */
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
    Preconditions.checkNotNull(currentUser.get().getPerson());

    IWrapperPerson person = wrapperFactory.create(currentUser.get().getPerson());

    if (year == null) {
      year = LocalDate.now().getYear();
    }

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
    List<VacationSituation> vacationSituations = Lists.newArrayList();

    if (year > LocalDate.now().getYear()) {
      flash.error("Anno selezionato nel futuro, mostrate le ferie dell'anno corrente");
      show(LocalDate.now().getYear());
      return;
    }
    for (Contract contract : person.orderedYearContracts(year)) {
      
      VacationSituation vacationSituation = absenceService.buildVacationSituation(contract, year, 
          vacationGroup, Optional.absent(), false);
      vacationSituations.add(vacationSituation);
    }
    
    GroupAbsenceType permissionGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.G_661.name()).get();
    PeriodChain periodChain = absenceService
        .residual(person.getValue(), permissionGroup, LocalDate.now());
    AbsenceForm categorySwitcher = absenceService
        .buildForCategorySwitch(person.getValue(), LocalDate.now(), permissionGroup);
    
    boolean showVacationPeriods = true;
    render(vacationSituations, year, showVacationPeriods, periodChain, categorySwitcher);
  }
  
  /**
   * Situazione di riepilogo contratto per il dipendente.
   *
   * @param contractId contratto
   * @param year anno
   * @param type VACATION/PERMISSION
   */
  public static void personVacationSummary(Long contractId, Integer year, TypeSummary type) {
    
    Contract contract = contractDao.getContractById(contractId);
    Optional<User> currentUser = Security.getUser();
    if (contract == null || type == null 
        || !currentUser.isPresent() || currentUser.get().getPerson() == null 
        || !contract.getPerson().equals(currentUser.get().getPerson())) {
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
      for (Contract contract : person.getContracts()) {

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
   *
   * @param contractId contratto
   * @param year anno
   * @param type VACATION/PERMISSION
   */
  public static void vacationSummary(Long contractId, Integer year, TypeSummary type) {
    
    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    notFoundIfNull(type);
    rules.checkIfPermitted(contract.getPerson().getOffice());
    
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
