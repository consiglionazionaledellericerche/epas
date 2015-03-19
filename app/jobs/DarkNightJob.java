package jobs;

import org.joda.time.LocalDate;
import javax.inject.Inject;

import manager.ConsistencyManager;
import models.Person;
import models.User;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;

import com.google.common.base.Optional;



@SuppressWarnings("rawtypes")
@On("0 1 5 * * ?") // Ore 5:01
//@On("0 /1 * * * ?") // Ogni minuto

public class DarkNightJob extends Job{
	
	@Inject
	static ConsistencyManager consistencyManager;
	
	public void doJob(){
		
		Logger.info("Start DarkNightJob");

		consistencyManager.fixPersonSituation(Optional.<Person>absent(),
				Optional.<User>absent(),LocalDate.now().minusYears(1), true);
		
		Logger.info("Concluso DarkNightJob");

	}
		

}
