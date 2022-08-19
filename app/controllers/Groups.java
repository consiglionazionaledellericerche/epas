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
import common.security.SecurityRules;
import dao.GeneralSettingDao;
import dao.GroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import helpers.Web;
import helpers.jpa.JpaReferenceBinder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.val;
import manager.GroupManager;
import models.GeneralSetting;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.dto.SeatSituationDto;
import models.flows.Group;
import org.testng.collections.Lists;
import org.testng.util.Strings;
import play.data.binding.As;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione dei gruppi.
 */
@Slf4j
@With({Resecure.class})
public class Groups extends Controller {

  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static GroupDao groupDao;
  @Inject
  private static GroupManager groupManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static GeneralSettingDao settingDao;
  @Inject
  private static UsersRolesOfficesDao uroDao;
  @Inject
  private static RoleDao roleDao;

  /**
   * Metodo che crea il gruppo.
   *
   * @param group il gruppo da creare
   * @param office la sede su cui crearlo
   */
  public static void createGroup(
      @Valid Group group, Office office,
      @As(binder = JpaReferenceBinder.class)
      Set<Person> people) {

    boolean hasErrors = Validation.hasErrors();
    if (!Strings.isNullOrEmpty(group.externalId) 
        && groupDao.byOfficeAndExternalId(office, group.externalId).isPresent()) {
      hasErrors = true;
      Validation.addError("group.externalId", "deve essere univoco");
    }
    ;

    if (hasErrors) {
      response.status = 400;
      List<Person> peopleForGroups = personDao.byInstitute(office.institute);
      log.info("Create groups errors = {}", validation.errorsMap());
      render("@edit", group, office, peopleForGroups);
    } 

    rules.checkIfPermitted(group.office);
    group.office = office;
    final boolean isNew = group.id != null;
    group.save();
    log.debug("Salvato gruppo di lavoro: {} per la sede {}", group.name, group.office);

    groupManager.updatePeople(group, people);

    UsersRolesOffices uro = new UsersRolesOffices();
    groupManager.createManager(office, group, uro);
    String message = isNew 
        ? "Nuovo gruppo di lavoro %s salvato correttamente." 
            : "Gruppo  di lavoro %s salvato correttamente.";
    flash.success(message, group.name);
    showGroups(office.id);
  }

  /**
   * Metodo che cancella il gruppo.
   *
   * @param groupId id del gruppo da cancellare
   */
  public static void deleteGroup(long groupId) {
    final Group group = Group.findById(groupId);
    notFoundIfNull(group);
    rules.checkIfPermitted(group.office);

    //elimino il ruolo di manager
    groupManager.deleteManager(group);

    if (group.getPeople().isEmpty()) {
      //Elimino eventuali vecchie associazioni
      group.affiliations.stream().forEach(a -> a.delete());
      //elimino il gruppo.
      group.delete();
      flash.success(Web.msgDeleted(Group.class));
      log.info("Eliminato gruppo {}", group.name);
    } else {
      group.endDate = LocalDate.now();
      group.save();
      log.info("Disattivato gruppo {}", group.name);
      flash.success("Gruppo disattivato con successo");
    }

    showGroups(group.office.id);
  }

  /**
   * Metodo che mostra i gruppi appartenenti a una sede.
   *
   * @param officeId l'id della sede di cui vedere i gruppi
   */
  public static void showGroups(Long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    User user = Security.getUser().get();
    List<Group> groups = Lists.newArrayList();
    if (uroDao.getUsersRolesOffices(user, roleDao.getRoleByName(Role.GROUP_MANAGER), office)
        .isPresent()) {
      groups = 
          groupDao.groupsByOffice(office, Optional.fromNullable(user.person), Optional.of(true));
    }
    if (user.isSystemUser() 
        || uroDao.getUsersRolesOffices(user, roleDao.getRoleByName(Role.PERSONNEL_ADMIN), office)
        .isPresent()) {
      groups = groupDao.groupsByOffice(office, Optional.<Person>absent(), Optional.of(true));
    }
    val activeGroups = groups.stream().filter(g -> g.isActive())
        .sorted((g1, g2) -> 
            g1.getName().toLowerCase().compareTo(g2.getName().toLowerCase()))
         .collect(Collectors.toList());
    val disabledGroups = groups.stream().filter(g -> !g.isActive())
        .sorted((g1, g2) -> 
            g1.getName().toLowerCase().compareTo(g2.getName().toLowerCase()))
        .collect(Collectors.toList());
    render(activeGroups, disabledGroups, office);
  }

  /**
   * Metodo che permette la modifica del gruppo.
   *
   * @param groupId id del gruppo da modificare
   */
  public static void edit(long groupId) {
    Group group = Group.findById(groupId);
    notFoundIfNull(group);
    rules.checkIfPermitted(group.manager.office);
    Office office = group.manager.office;
    List<Person> peopleForGroups = personDao.byInstitute(office.institute);
    render(group, office, peopleForGroups);
  }

  /**
   * Metodo che permette l'apertura della pagina di creazione del gruppo.
   *
   * @param officeId l'id della sede su cui creare il gruppo
   */
  public static void blank(long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    List<Person> peopleForGroups = null;
    GeneralSetting settings = settingDao.generalSetting();
    if (settings.handleGroupsByInstitute) {
      peopleForGroups = personDao.byInstitute(office.institute);
    } else {
      peopleForGroups = personDao.byOffice(office);
    }
    render("@edit", office, peopleForGroups);
  }
  
  /**
   * Ritorna le informazioni sui ruoli presenti nella sede di appartenenza del dipendente.
   */
  public static void seatOrganizationChart() {
    
    val currentPerson = Security.getUser().get().person;
    //Accesso da utente di sistema senza persona associata
    if (currentPerson == null) {
      Application.index();
    }
    Map<Role, List<User>> seatSupervisors = groupManager
        .createOrganizationChart(currentPerson, roleDao.getRoleByName(Role.SEAT_SUPERVISOR));
    Map<Role, List<User>> personnelAdmins = groupManager
        .createOrganizationChart(currentPerson, roleDao.getRoleByName(Role.PERSONNEL_ADMIN));
    Map<Role, List<User>> technicalAdmins = groupManager
        .createOrganizationChart(currentPerson, roleDao.getRoleByName(Role.TECHNICAL_ADMIN));
    Map<Role, List<User>> registryManagers = groupManager
        .createOrganizationChart(currentPerson, roleDao.getRoleByName(Role.REGISTRY_MANAGER));
    Map<Role, List<User>> mealTicketsManagers = groupManager
        .createOrganizationChart(currentPerson, roleDao.getRoleByName(Role.MEAL_TICKET_MANAGER));
    Map<Role, List<User>> personnelAdminsMini = groupManager
        .createOrganizationChart(currentPerson, roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI));
    Map<Role, List<User>> shiftManagers = groupManager
        .createOrganizationChart(currentPerson, roleDao.getRoleByName(Role.SHIFT_MANAGER));
    Map<Role, List<User>> reperibilityManagers = groupManager
        .createOrganizationChart(currentPerson, roleDao.getRoleByName(Role.REPERIBILITY_MANAGER));
    
    
    List<Role> roles = uroDao.getUsersRolesOfficesByUser(currentPerson.user)
        .stream().map(uro -> uro.role).collect(Collectors.toList());
    render(seatSupervisors, personnelAdmins, technicalAdmins, registryManagers, mealTicketsManagers, 
        personnelAdminsMini, shiftManagers, reperibilityManagers, currentPerson, roles);
  }
  
  public static void viewInfoRole(Long id) {
    Role role = roleDao.getRoleById(id);
    render(role);
  }

}



