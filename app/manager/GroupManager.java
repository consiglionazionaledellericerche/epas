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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.GroupDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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


/**
 * Manager per la gestione dei gruppi di persone.
 *
 * @author Dario Tagliaferri
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class GroupManager {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        uroDao.getUsersRolesOffices(group.getManager().getUser(), role, group.getOffice());
    if (uro.isPresent()) {
      uro.get().delete();
      log.debug("Eliminato ruolo {} per l'utente {}", 
          uro.get().getRole().getName(), uro.get().getUser().getPerson().fullName());
      return true;
    }
    return false;
  }

  /**
   * Inserisce e termina le affiliazioni ad un gruppo con data corrente in funzione della lista
   * delle persone passate. Alle nuove affiliazioni viene assegnata la percentuale ancora libera
   * della persona (100% meno le afferenze già attive): chi ha già il 100% non viene inserito.
   *
   * @return le segnalazioni sulle persone non inserite.
   */
  public List<String> updatePeople(Group group, Set<Person> people) {
    log.info("current people = {}, new people = {}", group.getPeople(), people);
    final List<String> messages = Lists.newArrayList();
    if (people == null) {
      people = Sets.newHashSet();
    }
    final LocalDate today = LocalDate.now();
    val toDisable = Sets.difference(Sets.newHashSet(group.getPeople()), people);
    log.info("Person toDisable = {}", toDisable);
    val currentAffiliationsToDisable = 
        group.getAffiliations().stream()
        .filter(a -> a.contains(today) && toDisable.contains(a.getPerson()))
        .collect(Collectors.toSet());
    currentAffiliationsToDisable.forEach(a -> endAffiliation(a, today));
    val toInsert = Sets.difference(people, Sets.newHashSet(group.getPeople()));
    toInsert.forEach(person -> {
      final BigDecimal free = freePercentage(person, today);
      if (free.signum() <= 0) {
        messages.add(String.format("%s non è stato inserito nel gruppo %s: le sue afferenze "
            + "ai gruppi raggiungono già il 100%%", person.getFullname(), group.getName()));
        return;
      }
      val affiliation = new Affiliation();
      affiliation.setPerson(person);
      affiliation.setGroup(group);
      affiliation.setBeginDate(today);
      affiliation.setPercentage(free);
      affiliation.save();
      log.info("Inserita nuova associazione tra {} al gruppo {} al {}%", 
          person.getFullname(), group.getName(), free);
    });
    return messages;
  }

  /**
   * La percentuale di afferenza ancora libera della persona alla data.
   */
  private BigDecimal freePercentage(Person person, LocalDate date) {
    return person.getAffiliations().stream()
        .filter(a -> a.isActiveAt(date))
        .map(Affiliation::getEffectivePercentage)
        .reduce(Affiliation.FULL_PERCENTAGE, BigDecimal::subtract);
  }

  /**
   * Termina l'affiliazione il giorno precedente la data indicata; se l'affiliazione inizia
   * dalla data indicata in poi viene eliminata. Non fa nulla se è già terminata prima.
   *
   * @return la descrizione dell'operazione effettuata, vuoto se non è stato fatto nulla.
   */
  public java.util.Optional<String> endAffiliation(Affiliation affiliation, LocalDate date) {
    final String group = affiliation.getGroup().getName();
    final String person = affiliation.getPerson().getFullname();
    if (!affiliation.getBeginDate().isBefore(date)) {
      affiliation.getPerson().getAffiliations().remove(affiliation);
      affiliation.getGroup().getAffiliations().remove(affiliation);
      affiliation.delete();
      log.info("Eliminata l'appartenenza di {} al gruppo {}", person, group);
      return java.util.Optional.of(String.format(
          "Eliminata l'appartenenza di %s al gruppo %s, che iniziava dal %s",
          person, group, date.format(DATE_FORMAT)));
    }
    if (affiliation.getEndDate() == null || !affiliation.getEndDate().isBefore(date)) {
      final LocalDate lastDay = date.minusDays(1);
      affiliation.setEndDate(lastDay);
      affiliation.save();
      log.info("Terminata al {} l'appartenenza di {} al gruppo {}", lastDay, person, group);
      return java.util.Optional.of(String.format(
          "Terminata al %s l'appartenenza di %s al gruppo %s",
          lastDay.format(DATE_FORMAT), person, group));
    }
    return java.util.Optional.empty();
  }

  /**
   * Chiude il gruppo alla data indicata (il gruppo non è più attivo da quella data), termina le
   * affiliazioni dei suoi membri il giorno precedente e toglie al responsabile il ruolo di
   * responsabile di gruppo se non gestisce altri gruppi attivi sulla sede.
   *
   * @return le descrizioni delle operazioni effettuate.
   */
  public List<String> closeGroup(Group group, LocalDate date) {
    final List<String> messages = Lists.newArrayList();
    if (group.getEndDate() == null || group.getEndDate().isAfter(date)) {
      group.setEndDate(date);
      group.save();
      messages.add(String.format("Chiuso il gruppo %s dal %s",
          group.getName(), date.format(DATE_FORMAT)));
    }
    for (Affiliation affiliation : Lists.newArrayList(group.getAffiliations())) {
      endAffiliation(affiliation, date).ifPresent(messages::add);
    }
    removeManagerRoleIfUnused(group.getManager(), group.getOffice()).ifPresent(messages::add);
    messages.forEach(m -> log.info("{}", m));
    return messages;
  }

  /**
   * Chiude, alla data di chiusura della sede, i gruppi della sede ancora aperti a quella data.
   *
   * @return le descrizioni delle operazioni effettuate.
   */
  public List<String> closeOfficeGroups(Office office, LocalDate date) {
    return groupDao.groupsByOffice(office, Optional.absent(), Optional.of(true)).stream()
        .filter(g -> g.getEndDate() == null || g.getEndDate().isAfter(date))
        .flatMap(g -> closeGroup(g, date).stream())
        .collect(Collectors.toList());
  }

  /**
   * Passa al responsabile della sede i gruppi attivi della sede gestiti dalla persona, che
   * lascia la sede. Il responsabile di gruppo è obbligatorio: il responsabile di sede subentra
   * temporaneamente, in attesa che la sede ne nomini uno nuovo. Se la sede non ha un
   * responsabile il gruppo resta invariato e viene solo segnalato.
   *
   * @param manager la persona trasferita
   * @param office la sede lasciata
   * @return le descrizioni delle operazioni effettuate e gli avvisi.
   */
  public List<String> handOverGroups(Person manager, Office office) {
    final List<String> messages = Lists.newArrayList();
    final List<Group> groups = 
        groupDao.groupsByOffice(office, Optional.of(manager), Optional.absent());
    if (groups.isEmpty()) {
      return messages;
    }
    final java.util.Optional<Person> supervisor = uroDao
        .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.SEAT_SUPERVISOR), office).stream()
        .map(User::getPerson)
        .filter(p -> p != null && !p.equals(manager))
        .findFirst();
    for (Group group : groups) {
      if (!supervisor.isPresent()) {
        messages.add(String.format("È ancora responsabile del gruppo %s della sede %s, che non "
            + "ha un responsabile di sede: assegnare un nuovo responsabile",
            group.getName(), office.getName()));
        continue;
      }
      group.setManager(supervisor.get());
      group.save();
      createManager(office, group, new UsersRolesOffices());
      messages.add(String.format("Il gruppo %s della sede %s è passato temporaneamente al "
          + "responsabile di sede %s: assegnare un nuovo responsabile", group.getName(),
          office.getName(), supervisor.get().getFullname()));
    }
    removeManagerRoleIfUnused(manager, office).ifPresent(messages::add);
    messages.forEach(m -> log.info("{}", m));
    return messages;
  }

  /**
   * Toglie alla persona il ruolo di responsabile di gruppo sulla sede se non gestisce più
   * gruppi attivi della sede.
   */
  private java.util.Optional<String> removeManagerRoleIfUnused(Person manager, Office office) {
    if (manager == null || manager.getUser() == null
        || !groupDao.groupsByOffice(office, Optional.of(manager), Optional.absent()).isEmpty()) {
      return java.util.Optional.empty();
    }
    final Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    final Optional<UsersRolesOffices> uro = 
        uroDao.getUsersRolesOffices(manager.getUser(), role, office);
    if (!uro.isPresent()) {
      return java.util.Optional.empty();
    }
    manager.getUser().getUsersRolesOffices().remove(uro.get());
    uro.get().delete();
    return java.util.Optional.of(String.format(
        "Rimosso a %s il ruolo di responsabile di gruppo sulla sede %s",
        manager.getFullname(), office.getName()));
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
      if (!groupDao.approvalGroups(person).isEmpty()) {
        map.put(role, groupDao.approvalGroups(person).stream()
            .map(g -> g.getManager().getUser()).collect(Collectors.toList()));
      } else {
        map.put(role, Lists.newArrayList());
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

