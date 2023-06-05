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

package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.GroupDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.flows.Affiliation;
import models.flows.Group;
import org.assertj.core.util.Lists;


/**
 * Manager per la gestione dei gruppi di persone.
 *
 * @author Dario Tagliaferri
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class GroupManager {

  private final RoleDao roleDao;
  private final UsersRolesOfficesDao uroDao;
  private final GroupDao groupDao;

  /**
   * Injection.
   *
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
   * gruppo di lavoro.
   *
   * @param office la sede cui appartiene il ruolo
   * @param group il gruppo di cui fa parte il manager
   * @param uro l'user role office da creare
   */
  public void createManager(Office office, Group group, UsersRolesOffices uro) {
    Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    Optional<UsersRolesOffices> uroPresent = 
        uroDao.getUsersRolesOffices(group.getManager().getUser(), role, group.getOffice());
    if (uroPresent.isPresent()) {
      return;
    }
    uro.setOffice(office);
    uro.setRole(role);
    uro.setUser(group.getManager().getUser());
    uro.save();   
    log.debug("Creato ruolo {} per l'utente {}", 
        role.getName(), uro.getUser().getPerson().fullName());
  }

  /**
   * Metodo che elimina il ruolo manager al responsabile di gruppo durante la fase di 
   * eliminazione del gruppo.
   *
   * @param group il gruppo di cui si vuole rimuvoere il ruolo di manager
   * @return true se è stato eliminato il manager del gruppo, false altrimenti.
   */
  public boolean deleteManager(Group group) {
    Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    List<Group> managerGroups = groupDao.groupsByManager(Optional.fromNullable(group.getManager()));
    if (managerGroups.size() > 1) {
      log.debug("Non elimino il ruolo perchè {} ha almeno un altro gruppo su cui è responsabile", 
          group.getManager().fullName());
      return true;
    }
    Optional<UsersRolesOffices> uro = 
        uroDao.getUsersRolesOffices(
            group.getManager().getUser(), role, group.getManager().getOffice());
    if (uro.isPresent()) {
      uro.get().delete();
      log.debug("Eliminato ruolo {} per l'utente {}", 
          uro.get().getRole().getName(), uro.get().getUser().getPerson().fullName());
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
    if (people == null) {
      people = Sets.newHashSet();
    }
    val toDisable = Sets.difference(Sets.newHashSet(group.getPeople()), people);
    log.info("Person toDisable = {}", toDisable);
    val currentAffiliationsToDisable = 
        group.getAffiliations().stream()
        .filter(a -> !a.isActive() && toDisable.contains(a.getPerson()))
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

  /**
   * Genera il dto contenente le liste dei possibili modificatori dello stato delle info
   * della persona passata come parametro.
   *
   * @param person la persona di cui conoscere tutti i possibili modificatori delle proprie info
   * @return il dto contenente tutte le informazioni degli utenti che possono in qualche modo
   *     modificare lo stato delle informazioni della persona passata come parametro.
   */
  public Map<Role, List<User>> createOrganizationChart(Person person, Role role) {

    Map<Role, List<User>> map = Maps.newHashMap();

    if (role.getName().equals(Role.GROUP_MANAGER)) {
      if (!groupDao.myGroups(person).isEmpty()) {
        map.put(role, groupDao.myGroups(person).stream()
            .map(g -> g.getManager().getUser()).collect(Collectors.toList()));
      } else {
        map.put(role, Lists.emptyList());
      }
    }
    if (role.getName().equals(Role.MEAL_TICKET_MANAGER)) {
      map.put(role, getMealTicketsManager(person.getOffice()));
    }

    if (role.getName().equals(Role.PERSONNEL_ADMIN)) {
      map.put(role, getPersonnelAdminInSeat(person.getOffice()));
    }
    if (role.getName().equals(Role.PERSONNEL_ADMIN_MINI)) {
      map.put(role, getPersonnelAdminMiniInSeat(person.getOffice()));
    }
    if (role.getName().equals(Role.REGISTRY_MANAGER)) {
      map.put(role, getRegistryManager(person.getOffice()));
    }
    if (role.getName().equals(Role.REPERIBILITY_MANAGER)) {
      if (!person.getReperibility().isEmpty()) {
        map.put(role, person.getReperibility().stream()
            .map(pr -> pr.getPersonReperibilityType().getSupervisor().getUser())
            .collect(Collectors.toList()));
      } 
    }

    if (role.getName().equals(Role.SEAT_SUPERVISOR)) {
      map.put(role, getSeatSupervisor(person.getOffice()));
    }
    if (role.getName().equals(Role.SHIFT_MANAGER)) {
      if (!person.getPersonShifts().isEmpty()) {
        map.put(role, person.getPersonShifts().stream()
            .flatMap(ps -> ps.getPersonShiftShiftTypes().stream()
                .map(psst -> psst.getShiftType().getShiftCategories().getSupervisor().getUser()))
            .collect(Collectors.toList()));              
      } 
    }
    if (role.getName().equals(Role.TECHNICAL_ADMIN)) {
      map.put(role, getTechnicalAdminInSeat(person.getOffice()));
    }


    return map;
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

  private List<User> getPersonnelAdminMiniInSeat(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao
        .getRoleByName(Role.PERSONNEL_ADMIN_MINI), office);
  }


}

