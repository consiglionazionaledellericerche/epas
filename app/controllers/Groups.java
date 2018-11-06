package controllers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.testng.collections.Lists;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import dao.GroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDao.PersonLite;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import helpers.Web;
import lombok.extern.slf4j.Slf4j;
import manager.GroupManager;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.flows.Group;
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

  /**
   * Metodo che crea il gruppo.
   * @param group il gruppo da creare
   * @param office la sede su cui crearlo
   */
  public static void createGroup(@Valid Group group, Office office) {

    if (Validation.hasErrors()) {
      response.status = 400;
      render("@blank", office);
    }
    rules.checkIfPermitted(group.manager.office);
    group.save();
    log.debug("Salvato nuovo gruppo di lavoro: {}", group.name);
    UsersRolesOffices uro = new UsersRolesOffices();
    groupManager.createManager(office, group, uro);
    
    flash.success("Nuovo gruppo  di lavoro %s salvato correttamente.",
        group.name);
    showGroups(office.id);
  }

  /**
   * Metodo che cancella il gruppo.
   * @param groupId id del gruppo da cancellare
   */
  public static void deleteGroup(long groupId) {
    final Group group = Group.findById(groupId);
    notFoundIfNull(group);
    rules.checkIfPermitted(group.manager.office);

    //elimino il ruolo di manager
    if (!groupManager.deleteManager(group)) {
      flash.error("Non esiste un manager associato al gruppo {}. "
          + "Impossibile eliminarlo.", group.name);
      showGroups(group.manager.office.id);
    } 
    //elimino il gruppo.
    group.delete();
    log.debug("Eliminato gruppo {}", group.name);
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
    if (user.isSystemUser()) {
      groups = groupDao.groupsByOffice(office, Optional.<Person>absent());
    }
    if (user.hasRoles(Role.PERSONNEL_ADMIN)) {
      groups = groupDao.groupsByOffice(office, Optional.<Person>absent());
    }
    if (user.hasRoles(Role.GROUP_MANAGER)) {
      groups = groupDao.groupsByOffice(office, Optional.fromNullable(user.person));
    }
     
    render(groups, office);
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
    render(group, office);
  }

  /**
   * Metodo che permette l'apertura della pagina di creazione del gruppo.
   * @param officeId l'id della sede su cui creare il gruppo
   */
  public static void blank(long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    render("@edit", office);
  }
  
  //  public static void manageGroup() {
  //    User currentUser = Security.getUser().get();
  //    List<Group> managerGroups = Lists.newArrayList();
  //    if (currentUser.isSystemUser()) {
  //      managerGroups = groupDao.groupsByManager(Optional.<Person>absent());
  //    }
  //    if (!currentUser.hasRoles(Role.GROUP_MANAGER)) {
  //      flash.error("L'utente non dispone dei diritti per accedere alla funzionalità");
  //      Application.index();
  //    }
  //    //TODO: fare la regola drools per accedere alla funzionalità
  //    rules.checkIfPermitted();
  //    managerGroups = groupDao.groupsByManager(Optional.fromNullable(currentUser.person));
  //    render(managerGroups);
  //  }
}
