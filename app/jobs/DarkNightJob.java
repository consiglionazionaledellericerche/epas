package jobs;

import com.google.common.base.Optional;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;

import models.Person;
import models.User;

import org.joda.time.LocalDate;

import play.Play;
import play.jobs.Job;
import play.jobs.On;

import javax.inject.Inject;

@SuppressWarnings("rawtypes")
@Slf4j
@On("0 1 5 * * ?") // Ore 5:01
//@On("0/30 * * * * ?") ogni 30 secondi.
public class DarkNightJob extends Job {

  private static final String JOBS_CONF = "jobs.active";
  @Inject
  static ConsistencyManager consistencyManager;

  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(JOBS_CONF))) {
      log.info("DarkNightJob Interrotto. Disattivato dalla configurazione.");
      return;
    }

    log.info("Start DarkNightJob");

    consistencyManager.fixPersonSituation(
            Optional.<Person>absent(), Optional.<User>absent(),
            LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue(), true, false);

    log.info("Concluso DarkNightJob");

  }

}
