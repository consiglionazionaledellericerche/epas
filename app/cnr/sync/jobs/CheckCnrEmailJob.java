package cnr.sync.jobs;

import cnr.sync.manager.SyncManager;

import models.Office;
import models.Person;

import play.jobs.Job;

import javax.inject.Inject;

//@On("0 10 6 ? * MON")
//@On("0 40 10 * * ?")
@SuppressWarnings("rawtypes")
public class CheckCnrEmailJob extends Job {

  @Inject
  static SyncManager syncManager;

  @Override
  public void doJob() {
    if (Office.count() == 0 || Person.count() == 0) {
      return;
    }
    syncManager.syncronizeCnrEmail();

  }
}
