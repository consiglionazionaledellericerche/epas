package jobs;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;

import models.ConfGeneral;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.StampModificationType;
import models.Stamping;
import models.User;

import org.joda.time.LocalDate;

import controllers.Administration;
import controllers.Secure.Security;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;



@On("0 1 5 * * ?")
//@On("1 /1 * * * ?")
//@OnApplicationStart
public class DarkNightJob extends Job{
	
	public void doJob(){
		

		Logger.info("Start Job checkDay");
		User userLogged = User.find("byUsername", "admin").first();	
		PersonUtility.fixPersonSituation(-1l, 2013, 1, userLogged);
		
		/*
		LocalDate yesterday = new LocalDate().minusDays(1);
		
		List<Person> activePersons = Person.getActivePersonsInMonth(yesterday.getMonthOfYear(), yesterday.getYear(), false);
		for(Person person : activePersons)
		{
			PersonUtility.checkPersonDay(person.id, yesterday);
		}
		*/
		Logger.info("Concluso Job checkDay");

	}
		

}
