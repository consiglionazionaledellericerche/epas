package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.CompetenceManager;
import manager.ReperibilityManager;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

import javax.inject.Inject;

@Slf4j
@OnApplicationStart(async = true)
public class DeleteOutdatedCompetenceCodes extends Job {

  @Inject
  static CompetenceManager competenceManager;
  @Inject
  static ReperibilityManager reperibilityManager;
  
  
  public void doJob() {
    competenceManager.deleteObsoleteCompetenceCodes();
    log.debug("Terminata procedura di cancellazione dei codici di competenza non più usati.");
    reperibilityManager.associateOfficeToReperibilityService();
    log.debug("Associati eventuali reperibility type all'ufficio di appartenenza delle "
        + "persone associate a ciascun type");
  }
  
}
