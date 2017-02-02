package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.ShiftManager;

import play.jobs.Job;

import javax.inject.Inject;

@Slf4j
//@OnApplicationStart(async = true)
public class AssociateOfficeToShiftService extends Job<Void> {

  @Inject
  static ShiftManager shiftManager;
  
  @Override
  public void doJob() {
    
    shiftManager.associateOfficeToShiftService();
    log.debug("Associati eventuali categorie di turno all'ufficio di appartenenza dei "
        + "responsabili associati a ciascuna categoria di turno rilevata.");
  }
  
}
