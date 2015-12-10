package cnr.sync.jobs;

import javax.inject.Inject;

import cnr.sync.manager.SyncManager;
import models.Office;
import models.Person;
import play.jobs.Job;

//@On("0 10 6 ? * MON")
//@On("0 40 10 * * ?")
public class CheckCnrEmailJob extends Job {

  @Inject
  static SyncManager syncManager;

  public void doJob() {
    if (Office.count() == 0 || Person.count() == 0)
      return;

    syncManager.syncronizeCnrEmail();

  }
}
