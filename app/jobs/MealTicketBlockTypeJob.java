package jobs;

import lombok.extern.slf4j.Slf4j;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

@Slf4j
@On("0 0 6 * * ?") //tutte le mattine alle 6.00
public class MealTicketBlockTypeJob extends Job {

  @Override
  public void doJob() {
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.debug("Start meal ticket block type Job");
    /*
     * Inserire la chiamata al metodo getTipoBlocchetto della classe CertificationsCommunication
     * che recupererà le informazioni che servono da Attestati.
     */
  }
}
