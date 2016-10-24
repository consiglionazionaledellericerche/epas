package jobs;

import com.google.common.collect.ImmutableList;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayInTroubleManager;

import models.enumerate.Troubles;

import org.joda.time.LocalDate;

import play.Play;
import play.jobs.Job;
import play.jobs.On;

import javax.inject.Inject;

/**
 * @author daniele
 * @since 20/10/16.
 */
@Slf4j
@On("0 0 15 ? * MON-FRI")
public class TrAutocertificationAlerts extends Job {

  @Inject
  static PersonDayInTroubleManager personDayInTroubleManager;
  @Inject
  static PersonDao personDao;


  private static final String JOBS_CONF = "jobs.active";
  // i primi 5 giorni del mese e dal 25 all'ultimo giorno di ogni mese alle 15, ma non nei weekend
  private static final int FIRST_DAY = 25;
  private static final int LATEST_DAY = 5;

  /**
   * Esecuzione Job.
   */
  public void doJob() {

    // in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(JOBS_CONF))) {
      log.info("ExpandableJob Interrotto. Disattivato dalla configurazione.");
      return;
    }

    final LocalDate today = LocalDate.now();
    final LocalDate from;
    final LocalDate to;

    if (today.getDayOfMonth() > LATEST_DAY && today.getDayOfMonth() < FIRST_DAY) {
      return;
    }
    // Se sono a inizio mese verifico tutto il mese precedente
    if (today.getDayOfMonth() <= LATEST_DAY) {
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
