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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import dao.PersonDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.PersonDayInTroubleManager;
import models.enumerate.Troubles;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

/**
 * Job notturno per l'allineamento delle situazioni dei dipendenti.
 *
 * @author dario
 *
 */
@SuppressWarnings("rawtypes")
@Slf4j
@On("0 1 5 * * ?") // Ore 5:01
// @Every("30s") // ogni 30 secondi.
public class DarkNightJob extends Job {

  private static final List<Integer> weekEnd = ImmutableList
      .of(DateTimeConstants.SATURDAY, DateTimeConstants.SUNDAY);

  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayInTroubleManager personDayInTroubleManager;

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

    log.info("Start DarkNightJob");


    consistencyManager.fixPersonSituation(Optional.absent(), Optional.absent(),
        LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue(), false);

    LocalDate begin = LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue();
    LocalDate end = LocalDate.now().minusDays(1);

    if (!weekEnd.contains(LocalDate.now().getDayOfWeek())) {
      log.debug("Inizia la parte di invio email...");

      personDayInTroubleManager.sendTroubleEmails(personDao.eligiblesForSendingAlerts(),
          begin, end, ImmutableList.of(Troubles.UNCOUPLED_WORKING));
    }

    log.info("Concluso DarkNightJob");

  }
}
