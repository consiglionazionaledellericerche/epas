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
//@OnApplicationStart
public class TestJob extends Job{
	
	public void doJob(){
		Logger.info("Lanciato Job checkDay");
		
		PersonUtility.fixPersonSituation(-1l, 2013, 1);
		/*
		LocalDate yesterday = new LocalDate().minusDays(1);
		
		List<Person> activePersons = Person.getActivePersonsInMonth(yesterday.getMonthOfYear(), yesterday.getYear());
		for(Person person : activePersons)
		{
			PersonUtility.checkPersonDay(person.id, yesterday);
		}
		*/
		Logger.info("Concluso Job checkDay");
	}
	
	

}
