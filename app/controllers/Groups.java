package controllers;

import com.google.common.base.Optional;
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
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.GroupManager;
import models.GeneralSetting;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.flows.Group;
import play.data.binding.As;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

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
   * @param group il gruppo da creare
   * @param office la sede su cui crearlo
   */
  public static void createGroup(
      @Valid Group group, Office office,
      @As(binder = JpaReferenceBinder.class)
      Set<Person> people) {
    log.info("affiltionaPeople = {}", people);
    if (Validation.hasErrors()) {
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
      log.info("Eliminato gruppo {}", group.name);
    } else {
      group.endDate = LocalDate.now();
      group.save();
      log.info("Disattivato gruppo {}", group.name);
    }

    flash.success(Web.msgDeleted(Group.class));
    showGroups(group.manager.office.id);
  }

  /**
   * Metodo che mostra i gruppi appartenenti a una sede.
   * @param officeId l'id della sede di cui vedere i gruppi
   */
  public static void showGroups(Long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    User user = Security.getUser().get();
    List<Group> groups = null;
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

}