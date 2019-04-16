package jobs;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Job per aggiornare le configurazioni degli uffici con gli eventuali nuovi
 * paramtri di configurazione.
 * 
 * @author daniele
 * @since 30/06/16.
 */
@Slf4j
@OnApplicationStart(async = true)
public class OfficeConfigurationFix extends Job<Void> {

  @Inject
  static ConfigurationManager configurationManager;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    configurationManager.updateAllOfficesConfigurations();
  }
}
