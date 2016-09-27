package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.CompetenceManager;
import manager.ReperibilityManager;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

import javax.inject.Inject;

@Slf4j
@OnApplicationStart(async = true)
public class AssociateOfficeToReperibilityService extends Job {

  @Inject
  static ReperibilityManager reperibilityManager;
  
  
  public void doJob() {
    
    reperibilityManager.associateOfficeToReperibilityService();
    log.debug("Associati eventuali reperibility type all'ufficio di appartenenza delle "
        + "persone associate a ciascun type");
  }
  
}
