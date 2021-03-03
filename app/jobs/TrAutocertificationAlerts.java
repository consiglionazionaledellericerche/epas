/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package jobs;

import com.google.common.collect.ImmutableList;
import dao.PersonDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayInTroubleManager;
import models.enumerate.Troubles;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;


/**
 * Invia degli alert ai tecnologi e ricercatori che hanno
 * l'autocertificazione delle presenze/assenze attiva ma
 * hanno dei giorni non giustificati nel mese precedente.
 */
@Slf4j
// Ogni giorno alle 15 dal lunedì al venerdì
@On("0 0 15 ? * MON-FRI")
public class TrAutocertificationAlerts extends Job<Void> {

  private static final int DAYS = 5;

  @Inject
  static PersonDayInTroubleManager personDayInTroubleManager;
  @Inject
  static PersonDao personDao;


  /**
   * Esecuzione Job.
   */
  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    final LocalDate today = LocalDate.now();
    final LocalDate from;
    final LocalDate to;

    // Quinto giorno del mese
    final LocalDate fifthDayOfMonth = today.withDayOfMonth(DAYS);
    // Quintultimo giorno del mese
    final LocalDate fifthFromLast = today.dayOfMonth()
        .withMaximumValue().minusDays(DAYS);

    // Solo i primi 5 e gli ultimi 5 giorni del mese
    if (today.isAfter(fifthDayOfMonth) && today.isBefore(fifthFromLast)) {
      return;
    }
    // Se sono a inizio mese verifico tutto il mese precedente
    if (!today.isAfter(fifthDayOfMonth)) {
      from = today.minusMonths(1).dayOfMonth().withMinimumValue();
      to = today.minusMonths(1).dayOfMonth().withMaximumValue();
    } else {
      // Tutto il mese attuale fino a ieri
      from = today.dayOfMonth().withMinimumValue();
      to = today.minusDays(1);
    }

    log.info("Start Job TrAutocertificationAlerts");

    personDayInTroubleManager.sendTroubleEmails(personDao.trWithAutocertificationOn(),
        from, to, ImmutableList.of(Troubles.NO_ABS_NO_STAMP, Troubles.UNCOUPLED_WORKING));

    log.info("Concluso Job TrAutocertificationAlerts");
  }
}
