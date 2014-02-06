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
import controllers.Secure.Security;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;



@On("0 1 5 * * ?")
//@On("1 /1 * * * ?")
//@OnApplicationStart
public class TestJob extends Job{
	
	public void doJob(){
		

		
		Person person = Person.find("byUsername", "admin").first();	
		PersonUtility.fixPersonSituation(-1l, 2013, 1, person);

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
