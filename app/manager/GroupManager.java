package manager;


import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import dao.GroupDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.flows.Affiliation;
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
  
  /**
   * Inserisce ed elimina le affiliazioni ad un gruppo con data 
   * corrente in funzione della lista delle persone passate.
   */
  public void updatePeople(Group group, Set<Person> people) {
    log.info("current people = {}, new people = {}", group.getPeople(), people); 
    val toDisable = Sets.difference(Sets.newHashSet(group.getPeople()), people);
    log.info("toDisable = {}", toDisable);
    val currentAffiliationsToDisable = 
        group.getAffiliations().stream()
          .filter(a -> a.isActive() && toDisable.contains(a.getPerson()))
          .collect(Collectors.toSet());
    currentAffiliationsToDisable.stream().forEach(a ->  {      
      a.setEndDate(LocalDate.now());
      a.save();
      log.info("Disabilita associazione di {} al gruppo {}", 
          a.getPerson().getFullname(), a.getGroup().getName());
    });
    val toInsert = Sets.difference(people, Sets.newHashSet(group.getPeople()));
    toInsert.forEach(person -> {
      val affiliation = new Affiliation();
      affiliation.setPerson(person);
      affiliation.setGroup(group);
      affiliation.setBeginDate(LocalDate.now());
      affiliation.save();
      log.info("Inserita nuova associazione tra {} al gruppo {}", 
          person.getFullname(), group.getName());
    });
  }
}
