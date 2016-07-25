package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.CompetenceManager;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

import javax.inject.Inject;

@Slf4j
@OnApplicationStart(async = true)
public class DeleteOutdatedCompetenceCodes extends Job {

  @Inject
  static CompetenceManager competenceManager;
  
  
  public void doJob() {
    competenceManager.deleteObsoleteCompetenceCodes();
    log.debug("Terminata procedura di cancellazione dei codici di competenza non più usati.");
  }
  
}
