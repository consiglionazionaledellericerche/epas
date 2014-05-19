
import java.util.ArrayList;
import java.util.List;

import models.CompetenceCode;
import models.ConfGeneral;
import models.ConfYear;
import models.Configuration;
import models.Office;
import models.Permission;
import models.Qualification;
import models.StampModificationType;
import models.StampType;
import models.User;
import models.UsersPermissionsOffices;
import models.VacationCode;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.enumerate.ConfigurationFields;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.libs.Codec;
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

		if (Play.id.equals("test")) {
			Logger.info("Application in test mode, default boostrap job not started");
			return;
		}
		
		try
		{
			if(Qualification.count() == 0){
				Fixtures.loadModels("absenceTypesAndQualifications.yml");
				Logger.info("Create qualifiche e codici di assenza");
			}
			if (Permission.count() <= 1) {

				Fixtures.loadModels("permissions.yml");
				Logger.info("Creati i permessi predefiniti e creato un utente amministratore con associati questi permessi");
			}

			if(CompetenceCode.count() == 0)
			{
				Fixtures.loadModels("competenceCodes.yml");
				Logger.info("Creata la struttura dati dei tipi competenze");
			}
			
			if(VacationCode.count() == 0)
			{
				Fixtures.loadModels("vacationCodes.yml");
				Logger.info("Creata la struttura dati dei piani ferie predefiniti");
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
			
			if(Office.count() == 0){
				//Configuration conf = (Configuration)Configuration.findAll().get(0);
				//ConfGeneral conf = ConfGeneral.getConfGeneral();
				Office office = new Office();
				Integer seatCode = Integer.parseInt(ConfGeneral.getFieldValue(ConfigurationFields.SeatCode.description, office));
				String instituteName = ConfGeneral.getFieldValue(ConfigurationFields.InstituteName.description, office);
				office.code = seatCode;
				office.name = instituteName;
				office.save();
				Logger.info("Creato ufficio di default con nome %s e codice %s", instituteName, seatCode);
			}
			
			if(User.count() == 0){
				User admin = new User();
				admin.username = "admin";
				admin.password = Codec.hexMD5("personnelEpasNewVersion");
				admin.save();
				
				List<String> descPermissions = new ArrayList<String>();
				descPermissions.add("insertAndUpdateOffices");
				descPermissions.add("viewPersonList");
				descPermissions.add("deletePerson");
//				descPermissions.add("insertAndUpdateStamping");
				descPermissions.add("insertAndUpdatePerson");
//				descPermissions.add("insertAndUpdateWorkingTime");
//				descPermissions.add("insertAndUpdateAbsence");
				descPermissions.add("insertAndUpdateConfiguration");
				descPermissions.add("insertAndUpdatePassword");
				descPermissions.add("insertAndUpdateAdministrator");
//				descPermissions.add("insertAndUpdateCompetences");
//				descPermissions.add("insertAndUpdateVacations");
//				descPermissions.add("viewPersonalSituation");
//				descPermissions.add("uploadSituation");
				
				List<Permission> permissions = Permission.find("description in (?1)", descPermissions).fetch();
				
				List<UsersPermissionsOffices> usersPermissionOffices = new ArrayList<UsersPermissionsOffices>(); 
				
				Office office = Office.findById(1L);
				if(office == null){
					office = new Office();
					office.save();
				}
				
				for (Permission p: permissions){
					UsersPermissionsOffices upo = new UsersPermissionsOffices();
					upo.office = office;
					upo.user = admin;
					upo.permission = p;
					upo.save();
					usersPermissionOffices.add(upo);
				}
				
				admin.userPermissionOffices = usersPermissionOffices;
				admin.save();
		
			}
			
//			Person admin = Person.find("byUsername", "admin").first();
//			if(admin!=null && admin.office==null){
//				admin.office = (Office)Office.findAll().get(0);
//				admin.save();
//				
//			}
			
			//Fix Creazione configurazione 2012 se non esiste
			List<ConfYear> confYearList = ConfYear.find("byYear", 2012).fetch();
			if(confYearList.size() == 0) {
				
				List<ConfYear> confYear2013 = ConfYear.find("byYear", 2013).fetch();
				for(ConfYear confYear : confYear2013) {
					ConfYear newConf2012 = new ConfYear();
					newConf2012.field = confYear.field;
					newConf2012.fieldValue = confYear.fieldValue;
					newConf2012.office = confYear.office;
					newConf2012.year = 2012;
					newConf2012.save();
				}
				
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
