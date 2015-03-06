package jobs;

import javax.inject.Inject;

import manager.ConsistencyManager;
import models.User;

import org.apache.commons.mail.EmailException;

import play.Logger;
import play.jobs.Job;
import play.jobs.On;

import com.google.common.base.Optional;

import dao.UserDao;

//@On("0 34 15 ? * *")
@SuppressWarnings("rawtypes")
@On("0 0 15 ? * MON,WED,FRI")
public class ExpandableJob extends Job{

	@Inject
	static ConsistencyManager consistencyManager;
	
	public void doJob(){
		Logger.info("Start Job expandable");
		
		User userLogged = UserDao.getUserByUsernameAndPassword("admin", Optional.<String>absent());	
		
		try {
			consistencyManager.checkNoAbsenceNoStamping(2014, 1, userLogged);
		}
		catch(EmailException e){
			e.printStackTrace();
		}
		
		Logger.info("Concluso Job expandable");	
	}
}
