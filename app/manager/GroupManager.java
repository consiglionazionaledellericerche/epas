package manager;


import com.google.common.base.Optional;
import dao.GroupDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Role;
import models.UsersRolesOffices;
import models.flows.Group;

@Slf4j
public class GroupManager {

  private final RoleDao roleDao;
  private final UsersRolesOfficesDao uroDao;
  private final GroupDao groupDao;

  /**
   * Injection.
   * @param roleDao il dao sui ruoli
   * @param uroDao il dao sugli usersRolesOffices
   * @param groupDao il dao sui gruppi
   */
  @Inject
  public GroupManager(RoleDao roleDao, UsersRolesOfficesDao uroDao, GroupDao groupDao) {
    this.roleDao = roleDao;
    this.uroDao = uroDao;
    this.groupDao = groupDao;
  }

  /**
   * Metodo di utilità per creare il ruolo manager da associare al responsabile del 
   *     gruppo di lavoro. 
   * @param office la sede cui appartiene il ruolo
   * @param group il gruppo di cui fa parte il manager
   * @param uro l'user role office da creare
   */
  public void createManager(Office office, Group group, UsersRolesOffices uro) {
    Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    Optional<UsersRolesOffices> uroPresent = 
        uroDao.getUsersRolesOffices(group.manager.user, role, group.office);
    if (uroPresent.isPresent()) {
      return;
    }
    uro.office = office;
    uro.role = role;
    uro.user = group.manager.user;
    uro.save();   
    log.debug("Creato ruolo {} per l'utente {}", role.name, uro.user.person.fullName());
  }

  /**
   * Metodo che elimina il ruolo manager al responsabile di gruppo durante la fase di 
   *     eliminazione del gruppo.
   * @param group il gruppo di cui si vuole rimuvoere il ruolo di manager
   * @return true se è stato eliminato il manager del gruppo, false altrimenti.
   */
  public boolean deleteManager(Group group) {
    Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    List<Group> managerGroups = groupDao.groupsByManager(Optional.fromNullable(group.manager));
    if (managerGroups.size() > 1) {
      log.debug("Non elimino il ruolo perchè {} ha almeno un altro gruppo su cui è responsabile", 
          group.manager.fullName());
      return true;
    }
    Optional<UsersRolesOffices> uro = 
        uroDao.getUsersRolesOffices(group.manager.user, role, group.manager.office);
    if (uro.isPresent()) {
      uro.get().delete();
      log.debug("Eliminato ruolo {} per l'utente {}", 
          uro.get().role.name, uro.get().user.person.fullName());
      return true;
    }
    return false;
  }
}
