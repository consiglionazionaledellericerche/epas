package jobs;

import org.apache.commons.mail.EmailException;

import models.User;
import it.cnr.iit.epas.PersonUtility;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;

@On("0 43 16 ? * *")
//@On("0 0 15 ? * MON,WED,FRI")
public class ExpandableJob extends Job{

	public void doJob(){
		Logger.info("Start Job expandable");
		User userLogged = User.find("byUsername", "admin").first();	
		try {
			PersonUtility.checkNoAbsenceNoStamping(-1l, 2014, 1, userLogged);
		}
		catch(EmailException e){
			e.printStackTrace();
		}
		Logger.info("Concluso Job expandable");	
	}
}
