package jobs;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.services.absences.AbsenceService;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@Slf4j
@OnApplicationStart(async = true)
public class AbsencesRedefinitionsJob extends Job<Void> {

  @Inject
  static AbsenceService absenceService;
  
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Lanciata procedura di allineamento codici di assenza");
    absenceService.enumAllineator();
    log.info("Procedura terminata");
    
  }
}
