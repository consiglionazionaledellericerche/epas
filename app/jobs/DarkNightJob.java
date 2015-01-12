package jobs;

import com.google.common.base.Optional;

import dao.UserDao;
import it.cnr.iit.epas.PersonUtility;
import models.User;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;



@SuppressWarnings("rawtypes")
@On("0 1 5 * * ?")

//@On("1 /1 * * * ?")
//@OnApplicationStart

//@On("0 30 14 * * ?")

public class DarkNightJob extends Job{
	
	public void doJob(){
		
		Logger.info("Start DarkNightJob");
		
		User userLogged = UserDao.getUserByUsernameAndPassword("admin", Optional.<String>absent());
//		User userLogged = User.find("byUsername", "admin").first();	

		PersonUtility.fixPersonSituation(-1l, 2014, 4, userLogged, true);

		
		Logger.info("Concluso DarkNightJob");

	}
		

}
