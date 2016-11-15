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

//@On("0 34 15 ? * *")
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
