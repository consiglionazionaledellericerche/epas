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
import dao.PersonDayDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import models.Person;
import models.PersonDay;
import org.joda.time.LocalDate;
import play.Logger;
import play.Play;
import play.jobs.Job;

@Slf4j
public class RemoveInvalidStampingsJob extends Job<Void> {

  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static ConsistencyManager consistencyManager;

  private final Person person;
  private final LocalDate begin;
  private final LocalDate end;

  /**
   * Default constructor.
   */
  public RemoveInvalidStampingsJob(
      Person person, LocalDate begin, LocalDate end) {
    this.person = person;
    this.begin = begin;
    this.end = end;
  }

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    log.info("Inizio Job RemoveInvalidStampingsJob per {},Dal {} al {}", person, begin, end);
    List<PersonDay> persondays = personDayDao.getPersonDayInPeriod(person, begin, Optional.of(end));

    for (PersonDay pd : persondays) {
      pd.getStampings().stream().filter(stamping -> !stamping.valid).forEach(stamping -> {
        log.info("Eliminazione timbratura non valida per {} in data {} : {}",
            pd.getPerson().fullName(), pd.getDate(), stamping);
        stamping.delete();
      });
    }

    consistencyManager.updatePersonSituation(person.id, begin);

    Logger.info("Terminato Job RemoveInvalidStampingsJob per %s,Dal %s al %s", person, begin, end);
  }
}
