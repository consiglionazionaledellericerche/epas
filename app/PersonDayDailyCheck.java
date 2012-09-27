import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.On;

/**
 * 
 */

/**
 * @author cristian
 *
 */
/** Fire at 12pm (noon) every day **/ 
//@On("0 1 0 * * ?")
@On("0 57 15 * * ?")
public class PersonDayDailyCheck extends Job {

	@Override
	public void doJob() {
		Logger.info("Maintenance job ...");
		//Verifica la presenza del PersonDay del giorno precedente
		//Per ogni persona con contratto attivo, se il giorno precedente è per lei lavorativo,
		//se non c'è il personDay va creato
	}
}
