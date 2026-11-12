/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.filter.AffiliationFilter;
import db.h2support.H2Examples;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.flows.Affiliation;
import models.flows.Group;
import models.query.QPerson;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Test sui gruppi: gruppo di riferimento in base alla percentuale, chiusura con la sede e
 * passaggio dei gruppi quando il responsabile viene trasferito.
 */
@StaticInject
public class GroupManagerTest extends UnitTest {

  private static final org.joda.time.LocalDate BEGIN = new org.joda.time.LocalDate(2026, 1, 1);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static GroupManager groupManager;
  @Inject
  private static PersonOfficeServicesManager servicesManager;

  @Test
  public void personsInChargeQueryUsesThePrevailingPercentage() {
    final Person member = employee();
    final Person tied = employee();
    final Person main = employee();
    final Person minor = employee();
    final Office office = member.getOffice();
    final Group mainGroup = group(office, main, "principale");
    final Group minorGroup = group(office, minor, "secondario");
    affiliation(mainGroup, member, BigDecimal.valueOf(60));
    affiliation(minorGroup, member, BigDecimal.valueOf(40));
    affiliation(mainGroup, tied, BigDecimal.valueOf(50));
    affiliation(minorGroup, tied, BigDecimal.valueOf(50));
    JPA.em().flush();

    assertEquals(ImmutableSet.of(member.id, tied.id), inChargeOf(main));
    assertEquals(ImmutableSet.of(tied.id), inChargeOf(minor));

    // chiuso il gruppo principale, resta di riferimento quello secondario
    mainGroup.setEndDate(LocalDate.now());
    mainGroup.save();
    JPA.em().flush();
    assertEquals(ImmutableSet.of(member.id, tied.id), inChargeOf(minor));
  }

  @Test
  public void closingTheOfficeClosesItsGroups() {
    final Person member = employee();
    final Person manager = employee();
    final Office office = member.getOffice();
    final Group group = group(office, manager, "da chiudere");
    final Affiliation affiliation = affiliation(group, member, null);
    final UsersRolesOffices managerRole = role(manager, office, Role.GROUP_MANAGER);
    JPA.em().flush();

    final LocalDate closing = LocalDate.of(2026, 6, 1);
    final List<String> messages = groupManager.closeOfficeGroups(office, closing);
    JPA.em().flush();

    assertEquals(closing, group.getEndDate());
    assertEquals(closing.minusDays(1), affiliation.getEndDate());
    assertNull(UsersRolesOffices.findById(managerRole.id));
    assertFalse(messages.isEmpty());
    // una seconda esecuzione non cambia nulla
    assertTrue(groupManager.closeOfficeGroups(office, closing).isEmpty());
  }

  @Test
  public void transferredGroupManagerHandsTheGroupsToTheSeatSupervisor() {
    final Person manager = employee();
    final Person supervisor = employee();
    final Person member = employee();
    final Office source = manager.getOffice();
    final Group group = group(source, manager, "del trasferito");
    affiliation(group, member, null);
    final UsersRolesOffices managerRole = role(manager, source, Role.GROUP_MANAGER);
    role(supervisor, source, Role.SEAT_SUPERVISOR);
    // nella sessione di test il lato inverso utente -> persona non è valorizzato
    supervisor.getUser().setPerson(supervisor);
    JPA.em().flush();

    // è ciò che il trasferimento esegue sulla sede lasciata quando diventa effettivo
    final List<String> messages = servicesManager.removeRolesInOffice(manager, source);
    JPA.em().flush();

    assertEquals(supervisor, group.getManager());
    assertNull(group.getEndDate());
    assertNull(UsersRolesOffices.findById(managerRole.id));
    assertEquals(1, UsersRolesOffices.count("user = ?1 and office = ?2 and role.name = ?3",
        supervisor.getUser(), source, Role.GROUP_MANAGER));
    assertTrue(messages.stream().anyMatch(m -> m.contains("passato temporaneamente")));
  }

  @Test
  public void groupEditAssignsOnlyTheFreePercentage() {
    final Person full = employee();
    final Person partial = employee();
    final Person manager = employee();
    final Office office = full.getOffice();
    final Group other = group(office, manager, "altro");
    affiliation(other, full, null);
    affiliation(other, partial, BigDecimal.valueOf(60));
    final Group group = group(office, manager, "modificato");
    JPA.em().flush();

    final List<String> messages =
        groupManager.updatePeople(group, ImmutableSet.of(full, partial));
    JPA.em().flush();

    assertEquals(1, messages.size());
    assertTrue(messages.get(0).contains(full.getFullname()));
    final Affiliation added = Affiliation.find("person = ?1 and group = ?2", partial, group)
        .first();
    assertEquals(0, BigDecimal.valueOf(40).compareTo(added.getPercentage()));
  }

  private java.util.Set<Long> inChargeOf(Person manager) {
    final QPerson person = QPerson.person;
    return ImmutableSet.copyOf(new com.querydsl.jpa.impl.JPAQueryFactory(JPA.em())
        .select(person.id).from(person)
        .where(person.id.in(AffiliationFilter.personIdsInChargeOf(manager, LocalDate.now())))
        .fetch());
  }

  private Person employee() {
    return h2Examples.normalEmployee(BEGIN, Optional.absent());
  }

  private Group group(Office office, Person manager, String name) {
    final Group group = new Group();
    group.setName("Gruppo " + name + " " + manager.id);
    group.setOffice(office);
    group.setManager(manager);
    group.save();
    manager.getGroupsPeople().add(group);
    return group;
  }

  private Affiliation affiliation(Group group, Person person, BigDecimal percentage) {
    final Affiliation affiliation = new Affiliation();
    affiliation.setGroup(group);
    affiliation.setPerson(person);
    affiliation.setBeginDate(LocalDate.of(2026, 1, 1));
    affiliation.setPercentage(percentage);
    affiliation.save();
    person.getAffiliations().add(affiliation);
    group.getAffiliations().add(affiliation);
    return affiliation;
  }

  private UsersRolesOffices role(Person person, Office office, String roleName) {
    Role role = Role.find("byName", roleName).first();
    if (role == null) {
      role = new Role();
      role.setName(roleName);
      role.save();
    }
    final UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(person.getUser());
    uro.setOffice(office);
    uro.setRole(role);
    uro.save();
    person.getUser().getUsersRolesOffices().add(uro);
    return uro;
  }
}
