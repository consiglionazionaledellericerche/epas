package jobs;

import javax.inject.Inject;
import org.apache.commons.mail.EmailException;
import lombok.extern.slf4j.Slf4j;
import manager.PersonManager;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

@Slf4j
@On("0 30 7 ? * MON *") // Ore 7:30 ogni lunedì
public class CheckWeeklyWorkingTime extends Job {
  
  public static final int MAXWEEKLYMINUTES = 2880;
  
  @Inject
  static PersonManager personManager;

  @Override
  public void doJob() throws EmailException {

    
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Inizio CheckWeeklyWorkingTime");
    int counter = personManager.checkWeeklyWorkingTime(MAXWEEKLYMINUTES);
    log.info("Concluso CheckWeeklyWorkingTime, trovati %s dipendenti con orario settimanale superiore al limite previsto di 48 ore", counter);
  }
}
