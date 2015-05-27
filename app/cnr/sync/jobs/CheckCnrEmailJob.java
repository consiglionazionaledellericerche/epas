package cnr.sync.jobs;

import groovy.util.logging.Slf4j;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.PersonManager;
import models.Office;
import models.Person;
import cnr.sync.dto.DepartmentDTO;
import cnr.sync.dto.PersonRest;
import cnr.sync.manager.SyncManager;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import dao.OfficeDao;
import dao.PersonDao;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.On;
import play.libs.WS;
import play.libs.WS.HttpResponse;

@On("0 10 6 ? * MON")
//@On("0 40 10 * * ?")
public class CheckCnrEmailJob extends Job{
	
	@Inject
	static SyncManager syncManager;

	public void doJob() {
		if (Office.count() == 0 || Person.count() == 0)
			return;
		
		syncManager.syncronizeCnrEmail();

	}
}
