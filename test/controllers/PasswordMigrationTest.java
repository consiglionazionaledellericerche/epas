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

package controllers;

import com.google.common.collect.ImmutableMap;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.UUID;
import javax.inject.Inject;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Passaggio dagli hash MD5/SHA-512 non salati all'hash con salt: le password esistenti
 * continuano a funzionare e vengono aggiornate al primo login riuscito.
 */
@StaticInject
public class PasswordMigrationTest extends FunctionalTest {

  private static final String PASSWORD = "unaPasswordDiTest";

  @Inject
  private static H2Examples h2Examples;

  @Test
  public void legacyPasswordIsUpgradedAtLogin() {
    User user = new User();
    user.setUsername("legacy." + UUID.randomUUID());
    user.setPassword(User.cryptPasswordMd5(PASSWORD));
    user.setPasswordSha512(User.cryptPasswordSha512(PASSWORD));
    user.save();

    assertFalse(Security.authenticate(user.getUsername(), "sbagliata"));
    assertTrue(Security.authenticate(user.getUsername(), PASSWORD));

    User reloaded = User.findById(user.id);
    assertNotNull(reloaded.getPasswordHash());
    assertNull(reloaded.getPassword());
    assertNull(reloaded.getPasswordSha512());
    assertFalse(reloaded.passwordNeedsRehash());
    assertTrue(Security.authenticate(user.getUsername(), PASSWORD));
    assertFalse(Security.authenticate(user.getUsername(), "sbagliata"));
  }

  @Test
  public void legacySha512OnlyPasswordIsAccepted() {
    User user = new User();
    user.setUsername("legacy.sha." + UUID.randomUUID());
    user.setPasswordSha512(User.cryptPasswordSha512(PASSWORD));
    user.save();

    assertTrue(Security.authenticate(user.getUsername(), PASSWORD));
    assertNotNull(((User) User.findById(user.id)).getPasswordHash());
  }

  @Test
  public void newPasswordsAreNotStoredUnsalted() {
    User user = new User();
    user.setUsername("new." + UUID.randomUUID());
    user.updatePassword(PASSWORD);
    user.save();

    assertNull(user.getPassword());
    assertNull(user.getPasswordSha512());
    assertTrue(Security.authenticate(user.getUsername(), PASSWORD));
  }

  /**
   * Il cambio password dalla pagina dell'utente verifica la vecchia password sul nuovo hash.
   */
  @Test
  public void changePasswordChecksTheOldPassword() {
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    Role role = Role.find("byName", Role.EMPLOYEE).first();
    if (role == null) {
      role = new Role();
      role.setName(Role.EMPLOYEE);
      role.save();
    }
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(person.getUser());
    uro.setOffice(person.getOffice());
    uro.setRole(role);
    uro.save();
    String username = person.getUser().getUsername();
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();

    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
    String url = Router.reverse("Persons.savePassword").url;
    POST(url, ImmutableMap.of("vecchiaPassword", "sbagliata",
        "nuovaPassword", "nuovaPassword1", "confermaPassword", "nuovaPassword1"));
    assertTrue(Security.authenticate(username, H2Examples.DEFAULT_PASSWORD));

    POST(url, ImmutableMap.of("vecchiaPassword", H2Examples.DEFAULT_PASSWORD,
        "nuovaPassword", "nuovaPassword1", "confermaPassword", "nuovaPassword1"));
    JPA.em().clear();
    assertTrue(Security.authenticate(username, "nuovaPassword1"));
    assertFalse(Security.authenticate(username, H2Examples.DEFAULT_PASSWORD));
  }
}
