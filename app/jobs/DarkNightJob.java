package jobs;

import javax.inject.Inject;

import manager.ConsistencyManager;
import models.Person;
import models.User;

import org.joda.time.LocalDate;

import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

import com.google.common.base.Optional;

@SuppressWarnings("rawtypes")
//@On("0 1 5 * * ?") // Ore 5:01
@On("0 /1 * * * ?") // Ogni minuto
public class DarkNightJob extends Job{

	@Inject
	static ConsistencyManager consistencyManager;
	
	private final static String JOBS_CONF = "jobs.active";

	public void doJob(){
		
//		in modo da inibire l'esecuzione dei job in base alla configurazione
		if(!Play.configuration.getProperty(JOBS_CONF).equals("true")){
			Logger.info("DarkNightJob Interrotto. Disattivato dalla configurazione.");
			return;
		}

		Logger.info("Start DarkNightJob");

		consistencyManager.fixPersonSituation(
				Optional.<Person>absent(),Optional.<User>absent(),
				LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue(), true);

		Logger.info("Concluso DarkNightJob");

	}

}
