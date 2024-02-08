/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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


import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import common.security.SecurityRules;
import dao.CompetenceDao;
import dao.GeneralSettingDao;
import dao.GroupDao;
import dao.GroupOvertimeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonOvertimeDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import helpers.Web;
import helpers.jpa.JpaReferenceBinder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.CompetenceManager;
import manager.GroupManager;
import manager.GroupOvertimeManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.GeneralSetting;
import models.GroupOvertime;
import models.Office;
import models.Person;
import models.PersonOvertime;
import models.Role;
import models.TotalOvertime;
import models.User;
import models.UsersRolesOffices;
import models.dto.PersonOvertimeInMonth;
import models.flows.Group;
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
  @Inject
  private static CompetenceDao competenceDao;
  @Inject
  private static CompetenceManager competenceManager;
  @Inject
  private static GroupOvertimeDao groupOvertimeDao;
  @Inject
  private static GroupOvertimeManager groupOvertimeManager;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static PersonOvertimeDao personOvertimeDao;

  /**
   * Metodo che crea il gruppo.
   *
   * @param group il gruppo da creare
   * @param office la sede su cui crearlo
   */
  public static void createGroup(
      Group group, Office office,
      @As(binder = JpaReferenceBinder.class)
      Set<Person> people) {

    boolean hasErrors = Validation.hasErrors();
    if (!Strings.isNullOrEmpty(group.getExternalId()) 
        && groupDao.byOfficeAndExternalId(office, group.getExternalId()).isPresent()) {
      hasErrors = true;
      Validation.addError("group.externalId", "deve essere univoco");
    }

    if (hasErrors) {
      response.status = 400;
      List<Person> peopleForGroups = personDao.byInstitute(office.getInstitute());
      log.info("Create groups errors = {}", validation.errorsMap());
      render("@edit", group, office, peopleForGroups);
    } 

    rules.checkIfPermitted(group.getOffice());
    group.setOffice(office);
    final boolean isNew = group.id != null;
    group.save();
    log.debug("Salvato gruppo di lavoro: {} per la sede {}", group.getName(), group.getOffice());

    groupManager.updatePeople(group, people);

    UsersRolesOffices uro = new UsersRolesOffices();
    groupManager.createManager(office, group, uro);
    String message = isNew 
        ? "Nuovo gruppo di lavoro %s salvato correttamente." 
            : "Gruppo  di lavoro %s salvato correttamente.";
    flash.success(message, group.getName());
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
    rules.checkIfPermitted(group.getOffice());

    //elimino il ruolo di manager
    groupManager.deleteManager(group);

    if (group.getPeople().isEmpty()) {
      //Elimino eventuali vecchie associazioni
      group.getAffiliations().stream().forEach(a -> a.delete());
      //elimino il gruppo.
      group.delete();
      flash.success(Web.msgDeleted(Group.class));
      log.info("Eliminato gruppo {}", group.getName());
    } else {
      group.setEndDate(LocalDate.now());
      group.save();
      log.info("Disattivato gruppo {}", group.getName());
      flash.success("Gruppo disattivato con successo");
    }

    showGroups(group.getOffice().id);
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
      groups = groupDao.groupsByOffice(office, 
          Optional.fromNullable(user.getPerson()), Optional.of(true));
    }
    if (user.isSystemUser() 
        || uroDao.getUsersRolesOffices(user, roleDao.getRoleByName(Role.PERSONNEL_ADMIN), office)
        .isPresent()
        || uroDao.getUsersRolesOffices(user, roleDao.getRoleByName(Role.SEAT_SUPERVISOR), office)
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
    rules.checkIfPermitted(group.getManager().getOffice());
    Office office = group.getManager().getOffice();
    List<Person> peopleForGroups = personDao.byInstitute(office.getInstitute());
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
    if (settings.isHandleGroupsByInstitute()) {
      peopleForGroups = personDao.byInstitute(office.getInstitute());
    } else {
      peopleForGroups = personDao.byOffice(office);
    }
    render("@edit", office, peopleForGroups);
  }
  
  /**
   * Ritorna le informazioni sui ruoli presenti nella sede di appartenenza del dipendente.
   */
  public static void seatOrganizationChart() {
    
    val currentPerson = Security.getUser().get().getPerson();
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
    
    
    List<Role> roles = uroDao.getUsersRolesOfficesByUser(currentPerson.getUser())
        .stream().map(uro -> uro.getRole()).collect(Collectors.toList());
    render(seatSupervisors, personnelAdmins, technicalAdmins, 
        registryManagers, mealTicketsManagers, personnelAdminsMini, shiftManagers, 
        reperibilityManagers, currentPerson, roles);
  }
  
  public static void viewInfoRole(Long id) {
    Role role = roleDao.getRoleById(id);
    render(role);
  }
  
  /**
   * Ritorna la pagina di gestione delle ore di straordinario da associare al gruppo.
   * @param groupId l'identificativo del gruppo
   */
  public static void handleOvertimeGroup(Long groupId) {
    Group group = Group.findById(groupId);
    notFoundIfNull(group);
    rules.checkIfPermitted(group.getOffice());
    //La quantità di ore di straordinario accordate al gruppo nell'anno
    int year = LocalDate.now().getYear();
    int totalGroupOvertimes = group.getGroupOvertimes().stream()
        .filter(go -> go.getYear().equals(year))
        .mapToInt(go -> go.getNumberOfHours()).sum();
    //Recupero il monte ore della sede decurtandolo di eventuali assegnamenti ad atri gruppi
    List<TotalOvertime> totalList = competenceDao
        .getTotalOvertime(LocalDate.now().getYear(), group.getOffice());
    int totale = competenceManager.getTotalOvertime(totalList);
    List<Group> groupList = group.getOffice().getGroups().stream()
        .filter(g -> !g.getName().equals(group.getName())).collect(Collectors.toList());
    int groupOvertimeSum = 0;
    for (Group otherGroup : groupList)  {
      List<GroupOvertime> groupOvertimeList = groupOvertimeDao
          .getByYearAndGroup(year, otherGroup);
      groupOvertimeSum = groupOvertimeSum + groupOvertimeList.stream()
      .mapToInt(go -> go.getNumberOfHours()).sum();
    }
    Map<Integer, List<PersonOvertimeInMonth>> mapFirst = 
        groupOvertimeManager.groupOvertimeSituationInYear(group.getPeople(), 
            year);
    Ordering naturalOrderingDesc = Ordering.natural().reverse();
    Map<Integer, List<PersonOvertimeInMonth>> map = 
        ImmutableSortedMap.copyOf(mapFirst, naturalOrderingDesc);
    int overtimeAssigned = groupOvertimeManager.groupOvertimeAssignedInYear(mapFirst);
    int groupOvertimesAvailable = totalGroupOvertimes - overtimeAssigned; 
    int hoursAvailable = totale - totalGroupOvertimes - groupOvertimeSum;
    Office office = group.getOffice();
    GroupOvertime groupOvertime = new GroupOvertime();
    List<GroupOvertime> groupOvertimeInYearList = group.getGroupOvertimes().stream()
        .filter(go -> go.getYear().equals(year)).collect(Collectors.toList());
    boolean check = ((Boolean) configurationManager
        .configValue(office, EpasParam.ENABLE_OVERTIME_PER_PERSON));
    render(group, totalGroupOvertimes, office, groupOvertime, hoursAvailable, map, 
        groupOvertimesAvailable, groupOvertimeInYearList, year, check, totale);
  }
  
  public static void addHours(Long personId, int year) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
    List<PersonOvertime> personOvertimes = personOvertimeDao.personListInYear(person, year);
    PersonOvertime personOvertime = new PersonOvertime();
    render(personOvertimes, person, year, personOvertime);
  }
  
  public static void saveHours(PersonOvertime personOvertime, 
      int year, Long personId) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
    Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
    
    if (personOvertime.getNumberOfHours() == null 
        || !pattern.matcher(personOvertime.getNumberOfHours().toString()).matches()) {
      Validation.addError("personOvertime.getNumberOfHours", "Inserire una quantità numerica!");
    }
    if (personOvertime.getDateOfUpdate() == null) {
      Validation.addError("personOvertime.dateOfUpdate", "Inserire una data valida!!");
    }
    if (personOvertime.getDateOfUpdate().getYear() != year) {
      Validation.addError("personOvertime.dateOfUpdate", 
          "Si sta inserendo una quantità per un anno diverso da quello specificato nella data!!");
    }
    int totalOvertimes = 0;
    List<GroupOvertime> list = person.getGroups().stream().flatMap(g -> g.getGroupOvertimes().stream()
        .filter(go -> go.getYear().equals(LocalDate.now().getYear())))
        .collect(Collectors.toList());
    totalOvertimes = list.stream().mapToInt(go -> go.getNumberOfHours()).sum();
    if (personOvertime.getNumberOfHours() > totalOvertimes) {
      Validation.addError("personOvertime.numberOfHours",
          "Si sta inserendo una quantità che supera il limite massimo di ore previste "
          + "dalla propria configurazione. Aggiungere monte ore al gruppo o alla sede!");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      render("@addHours", person, year, personOvertime);
    }
    personOvertime.setPerson(person);
    personOvertime.setYear(year);
    personOvertime.save();
    flash.success("Aggiunta nuova quantità al monte ore per straordinari di %s", 
        person.getFullname());
    handleOvertimeGroup(person.getGroups().get(0).id);
  }

}



