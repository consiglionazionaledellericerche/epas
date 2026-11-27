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

package helpers.jpa;

import models.Office;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType;
import org.hibernate.Session;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Le impostazioni della issue #707 devono arrivare davvero a Hibernate: in Play 1.5.3 le
 * proprietà {@code hibernate.*} di application.conf non vengono passate, valgono solo quelle di
 * conf/hibernate.properties. Il caricamento a blocchi è attivo solo sulle associazioni annotate
 * con {@code @BatchSize}, non su tutte.
 */
public class QueryStatsListenerTest extends UnitTest {

  private static SessionFactoryImplementor factory() {
    return JPA.em().getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);
  }

  private static SessionFactoryOptions options() {
    return factory().getSessionFactoryOptions();
  }

  @Test
  public void batchFetchIsNotGlobal() {
    assertTrue("il caricamento a blocchi globale rallenta le pagine con molte sedi in sessione",
        options().getDefaultBatchFetchSize() <= 1);
    assertTrue(factory().getMetamodel()
        .collectionPersister(Office.class.getName() + ".configurations").getBatchSize() <= 1);
  }

  @Test
  public void batchFetchIsActiveOnAnnotatedAssociations() {
    assertEquals(16, factory().getMetamodel()
        .collectionPersister(Absence.class.getName() + ".troubles").getBatchSize());
    assertEquals(16, factory().getMetamodel()
        .collectionPersister(AbsenceType.class.getName() + ".takableGroup").getBatchSize());
    assertTrue(factory().getMetamodel().entityPersister(JustifiedType.class).isBatchLoadable());
  }

  @Test
  public void listenerIsRegisteredForEverySession() {
    Class<?> listener = options().getBaselineSessionEventsListenerBuilder().getAutoListener();
    assertNotNull(listener);
    assertEquals(QueryStatsListener.class.getName(), listener.getName());
  }

  @Test
  public void listenerCountsStatements() {
    QueryStatsListener stats = new QueryStatsListener();
    JPA.em().unwrap(Session.class).addEventListeners(stats);

    JPA.em().createQuery("select count(o) from Office o").getSingleResult();
    JPA.em().createQuery("select count(p) from Person p").getSingleResult();

    assertEquals(2, stats.getPrepared());
    assertEquals(2, stats.getExecuted());
    // la chiusura della sessione con statement non deve sollevare eccezioni
    stats.end();
  }
}
