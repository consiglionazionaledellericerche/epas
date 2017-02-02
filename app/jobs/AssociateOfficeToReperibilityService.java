package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.ReperibilityManager;

import play.Play;
import play.jobs.Job;

import javax.inject.Inject;

@Slf4j
//@OnApplicationStart(async = true)
public class AssociateOfficeToReperibilityService extends Job<Void> {

  @Inject
  static ReperibilityManager reperibilityManager;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    reperibilityManager.associateOfficeToReperibilityService();
    log.debug("Associati eventuali reperibility type all'ufficio di appartenenza delle "
        + "persone associate a ciascun type");
  }

}
