package jobs;

import dao.OfficeDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.CheckGreenPassManager;
import manager.EmailManager;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

/**
 * Job per il controllo del green pass.
 *
 * @author dario
 *
 */
@Slf4j
@On("0 15 10 ? * MON-FRI") //tutti i giorni dal lunedi al venerdi di ogni mese alle 10.15
public class CheckGreenPassJob extends Job<Void> {

  static final String GREENPASS_CONF = "greenpass.active";
  
  @Inject
  static CheckGreenPassManager passManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static EmailManager emailManager;

  @Override
  public void doJob() {
    
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    if (!"true".equals(Play.configuration.getProperty(GREENPASS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    log.info("Start Check Green Pass Job");
    passManager.checkGreenPassProcedure(LocalDate.now());
    log.info("End Check Green Pass Job");
  }
}
