package jobs;

import org.apache.commons.mail.EmailException;

import it.cnr.iit.epas.PersonUtility;
import models.User;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;



@On("0 1 5 * * ?")
//@On("0 30 14 * * ?")
public class DarkNightJob extends Job{
	
	public void doJob(){
		
		Logger.info("Start DarkNightJob");
		
		User userLogged = User.find("byUsername", "admin").first();	
	
		PersonUtility.fixPersonSituation(-1l, 2014, 4, userLogged, true);
		
		Logger.info("Concluso DarkNightJob");

	}
		

}
