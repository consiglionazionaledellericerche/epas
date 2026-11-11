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
import org.junit.Test;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.test.UnitTest;

/**
 * Fuori da una richiesta HTTP il job deve partire solo dopo il commit della transazione
 * corrente, non subito su un altro thread.
 */
public class AfterCommitJobsTest extends UnitTest {

  @Test
  public void jobInsideTransactionWaitsForCommit() throws InterruptedException {
    assertTrue(JPA.isInsideTransaction());
    CountDownLatch executed = new CountDownLatch(1);

    AfterCommitJobs.schedule(new Job<Void>() {
      @Override
      public void doJob() {
        executed.countDown();
      }
    });

    assertFalse("Il job non deve partire prima del commit",
        executed.await(1, TimeUnit.SECONDS));
  }
}
