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

import javax.transaction.Status;
import javax.transaction.Synchronization;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import play.Invoker.InvocationContext;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.mvc.Http;

/**
 * Avvio di job che devono leggere i dati salvati dalla transazione corrente.
 *
 * <p>Il job gira su un altro thread e con un'altra transazione, quindi deve partire solo dopo
 * il salvataggio dei dati. {@link Job#afterRequest()} lo garantisce solo durante una richiesta
 * HTTP: altrimenti (job schedulati, test) esegue subito il job, che non vede i dati non ancora
 * salvati.</p>
 */
@Slf4j
public final class AfterCommitJobs {

  private AfterCommitJobs() {
  }

  /**
   * Esegue il job dopo il salvataggio della transazione corrente.
   *
   * <ul>
   *   <li>durante una richiesta HTTP: alla fine della richiesta, come {@link Job#afterRequest()};
   *   </li>
   *   <li>dentro una transazione attiva: dopo il commit; se la transazione viene annullata il job
   *   non parte;</li>
   *   <li>altrimenti: subito.</li>
   * </ul>
   *
   * @param job il job da eseguire
   */
  public static void schedule(Job<?> job) {
    InvocationContext current = InvocationContext.current();
    if (current != null && Http.invocationType.equals(current.getInvocationType())) {
      job.afterRequest();
      return;
    }
    if (JPA.isInsideTransaction() && JPA.em().getTransaction().isActive()) {
      JPA.em().unwrap(Session.class).getTransaction()
          .registerSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
              // niente da fare prima del commit
            }

            @Override
            public void afterCompletion(int status) {
              if (status == Status.STATUS_COMMITTED) {
                job.now();
              } else {
                log.debug("Transazione non salvata (stato {}): job {} non eseguito",
                    status, job.getClass().getName());
              }
            }
          });
      return;
    }
    job.now();
  }
}
