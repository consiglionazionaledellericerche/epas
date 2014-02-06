import it.cnr.iit.epas.FromMysqlToPostgres;
import models.AbsenceType;
import models.CompetenceCode;
import models.Configuration;
import models.Permission;
import models.Person;
import models.PersonShift;
import models.Qualification;
import models.ShiftTimeTable;
import models.ShiftType;
import models.StampModificationType;
import models.StampType;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import play.Logger;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;


/**
 * Carica nel database dell'applicazione i dati iniziali predefiniti nel caso questi non siano già presenti 
 * 
 * @author cristian
 *
 */
@OnApplicationStart
public class Bootstrap extends Job {
	
	public void doJob() {
//		if (ShiftType.count() == 0) {
		//			Fixtures.loadModels("shiftTypes.yml");
		//			Logger.info("Creati i tipi di turno");
		//		}
		//		
		//		if (ShiftTimeTable.count() == 0) {
		//			Fixtures.loadModels("shiftTimeTables.yml");
		//			Logger.info("Create le fasce di orario dei vari turni");
		//		}

		try
		{
			if (Permission.count() <= 1) {
				Fixtures.loadModels("permissions.yml");
				Logger.info("Creati i permessi predefiniti e creato un utente amministratore con associati questi permessi");
			}
			
			/*
			if(AbsenceType.count() == 0)
			{
				Fixtures.loadModels("absenceTypes.yml");
				Logger.info("Creata la struttura dati dei tipi assenza predefiniti");
			}
			
			if (Qualification.count() == 0)	{
				Fixtures.loadModels("qualifications.yml");
				Logger.info("Create le qualifiche predefinite");
			}
			*/
			
			if(AbsenceType.count() == 0)
			{
				Fixtures.loadModels("absenceTypesAndQualifications.yml");
				Logger.info("Creata la struttura dati dei tipi assenza predefiniti e delle qualifiche");
			}
			
			if(CompetenceCode.count() == 0)
			{
				Fixtures.loadModels("competenceCodes.yml");
				Logger.info("Creata la struttura dati dei tipi competenze");
			}

			if (WorkingTimeType.count() == 0) {
				createWorkinTimeTypeNormaleMod();
				Logger.info("Creato il workingTimeType predefinito \"normale-mod\" con i rispettivi WorkingTimeTypeDay");
			}

			if (StampModificationType.count() == 0)	{
				Fixtures.loadModels("stampModificationTypes.yml");
				Logger.info("Creati gli StampModificationType predefiniti");
			}

			if (StampType.count() == 0)	{
				Fixtures.loadModels("stampTypes.yml");
				Logger.info("Creati gli StampType predefiniti");
			}

			if(Configuration.count() == 0){
				Fixtures.loadModels("defaultConfiguration.yml");
				Logger.info("Creata la configurazione iniziale per il programma");
			}
			
			

	
		}
		catch(RuntimeException e)
		{
			//do nothing (test exception)
			e.printStackTrace();
		}

	}


	/**
	 * Crea il tipo di WorkingTimeTypeNormaleMod. Questo tipo non era presente nella vecchia applicazione
	 * che mostrava per default questo tipo di Orario quando la persona non aveva nessuno Orario impostato.
	 */
	private static void createWorkinTimeTypeNormaleMod() {
		final String NORMALE_MOD = "normale-mod";

		Logger.debug("Inizio a creare il workingTimeType \"%s\"", NORMALE_MOD);
		if (WorkingTimeType.find("byDescription", NORMALE_MOD).first() != null) {
			Logger.warn("Il WorkingTimeType %s è gia presente nel db, non ne verrà creato uno nuovo", NORMALE_MOD);
			return;
		}

		WorkingTimeType wttNew = new WorkingTimeType();

		wttNew.description = NORMALE_MOD;
		wttNew.shift = false;
		wttNew.create();
		
		WorkingTimeTypeDay wttd = null;
		for(int dayOfWeek=1; dayOfWeek<=5; dayOfWeek++){
			wttd = new WorkingTimeTypeDay();
			wttd.workingTimeType = wttNew;
			wttd.breakTicketTime = 30;
			wttd.dayOfWeek = dayOfWeek;
			wttd.holiday = false;
			wttd.mealTicketTime = 360;
			wttd.timeMealFrom = 0;
			wttd.timeMealTo = 0;
			wttd.timeSlotEntranceFrom = 0;
			wttd.timeSlotEntranceTo = 0;
			wttd.timeSlotExitFrom = 0;
			wttd.timeSlotExitTo = 0;
			wttd.willBeSaved = false;
			wttd.workingTime = 432;
			wttd.save();
			Logger.debug("Creato il WorkingTimeTypeDay per il giorno %d del WorkingTimeType %s", dayOfWeek, wttNew.description);

		}
		for(int dayOfWeek=6; dayOfWeek <= 7; dayOfWeek++){
			wttd = new WorkingTimeTypeDay();
			wttd.workingTimeType = wttNew;
			wttd.breakTicketTime = 30;
			wttd.dayOfWeek = dayOfWeek;
			wttd.holiday = true;
			wttd.mealTicketTime = 360;
			wttd.timeMealFrom = 0;
			wttd.timeMealTo = 0;
			wttd.timeSlotEntranceFrom = 0;
			wttd.timeSlotEntranceTo = 0;
			wttd.timeSlotExitFrom = 0;
			wttd.timeSlotExitTo = 0;
			wttd.willBeSaved = false;
			wttd.workingTime = 432;
			wttd.save();
			Logger.debug("Creato il WorkingTimeTypeDay per il giorno %d del WorkingTimeType %s", dayOfWeek, wttNew.description);
		}
	}
}
