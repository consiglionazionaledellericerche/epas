package jobs;

import it.cnr.iit.epas.PersonUtility;
import models.User;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;



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
