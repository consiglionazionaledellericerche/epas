package jobs;

import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.Stamping;

import org.joda.time.LocalDate;

import play.Logger;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;


//@OnApplicationStart
@On("0 1 5 * * ?")
public class TestJob extends Job{
	
	public void doJob(){
		Logger.debug("*********************************************************************************");
		LocalDate yesterday = new LocalDate().minusDays(1);
		List<Person> active = Person.getActivePersons(new LocalDate());
		for(Person person : active)
		{
			PersonDay pd = PersonDay.find(""
					+ "SELECT pd "
					+ "FROM PersonDay pd "
					+ "WHERE pd.person = ? AND pd.date = ? ", 
					person, 
					yesterday)
					.first();
			
			if(pd!=null)
			{
				//check for error
				PersonDay.checkForError(pd, yesterday, person);
				continue;
			}
			
			if(pd==null)
			{
				if(DateUtility.isGeneralHoliday(yesterday))
				{
					continue;
				}
				if(person.workingTimeType.workingTimeTypeDays.get(yesterday.getDayOfWeek()-1).holiday)
				{
					continue;
				}
				
				pd = new PersonDay(person, yesterday);
				pd.create();
				pd.populatePersonDay();
				pd.save();
				//check for error
				PersonDay.checkForError(pd, yesterday, person);
				continue;
				
			}
		}
		Logger.debug("Fine metodo");
	}
	
	

}
