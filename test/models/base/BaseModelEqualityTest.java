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

package models.base;

import com.google.common.collect.Sets;
import models.Qualification;
import models.Role;
import org.hibernate.Hibernate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Verifica equals e hashCode di BaseModel: per id e classe, anche con i proxy Hibernate.
 */
public class BaseModelEqualityTest extends UnitTest {

  private static Role role(Long id, String name) {
    Role role = new Role();
    role.id = id;
    role.setName(name);
    return role;
  }

  @Test
  public void sameIdAndClassAreEqual() {
    Role first = role(10L, Role.EMPLOYEE);
    Role second = role(10L, Role.PERSONNEL_ADMIN);
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertEquals(Long.valueOf(10L).hashCode(), first.hashCode());
    assertEquals(1, Sets.newHashSet(first, second).size());
  }

  @Test
  public void differentIdOrClassAreNotEqual() {
    assertNotEquals(role(10L, Role.EMPLOYEE), role(11L, Role.EMPLOYEE));
    Qualification qualification = new Qualification();
    qualification.id = 10L;
    assertNotEquals(role(10L, Role.EMPLOYEE), qualification);
    assertNotEquals(qualification, role(10L, Role.EMPLOYEE));
    assertNotEquals(role(10L, Role.EMPLOYEE), null);
    assertNotEquals(role(10L, Role.EMPLOYEE), "10");
  }

  @Test
  public void entityWithoutIdIsEqualOnlyToItself() {
    Role first = role(null, Role.EMPLOYEE);
    Role second = role(null, Role.EMPLOYEE);
    assertEquals(first, first);
    assertNotEquals(first, second);
    assertEquals(0, first.hashCode());
  }

  @Test
  public void proxyEqualsEntityWithoutInitialization() {
    Role saved = role(null, "ruoloProxy");
    saved.save();
    Long id = saved.id;
    JPA.em().flush();
    JPA.em().clear();

    Role proxy = JPA.em().getReference(Role.class, id);
    assertFalse(Hibernate.isInitialized(proxy));
    Role detached = role(id, "ruoloProxy");
    // confrontare un'entità con un proxy non lo carica
    assertTrue(detached.equals(proxy));
    assertFalse("il confronto non deve caricare l'entità", Hibernate.isInitialized(proxy));
    // un metodo chiamato sul proxy lo carica (come con JPABase), con lo stesso risultato
    assertTrue(proxy.equals(detached));
    assertEquals(detached.hashCode(), proxy.hashCode());
  }
}
