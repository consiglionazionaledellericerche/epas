package jobs;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.List;

import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.Stamping;

import org.joda.time.LocalDate;

import controllers.Administration;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;



@On("0 1 5 * * ?")
//@On("1 /1 * * * ?")
//@OnApplicationStart
public class TestJob extends Job{
	
	public void doJob(){
		
		PersonUtility.fixPersonSituation(-1l, 2013, 1);
		Logger.info("------------------------------------------------------------------------------------------------------>");
	}
		

}
