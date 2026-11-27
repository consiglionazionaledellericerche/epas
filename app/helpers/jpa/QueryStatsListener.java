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

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.BaseSessionEventListener;
import play.Play;
import play.mvc.Http;

/**
 * Statistiche delle query per ogni sessione Hibernate, cioè per ogni richiesta HTTP o job.
 *
 * <p>Registrato per tutte le sessioni con {@code hibernate.session.events.auto} in
 * {@code conf/hibernate.properties}. Alla chiusura della sessione registra nel log l'azione,
 * il numero di statement JDBC preparati ed eseguiti, il tempo speso a eseguirli e la durata
 * della sessione. Il log è a livello DEBUG; diventa INFO quando gli statement preparati sono
 * almeno il valore di {@code jpa.queryStats.threshold} (default {@value #DEFAULT_THRESHOLD},
 * 0 per registrare sempre a INFO).</p>
 *
 * <p>Un'istanza per sessione: non serve sincronizzazione.</p>
 */
@Slf4j
public class QueryStatsListener extends BaseSessionEventListener {

  private static final long serialVersionUID = 1L;

  public static final String THRESHOLD_KEY = "jpa.queryStats.threshold";
  public static final int DEFAULT_THRESHOLD = 100;

  private final long sessionStart = System.nanoTime();
  private String action;

  private int prepared;
  private int executed;
  private int flushes;
  private long executeStart = -1;
  private long executeNanos;

  @Override
  public void jdbcPrepareStatementEnd() {
    prepared++;
    captureAction();
  }

  @Override
  public void jdbcExecuteStatementStart() {
    executeStart = System.nanoTime();
  }

  @Override
  public void jdbcExecuteStatementEnd() {
    executed++;
    if (executeStart >= 0) {
      executeNanos += System.nanoTime() - executeStart;
      executeStart = -1;
    }
  }

  @Override
  public void flushEnd(int numberOfEntities, int numberOfCollections) {
    flushes++;
  }

  public int getPrepared() {
    return prepared;
  }

  public int getExecuted() {
    return executed;
  }

  public long getExecuteMillis() {
    return TimeUnit.NANOSECONDS.toMillis(executeNanos);
  }

  @Override
  public void end() {
    if (prepared == 0) {
      return;
    }
    captureAction();
    final int threshold = threshold();
    final boolean heavy = prepared >= threshold;
    if (heavy ? !log.isInfoEnabled() : !log.isDebugEnabled()) {
      return;
    }
    final String message = String.format(
        "action=%s statements=%d executed=%d jdbcMs=%d sessionMs=%d flushes=%d",
        action, prepared, executed, getExecuteMillis(),
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sessionStart), flushes);
    if (heavy) {
      log.info(message);
    } else {
      log.debug(message);
    }
  }

  /**
   * L'azione si legge al primo statement: alla chiusura della sessione la richiesta corrente
   * potrebbe non essere più disponibile.
   */
  private void captureAction() {
    if (action != null) {
      return;
    }
    final Http.Request request = Http.Request.current();
    if (request != null && request.action != null) {
      action = request.action;
    } else {
      action = "thread:" + Thread.currentThread().getName();
    }
  }

  private static int threshold() {
    if (Play.configuration == null) {
      return DEFAULT_THRESHOLD;
    }
    try {
      return Integer.parseInt(Play.configuration.getProperty(
          THRESHOLD_KEY, String.valueOf(DEFAULT_THRESHOLD)).trim());
    } catch (NumberFormatException e) {
      return DEFAULT_THRESHOLD;
    }
  }
}
