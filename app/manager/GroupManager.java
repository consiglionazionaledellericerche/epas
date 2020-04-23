package manager;


import com.google.common.base.Optional;
import dao.GroupDao;
import dao.PersonReperibilityDayDao;
import dao.RoleDao;
import dao.ShiftDao;
import dao.UsersRolesOfficesDao;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.assertj.core.util.Lists;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.dto.SeatSituationDto;
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
  public GroupManager(RoleDao roleDao, UsersRolesOfficesDao uroDao, 
      GroupDao groupDao) {
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
   * Genera il dto contenente le liste dei possibili modificatori dello stato delle info
   * della persona passata come parametro.
   * @param person la persona di cui conoscere tutti i possibili modificatori delle proprie info
   * @return il dto contenente tutte le informazioni degli utenti che possono in qualche modo
   *     modificare lo stato delle informazioni della persona passata come parametro.
   */
  public SeatSituationDto createOrganizationChart(Person person) {
    
    SeatSituationDto seatSituation = new SeatSituationDto();
    seatSituation.personnelAdmins = getPersonnelAdminInSeat(person.office);
    seatSituation.technicalAdmins = getTechnicalAdminInSeat(person.office);
    seatSituation.seatSupervisors = getSeatSupervisor(person.office);
    /*
     * Turni, reperibilità e gruppi di lavoro devo cercarli sulla base 
     * dell'appartenenza della persona al gruppo/servizio
     */
    if (!person.personShifts.isEmpty()) {
      seatSituation.shiftSupervisors = person.personShifts.stream()
          .flatMap(ps -> ps.personShiftShiftTypes.stream()
              .map(psst -> psst.shiftType.shiftCategories.supervisor.user))
          .collect(Collectors.toList());
          
    } else {
      seatSituation.shiftSupervisors = Lists.emptyList();
    }
    if (!person.reperibility.isEmpty()) {
      seatSituation.reperibilitySupervisors = person.reperibility.stream()
          .map(pr -> pr.personReperibilityType.supervisor.user).collect(Collectors.toList());
    } else {
      seatSituation.reperibilitySupervisors = Lists.emptyList();
    }
    if (!person.groups.isEmpty()) {
      seatSituation.groupManagers = person.groups.stream()
          .map(g -> g.manager.user).collect(Collectors.toList());
    } else {
      seatSituation.groupManagers = Lists.emptyList();
    }
        
    /*
     * Fine
     */
    seatSituation.mealTicketsManager = getMealTicketsManager(person.office);
    seatSituation.registryManagers = getRegistryManager(person.office);
        
    return seatSituation;
  }
  
  private List<User> getTechnicalAdminInSeat(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.TECHNICAL_ADMIN), office);
  }
  
  private List<User> getPersonnelAdminInSeat(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.PERSONNEL_ADMIN), office);
  }
  
  private List<User> getSeatSupervisor(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.SEAT_SUPERVISOR), office);
  }
  
  private List<User> getMealTicketsManager(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.MEAL_TICKET_MANAGER), office);
  }
  
  private List<User> getRegistryManager(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.REGISTRY_MANAGER), office);
  }

}
