package controllers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
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
    log.debug("Creato nuovo ruolo {} per il responsabile di gruppo {}", 
        uro.role.name, group.manager.fullName());
    flash.success("Nuovo gruppo  di lavoro %s salvato correttamente.",
        group.name);
    showGroups(office.id);
  }

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

  public static void showGroups(Long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    List<Group> groups = groupDao.groupsByOffice(office);
    render(groups, office);
  }

  public static void edit(long groupId) {
    Group group = Group.findById(groupId);
    notFoundIfNull(group);
    rules.checkIfPermitted(group.manager.office);
    Office office = group.manager.office;
    render(group, office);
  }

  public static void blank(long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    render("@edit", office);
  }
}
