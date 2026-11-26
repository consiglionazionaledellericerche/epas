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

package common.security;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.AccountRole;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Verifica la cache degli esiti dei controlli senza target condivisa tra le richieste.
 */
public class SharedChecksCacheTest extends UnitTest {

  private static final Object RULES = new Object();
  private static final String ACTION = "Persons.list";

  private long ids = 100;

  private User user() {
    User user = new User();
    user.id = ids++;
    user.setUsername("utente" + user.id);
    return user;
  }

  private Office office() {
    Office office = new Office();
    office.id = ids++;
    return office;
  }

  private Role role(String name) {
    Role role = new Role();
    role.id = ids++;
    role.setName(name);
    return role;
  }

  private void addRole(User user, Office office, Role role) {
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.id = ids++;
    uro.setUser(user);
    uro.setOffice(office);
    uro.setRole(role);
    user.getUsersRolesOffices().add(uro);
  }

  @Test
  public void resultIsSharedForSameUserAndAction() {
    SharedChecksCache cache = new SharedChecksCache(60);
    User user = user();
    addRole(user, office(), role(Role.PERSONNEL_ADMIN));
    assertNull(cache.get(RULES, user, ACTION));
    cache.put(RULES, user, ACTION, true);
    assertEquals(Boolean.TRUE, cache.get(RULES, user, ACTION));
    assertNull(cache.get(RULES, user, "Stampings.dailyPresence"));
    assertNull("un altro utente non usa l'esito", cache.get(RULES, user(), ACTION));
    assertNull("regole ricaricate", cache.get(new Object(), user, ACTION));
  }

  @Test
  public void roleChangesChangeTheKey() {
    User user = user();
    Office office = office();
    Role employee = role(Role.EMPLOYEE);
    addRole(user, office, employee);
    String before = SharedChecksCache.key(RULES, user, ACTION);

    addRole(user, office, role(Role.PERSONNEL_ADMIN));
    String withOfficeRole = SharedChecksCache.key(RULES, user, ACTION);
    assertNotEquals(before, withOfficeRole);

    user.getUsersRolesOffices().get(1).setOffice(office());
    String otherOffice = SharedChecksCache.key(RULES, user, ACTION);
    assertNotEquals(withOfficeRole, otherOffice);

    user.getRoles().add(AccountRole.DEVELOPER);
    assertNotEquals(otherOffice, SharedChecksCache.key(RULES, user, ACTION));
  }

  @Test
  public void keyDoesNotDependOnRolesOrder() {
    Office office = office();
    Role employee = role(Role.EMPLOYEE);
    Role admin = role(Role.PERSONNEL_ADMIN);
    User user = user();
    addRole(user, office, employee);
    addRole(user, office, admin);
    String key = SharedChecksCache.key(RULES, user, ACTION);
    java.util.Collections.reverse(user.getUsersRolesOffices());
    assertEquals(key, SharedChecksCache.key(RULES, user, ACTION));
  }

  @Test
  public void zeroSecondsDisablesTheCache() {
    SharedChecksCache cache = new SharedChecksCache(0);
    User user = user();
    cache.put(RULES, user, ACTION, true);
    assertFalse(cache.isEnabled());
    assertNull(cache.get(RULES, user, ACTION));
  }
}
