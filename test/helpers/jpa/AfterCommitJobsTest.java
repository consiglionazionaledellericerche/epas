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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityTransaction;
import org.junit.After;
import org.junit.Test;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.test.UnitTest;

/**
 * Fuori da una richiesta HTTP il job deve partire solo dopo il commit della transazione
 * corrente, non deve partire se la transazione viene annullata e, senza transazione attiva,
 * deve partire subito.
 */
public class AfterCommitJobsTest extends UnitTest {

  private static final long WAIT_SECONDS = 5;
  private static final long NOT_EXECUTED_MILLIS = 500;

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
  public void jobStartsOnlyAfterCommit() throws InterruptedException {
    EntityTransaction tx = JPA.em().getTransaction();
    assertTrue(tx.isActive());
    CountDownLatch executed = new CountDownLatch(1);

    AfterCommitJobs.schedule(countDownJob(executed));

    assertFalse("Il job non deve partire prima del commit",
        executed.await(NOT_EXECUTED_MILLIS, TimeUnit.MILLISECONDS));
    tx.commit();
    assertTrue("Il job deve partire dopo il commit",
        executed.await(WAIT_SECONDS, TimeUnit.SECONDS));
  }

  @Test
  public void jobDoesNotStartAfterRollback() throws InterruptedException {
    EntityTransaction tx = JPA.em().getTransaction();
    assertTrue(tx.isActive());
    CountDownLatch executed = new CountDownLatch(1);

    AfterCommitJobs.schedule(countDownJob(executed));

    tx.rollback();
    assertFalse("Il job non deve partire se la transazione viene annullata",
        executed.await(NOT_EXECUTED_MILLIS, TimeUnit.MILLISECONDS));
  }

  @Test
  public void jobStartsImmediatelyWithoutActiveTransaction() throws InterruptedException {
    EntityTransaction tx = JPA.em().getTransaction();
    tx.commit();
    assertFalse(tx.isActive());
    CountDownLatch executed = new CountDownLatch(1);

    AfterCommitJobs.schedule(countDownJob(executed));

    assertTrue("Senza transazione attiva il job deve partire subito",
        executed.await(WAIT_SECONDS, TimeUnit.SECONDS));
  }

  private static Job<Void> countDownJob(CountDownLatch latch) {
    return new Job<Void>() {
      @Override
      public void doJob() {
        latch.countDown();
      }
    };
  }
}
