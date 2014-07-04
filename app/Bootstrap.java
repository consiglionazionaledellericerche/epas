
import java.util.List;

import org.joda.time.LocalDate;

import models.Contract;
import models.Office;
import models.Permission;
import models.Person;
import models.Role;
import models.StampProfile;
import models.StampProfileContract;
import models.User;
import models.UsersRolesOffices;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import controllers.Security;


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
		

		convertPersonBornDateHandler();
		
		cleanOfficeTree();
		
		bootstrapPermissionsHandler();
		
		createStampProfileContract();
		
		try
		{
			/*
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
			
			*/
			
			if(User.count() == 0){
				
				/*
				
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
				
				*/
		
			}
			
//			Person admin = Person.find("byUsername", "admin").first();
//			if(admin!=null && admin.office==null){
//				admin.office = (Office)Office.findAll().get(0);
//				admin.save();
//				
//			}
			
			/*
			
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
			
			*/

			/*
			//FIX seat IIT, creo la sede pisa e IIT diventa l'istituto 
			Office iit = Office.find("byName", "IIT").first();
			if(iit!=null) {
				iit.code = 1;
				iit.name = "Istituto Informatica e Telematica";
				iit.contraction = "IIT";
				iit.save();
				if(iit.persons.size()!=0) {

					RemoteOffice iitpisa = new RemoteOffice();
					iitpisa.name = "IIT - Pisa";
					iitpisa.code = iit.code;
					iitpisa.address = iit.address;
					iitpisa.joiningDate = new LocalDate(2013,1,1);
					iitpisa.office = iit;
					iitpisa.save();

					for(Person person : iit.persons) {

						person.office = iitpisa;
						person.save();
					}

					for(ConfYear confYear : iit.confYear) {

						confYear.office = iitpisa;
						confYear.save();
					}

					for(ConfGeneral confGeneral : iit.confGeneral) {

						confGeneral.office = iitpisa;
						confGeneral.save();
					}

					for(UsersPermissionsOffices upo : iit.userPermissionOffices) {

						upo.office = iitpisa;
						upo.save();
					}

				}
			}
			
			*/
			

			
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
	
	
	private static void bootstrapPermissionsHandler() {
		
		/* Metodo provvisiorio per popolare la tabella Permissions con i nuovi permessi */
		
		Permission permission;

		if (Permission.find("byDescription", Security.DEVELOPER).first() == null) {
			
			Role role = new Role();
			role.name = Role.PERSONNEL_ADMIN;
			role.save();
			
			Role roleMini = new Role();
			roleMini.name = Role.PERSONNEL_ADMIN_MINI;
			
			permission = new Permission();
			permission.description = Security.DEVELOPER;
			permission.save();
			
			permission = new Permission();
			permission.description = Security.EMPLOYEE;
			permission.save();
			
			permission = new Permission();
			permission.description = Security.VIEW_PERSON;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_PERSON;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.VIEW_PERSON_DAY;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_PERSON_DAY;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.VIEW_COMPETENCE;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_COMPETENCE;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.UPLOAD_SITUATION;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.VIEW_ABSENCE_TYPE;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_ABSENCE_TYPE;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.VIEW_CONFIGURATION;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_CONFIGURATION;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.VIEW_OFFICE;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_OFFICE;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.VIEW_WORKING_TIME_TYPE;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_WORKING_TIME_TYPE;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.VIEW_COMPETENCE_CODE;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_COMPETENCE_CODE;
			permission.save();
			role.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.VIEW_ADMINISTRATOR;
			permission.save();
			role.permissions.add(permission);
			roleMini.permissions.add(permission);
			
			permission = new Permission();
			permission.description = Security.EDIT_ADMINISTRATOR;
			permission.save();
			role.permissions.add(permission);
			
			role.save();
			roleMini.save();
			
			/* ADMIN per IIT  
			Person person = Person.find("bySurname", "Lucchesi").first();
			UsersRolesOffices uro = new UsersRolesOffices();
			uro.user = person.user;
			uro.role = Role.find("byName", Role.PERSONNEL_ADMIN).first();
			uro.office = person.office;
			uro.save();
			
			/* ADMIN_MINI per COSENZA 
			UsersRolesOffices uro2 = new UsersRolesOffices();
			uro2.user = person.user;
			uro2.role = Role.find("byName", Role.PERSONNEL_ADMIN_MINI).first();
			uro2.office = Office.find("byCode", new Integer("223410")).first();
			uro2.save();
			*/
		}
		
		
		
		//Ogni ufficio deve essere associato ad admin
		User admin = User.find("byUsername", "admin").first();
		Role role = Role.find("byName", Role.PERSONNEL_ADMIN).first();

		List<Office> officeList = Office.findAll();
		for(Office office : officeList) {
			UsersRolesOffices uro = UsersRolesOffices.find("Select uro from UsersRolesOffices uro "
					+ "where uro.office = ? and uro.user = ? and uro.role = ?", office, admin, role).first();
			if(uro==null) {
				uro = new UsersRolesOffices();
				uro.user = admin;
				uro.office = office;
				uro.role = role;
				uro.save();
			}
		}
	
	}


	private static void convertPersonBornDateHandler() {
		
		//FIXME applicare l'evoluzione che crea il campo e lanciare questo metodo
		//oppure migliorare l'evoluzione che effettui direttamente la conversione 
		//da Date a LocalDate. In ogni caso va eliminato il campo born_date
		
		List<Person> personList = Person.findAll();
		for(Person person : personList) {
			
			if(person.bornDate != null) {
				person.birthday = new LocalDate(person.bornDate);
				person.bornDate = null;
				person.save();
			}
		}
	}
	
	private static void cleanOfficeTree() {
		
		//Primo livello AREA
		
		//Secondo livello ISTITUTO
		
		//Terzo livello SEDE
		
		Office areaPisa = Office.find("byName", "Area CNR Pisa").first();
		if(areaPisa == null) {
			
			areaPisa = new Office();
			areaPisa.name = "Area CNR Pisa";
			areaPisa.code = null;
			areaPisa.address = null;
			areaPisa.contraction = null;
			areaPisa.joiningDate = null;
			areaPisa.confGeneral = null;
			areaPisa.save();
			
		}
		
		
		if(areaPisa.subOffices.size() == 0) {
			
			Office iit = new Office();
			iit.name = "Istituto IIT";
			iit.address = null;
			iit.code = null;
			iit.contraction = "IIT";
			iit.joiningDate = null;
			iit.office = areaPisa;
			iit.confGeneral = null;
			iit.save();
		}
		
		
		Office iit = Office.find("byName", "Istituto IIT").first();
		if(iit.subOffices.size()  == 0) {
			
			Office iitPisa = Office.find("byCode", 223400).first();
			Office iitCos = Office.find("byCode", 223410).first();
			
			iitPisa.office = iit;
			iitPisa.name = "IIT - Pisa";
			iitPisa.save();
			
			iitCos.office = iit;
			iitCos.save();
		}
		
		/**
		 * 
		 * CREAZIONE ISTI
		 * 
		 * 
		 */
		
		/*
		if(areaPisa.subOffices.size() == 1) {
			
			Office isti = new Office();
			isti.name = "Istituto ISTI";
			isti.address = null;
			isti.code = null;
			isti.contraction = "ISTI";
			isti.joiningDate = null;
			isti.office = areaPisa;
			isti.save();
		}
		
		
		Office isti = Office.find("byName", "Istituto ISTI").first();
		if(isti.subOffices.size()  == 0) {
			
			Office seatIsti1 = new Office();
			seatIsti1.name = "Istituto ISTI Sede 1";
			seatIsti1.address = null;
			seatIsti1.code = 1;
			seatIsti1.contraction = "Sede1";
			seatIsti1.joiningDate = null;
			seatIsti1.office = isti;
			seatIsti1.save();

			Office seatIsti2 = new Office();
			seatIsti2.name = "Istituto ISTI Sede 2";
			seatIsti2.address = null;
			seatIsti2.code = 2;
			seatIsti2.contraction = "Sede2";
			seatIsti2.joiningDate = null;
			seatIsti2.office = isti;
			seatIsti2.save();

		}
		*/
		
		/**
		 * 
		 * CREAZIONE AREA ROMANA
		 * 
		 */
		
		/*
		Office areaRoma = Office.find("byName", "Area CNR Roma").first();
		if(areaRoma == null) {
			
			areaRoma = new Office();
			areaRoma.name = "Area CNR Roma";
			areaRoma.code = null;
			areaRoma.address = null;
			areaRoma.contraction = null;
			areaRoma.joiningDate = null;
			areaRoma.save();
		}
		
		
		if(areaRoma.subOffices.size() == 0) {
			
			Office roma = new Office();
			roma.name = "Istituto Romano";
			roma.address = null;
			roma.code = null;
			roma.contraction = "IIT";
			roma.joiningDate = null;
			roma.office = areaRoma;
			roma.save();
		}
		
		
		Office romaInst = Office.find("byName", "Istituto Romano").first();
		if(romaInst.subOffices.size()  == 0) {
			
			Office seatRoma1 = new Office();
			seatRoma1.name = "Istituto Romano Sede 1";
			seatRoma1.address = null;
			seatRoma1.code = 1;
			seatRoma1.contraction = "Sede1";
			seatRoma1.joiningDate = null;
			seatRoma1.office = romaInst;
			seatRoma1.save();

			Office seatRoma2 = new Office();
			seatRoma2.name = "Istituto Romano Sede 2";
			seatRoma2.address = null;
			seatRoma2.code = 2;
			seatRoma2.contraction = "Sede2";
			seatRoma2.joiningDate = null;
			seatRoma2.office = romaInst;
			seatRoma2.save();

		}
		
		*/
		
	}
	
	
	private void createStampProfileContract(){
		if(StampProfileContract.count() == 0){
			Logger.info("inizio operazioni di creazione nuovi stamp profile associati ai contratti");
			List<Office> officeList = Office.findAll();
			List<Person> personList = Person.getActivePersonsInDay(new LocalDate(), officeList, false);
			for(Person p : personList){
				Logger.info("Inizio a creare i nuovi stamp profile per %s %s", p.name, p.surname);
				for(Contract c : p.contracts){
					for(StampProfile sp : p.stampProfiles){
						if(c.endContract == null || sp.endTo == null){
							StampProfileContract spc = new StampProfileContract();
							spc.contract = c;
							spc.startFrom = sp.startFrom;
							spc.endTo = sp.endTo;
							spc.stampProfile = sp;
							spc.save();
							
						}
						else
							
							if(sp.endTo.isEqual(c.endContract) && sp.startFrom.isEqual(c.beginContract)){
								StampProfileContract spc = new StampProfileContract();
								spc.contract = c;
								spc.startFrom = c.beginContract;
								spc.endTo = c.endContract;
								spc.stampProfile = sp;
								spc.save();
							}
					}
				}
//				Contract c = p.getCurrentContract();
//				for(StampProfile sp : p.stampProfiles){
//					if(sp.endTo.isEqual(c.endContract) && sp.startFrom.isEqual(c.beginContract)){
//						StampProfileContract spc = new StampProfileContract();
//						spc.contract = c;
//						spc.startFrom = c.beginContract;
//						spc.endTo = c.endContract;
//						spc.stampProfile = sp;
//						spc.save();
//					}					
//				}				
			}
		}
	}
	
	
}
