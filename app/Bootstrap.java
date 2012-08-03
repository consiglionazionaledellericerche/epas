import models.Configuration;
import models.Permission;
import models.Person;
import play.Logger;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;


/**
 * @author cristian
 *
 */
@OnApplicationStart
public class Bootstrap extends Job {
	

	public void doJob() {

		
		if (Permission.count() == 0) {
			Fixtures.loadModels("permissions.yml");
			Logger.info("Creati i permessi predefiniti e creato un utente amministratore con associati questi permessi");
		} 
		
		if(Configuration.count() == 0){
			Fixtures.loadModels("defaultConfiguration.yml");
			Logger.info("Creata la configurazione iniziale dei parametri");
		}
		
	}

}
