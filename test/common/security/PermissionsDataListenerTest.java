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

import common.injection.StaticInject;
import db.h2support.H2Examples;
import javax.inject.Inject;
import javax.persistence.EntityTransaction;
import models.Person;
import models.PersonShift;
import models.ShiftCategories;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Il salvataggio dei dati letti dalle regole dei permessi invalida gli esiti condivisi dopo il
 * commit, non prima.
 */
@StaticInject
public class PermissionsDataListenerTest extends UnitTest {

  @Inject
  private static H2Examples h2Examples;

  /**
   * Il runner dei test chiude la transazione corrente: se un test l'ha chiusa, ne riapre una.
   */
  @After
  public void restoreTransaction() {
    EntityTransaction tx = JPA.em().getTransaction();
    if (!tx.isActive()) {
      tx.begin();
    }
  }

  @Test
  public void personChangeInvalidatesAfterCommit() {
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    commit();

    long before = SharedChecksCache.generation();
    person.setName("Cambiato");
    person.save();
    JPA.em().flush();
    assertEquals("prima del commit gli altri leggono ancora i dati precedenti",
        before, SharedChecksCache.generation());

    commit();
    assertTrue(SharedChecksCache.generation() > before);
  }

  @Test
  public void shiftCategoryManagersChangeInvalidates() {
    Person supervisor = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    Person manager = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    ShiftCategories category = new ShiftCategories();
    category.setDescription("Turno di prova " + System.nanoTime());
    category.setSupervisor(supervisor);
    category.setOffice(supervisor.getOffice());
    category.save();
    commit();

    long before = SharedChecksCache.generation();
    // solo la relazione molti-a-molti cambia
    category.getManagers().add(manager);
    category.save();
    commit();

    assertTrue("un nuovo responsabile del turno vale subito",
        SharedChecksCache.generation() > before);
  }

  @Test
  public void newPersonShiftInvalidates() {
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    commit();

    long before = SharedChecksCache.generation();
    // l'associazione appartiene a PersonShift: la persona non cambia
    PersonShift personShift = new PersonShift();
    personShift.setPerson(person);
    personShift.setBeginDate(new LocalDate(2026, 1, 1));
    personShift.save();
    commit();
    assertTrue("un nuovo turnista vede subito il calendario dei turni",
        SharedChecksCache.generation() > before);

    before = SharedChecksCache.generation();
    personShift.delete();
    commit();
    assertTrue("chi non è più turnista non vede più il calendario dei turni",
        SharedChecksCache.generation() > before);
  }

  @Test
  public void changeWithoutTransactionInvalidatesImmediately() {
    JPA.em().getTransaction().commit();
    long before = SharedChecksCache.generation();
    PermissionsDataListener.invalidateAfterTransaction();
    assertTrue(SharedChecksCache.generation() > before);
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }
}
