package jobs;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
import manager.PersonDayInTroubleManager;

import models.enumerate.Troubles;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Play;
import play.jobs.Job;
import play.jobs.On;

import java.util.List;

import javax.inject.Inject;

@SuppressWarnings("rawtypes")
@Slf4j
@On("0 1 5 * * ?") // Ore 5:01
// @Every("30s") ogni 30 secondi.
public class DarkNightJob extends Job {

  private static final String JOBS_CONF = "jobs.active";
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
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(JOBS_CONF))) {
      log.info("DarkNightJob Interrotto. Disattivato dalla configurazione.");
      return;
    }

    log.info("Start DarkNightJob");


    consistencyManager.fixPersonSituation(Optional.absent(), Optional.absent(),
        LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue(), false);

    LocalDate begin = LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue();
    LocalDate end = LocalDate.now().minusDays(1);

    if (!weekEnd.contains(LocalDate.now().getDayOfWeek())) {
      log.info("Inizia la parte di invio email...");

      personDayInTroubleManager.sendTroubleEmails(personDao.eligiblesForSendingAlerts(),
          begin, end, ImmutableList.of(Troubles.UNCOUPLED_WORKING));
    }

    log.info("Concluso DarkNightJob");

  }
}
