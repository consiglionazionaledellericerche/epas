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
		

		Logger.info("Start Job checkDay");
		User userLogged = User.find("byUsername", "admin").first();	
		try {
			PersonUtility.fixPersonSituation(-1l, 2014, 1, userLogged);
		} catch (EmailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
