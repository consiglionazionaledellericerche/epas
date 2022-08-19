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

//@On("0 34 15 ? * *")
/**
 * Job per la verifica dei trouble sui giorni dei dipendenti.
 *
 * @author dario
 *
 */
@SuppressWarnings("rawtypes")
@Slf4j
@On("0 0 15 ? * MON,WED,FRI")
public class ExpandableJob extends Job {

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

    log.info("Start Job expandable");

    final LocalDate fromDate = LocalDate.now().minusMonths(2);
    final LocalDate toDate = LocalDate.now().minusDays(1);

    personDayInTroubleManager.sendTroubleEmails(personDao.eligiblesForSendingAlerts(),
        fromDate, toDate, ImmutableList.of(Troubles.NO_ABS_NO_STAMP));

    log.info("Concluso Job expandable");
  }
}
