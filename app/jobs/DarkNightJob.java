package jobs;

import javax.inject.Inject;

import manager.ConsistencyManager;
import models.User;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;

import com.google.common.base.Optional;

import dao.UserDao;



@SuppressWarnings("rawtypes")
@On("0 1 5 * * ?")

//@On("1 /1 * * * ?")
//@OnApplicationStart

//@On("0 30 14 * * ?")

public class DarkNightJob extends Job{
	
	@Inject
	static ConsistencyManager consistencyManager;
	
	public void doJob(){
		
		Logger.info("Start DarkNightJob");
		
		User userLogged = UserDao.getUserByUsernameAndPassword("admin", Optional.<String>absent());	

		consistencyManager.fixPersonSituation(-1l, 2014, 4, userLogged, true);
		
		Logger.info("Concluso DarkNightJob");

	}
		

}
