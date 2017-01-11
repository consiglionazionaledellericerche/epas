package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.ShiftManager;

import models.PersonShift;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

import javax.inject.Inject;

@Slf4j
@OnApplicationStart(async = true)
public class AssociatePeopleToShiftActivity extends Job {

  @Inject
  static ShiftManager shiftManager;

  public void doJob() {

    if (PersonShift.count() == 0) {
      shiftManager.populatePersonShiftTable();
      log.info("Associate le persone con turno abilitato alla tabella person_shift");
    } else {
      log.info("Situazione relativa ad associazione persone in turno già aggiornata.");
    }    
    
  }
}
