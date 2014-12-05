
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import models.Office;
import models.Permission;
import models.Qualification;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.libs.Codec;

import com.google.common.io.Resources;

import controllers.Security;

/**
 * Carica nel database dell'applicazione i dati iniziali predefiniti nel caso questi non siano già presenti 
 * 
 * @author cristian
 *
 */
@OnApplicationStart
public class Bootstrap extends Job<Void> {

	public static class DatasetImport implements Work {

		private DatabaseOperation operation;
		private final URL url;

		public DatasetImport(DatabaseOperation operation, URL url) {
			this.operation = operation;
			this.url = url;
		}

		@Override
		public void execute(Connection connection) {
			try {
				//org.dbunit.dataset.datatype.DefaultDataTypeFactory
				IDataSet dataSet = new FlatXmlDataSetBuilder()
				.setColumnSensing(true).build(url);
				operation.execute(new H2Connection(connection, ""), dataSet);
			} catch (DataSetException e) {
				e.printStackTrace();
			} catch (DatabaseUnitException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void doJob() {

		if (Play.id.equals("test")) {
			Logger.info("Application in test mode, default boostrap job not started");
			return;
		}


		Session session = (Session) JPA.em().getDelegate();
		
		if(Qualification.count() == 0 ) {
			
			//qualification absenceType absenceTypeQualification absenceTypeGroup
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class, "absence-type-and-qualification-phase1.xml")));		
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class, "absence-type-and-qualification-phase2.xml")));

			//competenceCode
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class, "competence-codes.xml")));	

			//stampModificationType
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class, "stamp-modification-types.xml")));

			//stampType
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class, "stamp-types.xml")));

			//vacationCode
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class, "vacation-codes.xml")));
			
			//workingTimeType workingTimeTypeDay
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class, "working-time-types.xml")));

		}
		
		if(User.count() == 0) {
			User admin = new User();
			admin.username = "admin";
			admin.password = Codec.hexMD5("personnelEpasNewVersion");
			admin.save();
		}

		bootstrapStampingCreateHandler();
				
		bootstrapPermissionsHandler();

		
		/*
		convertPersonBornDateHandler();
		
		cleanOfficeTree();
		
		bootstrapPermissionsHandler();

		createContractStampProfile2();

		bootstrapTotalOvertimeHandler();
		
		insertDefaultStampModificationType();
		 */
		
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
	
	private static void bootstrapStampingCreateHandler() {
		
		//1) Creazione Ruolo con permesso
		Role role = Role.find("byName",  Role.BADGE_READER).first();
		if(role == null) {
			role = new Role();
			role.name = Role.BADGE_READER;
			role.save();
			Permission permission = new Permission();
			permission.description = Security.STAMPINGS_CREATE;
			permission.save();
			role.permissions.add(permission);
			role.save();
		}
		
		//2) Creazione lettore badge di default da associare ad ogni ufficio
		User defaultBadgeReader = User.find("byUsername", "defaultBadgeReader").first();
		if(defaultBadgeReader == null){
			defaultBadgeReader = new User();
			defaultBadgeReader.username = "defaultBadgeReader";
			defaultBadgeReader.password = Codec.hexMD5("defaultBadgeReader");
			defaultBadgeReader.save();
		}
		
		//3) Ogni ufficio sede senza alcun lettore badge è associato a lettore badge default
		List<Office> officeList = Office.findAll();
		for(Office office : officeList) {
			
			if( !office.isSeat() )
				continue;
			
			boolean hasBadgeReader = false;
			
			for(UsersRolesOffices uro : office.usersRolesOffices) {
				if(uro.role.name.equals(Role.BADGE_READER)) {
					hasBadgeReader = true;
					break;
				}
			}
			if(hasBadgeReader) {
				continue;
			}
			
			UsersRolesOffices uro = new UsersRolesOffices();
			uro.user = defaultBadgeReader;
			uro.office = office;
			uro.role = role;
			uro.save();
		}
		
		
		//4)TEST PISA E COSENZA
		Office pisa = Office.find("byCode", 223400).first();
		Office cosenza = Office.find("byCode", 223410).first();
		User pisaBadge = User.find("byUsername", "pisaBadge").first();
		if(pisaBadge == null){
			pisaBadge = new User();
			pisaBadge.username = "pisaBadge";
			pisaBadge.password = Codec.hexMD5("pisaBadge");
			pisaBadge.save();
		}
		User cosenzaBadge = User.find("byUsername", "cosenzaBadge").first();
		if(cosenzaBadge == null){
			cosenzaBadge = new User();
			cosenzaBadge.username = "cosenzaBadge";
			cosenzaBadge.password = Codec.hexMD5("cosenzaBadge");
			cosenzaBadge.save();
		}
		
		for(UsersRolesOffices uro : pisa.usersRolesOffices) {
			if(uro.role.name.equals(Role.BADGE_READER)) {
				uro.delete();
			}
		}
		for(UsersRolesOffices uro : cosenza.usersRolesOffices) {
			if(uro.role.name.equals(Role.BADGE_READER)) {
				uro.delete();
			}
		}
		
		UsersRolesOffices uro = new UsersRolesOffices();
		uro.office = pisa;
		uro.user = pisaBadge;
		uro.role = role;
		uro.save();
		
		uro = new UsersRolesOffices();
		uro.office = cosenza;
		uro.user = cosenzaBadge;
		uro.role = role;
		uro.save();
		
		uro = new UsersRolesOffices();
		uro.office = pisa;
		uro.user = cosenzaBadge;
		uro.role = role;
		uro.save();
	}

	
	/**
	 * Crea il tipo di WorkingTimeTypeNormaleMod. Questo tipo non era presente nella vecchia applicazione
	 * che mostrava per default questo tipo di Orario quando la persona non aveva nessuno Orario impostato.
	 */
	/*
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
	*/
	
	
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

	/*
	private static void convertPersonBornDateHandler() {
		
		List<Person> personList = Person.findAll();
		for(Person person : personList) {
			
			if(person.bornDate != null) {
				person.birthday = new LocalDate(person.bornDate);
				person.bornDate = null;
				person.save();
			}
		}
	}
	*/
	
	/*
	private static void cleanOfficeTree() {
		
		//EVOLUZIONE IRPI
		
		Office irpi = Office.find("byCode", 3000).first();	//WARNING è un codice forse non univoco!!!
		if(irpi != null) {
			
			// Primo livello AREA
			Office areaTorino = Office.find("byName", "Area CNR Torino").first();
			if(areaTorino == null) {
				
				areaTorino = new Office();
				areaTorino.name = "Area CNR Torino";
				areaTorino.code = null;
				areaTorino.address = null;
				areaTorino.contraction = null;
				areaTorino.joiningDate = null;
				areaTorino.confGeneral = null;
				areaTorino.save();
				
			}
			
			//Secondo livello ISTITUTO
			if(areaTorino.subOffices.size() == 0) {
				
				Office istitutoIrpi = new Office();
				istitutoIrpi.name = "Istituto IRPI";
				istitutoIrpi.address = null;
				istitutoIrpi.code = null;
				istitutoIrpi.contraction = "IRPI";
				istitutoIrpi.joiningDate = null;
				istitutoIrpi.office = areaTorino;
				istitutoIrpi.confGeneral = null;
				istitutoIrpi.save();
			}
			
			//Terzo livello SEDE
			Office istitutoIrpi = Office.find("byName", "Istituto IRPI").first();
			if(istitutoIrpi.subOffices.size()  == 0) {

				Office irpiTorino = Office.find("byCode", 3000).first();
				irpiTorino.office = istitutoIrpi;
				irpiTorino.name = "IRPI - Torino";
				irpiTorino.save();
			}
			
			
		}
		
		//EVOLUZIONE IVV
		
		Office ivv = Office.find("byCode", 1000).first();	//WARNING è un codice forse non univoco!!!
		if(ivv != null) {
			
			// Primo livello AREA
			Office areaTorino = Office.find("byName", "Area CNR Torino").first();
			if(areaTorino == null) {
				
				areaTorino = new Office();
				areaTorino.name = "Area CNR Torino";
				areaTorino.code = null;
				areaTorino.address = null;
				areaTorino.contraction = null;
				areaTorino.joiningDate = null;
				areaTorino.confGeneral = null;
				areaTorino.save();
				
			}
			
			//Secondo livello ISTITUTO
			if(areaTorino.subOffices.size() == 0) {
				
				Office istitutoIvv = new Office();
				istitutoIvv.name = "Istituto IVV";
				istitutoIvv.address = null;
				istitutoIvv.code = null;
				istitutoIvv.contraction = "IVV";
				istitutoIvv.joiningDate = null;
				istitutoIvv.office = areaTorino;
				istitutoIvv.confGeneral = null;
				istitutoIvv.save();
			}
			
			//Terzo livello SEDE
			Office istitutoIvv = Office.find("byName", "Istituto IVV").first();
			if(istitutoIvv.subOffices.size()  == 0) {
				
				Office ivvTorino = Office.find("byCode", 1000).first();
			
				
				ivvTorino.office = istitutoIvv;
				ivvTorino.name = "IVV - Torino";
				ivvTorino.save();

			}
			
			
		}
		
		
		
		// EVOLUZIONE PISA
		Office iitPisa = Office.find("byCode", 223400).first();
		if(iitPisa != null) 
		{
			//Primo livello AREA
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
			
			//Secondo livello ISTITUTO
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
			
			//Terzo livello SEDE
			Office iit = Office.find("byName", "Istituto IIT").first();
			if(iit.subOffices.size()  == 0) {
				
				iitPisa = Office.find("byCode", 223400).first();
				Office iitCos = Office.find("byCode", 223410).first();
				
				iitPisa.office = iit;
				iitPisa.name = "IIT - Pisa";
				iitPisa.save();
				
				iitCos.office = iit;
				iitCos.save();
			}
		}
		
		// EVOLUZIONE ISE
		
		Office ise = Office.find("byCode", 2000).first();	//WARNING è un codice forse non univoco!!!
		if(ise != null) {
			
			//Primo livello AREA
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
			
			//Secondo livello ISTITUTO
			if(areaPisa.subOffices.size() == 0) {
				
				Office istitutoIse = new Office();
				istitutoIse.name = "Istituto ISE";
				istitutoIse.address = null;
				istitutoIse.code = null;
				istitutoIse.contraction = "ISE";
				istitutoIse.joiningDate = null;
				istitutoIse.office = areaPisa;
				istitutoIse.confGeneral = null;
				istitutoIse.save();
			}
			
			//Terzo livello SEDE
			Office istitutoIse = Office.find("byName", "Istituto ISE").first();
			if(istitutoIse.subOffices.size()  == 0) {
				Office isePisa = Office.find("byCode", 2000).first();
				isePisa.office = istitutoIse;
				isePisa.name = "ISE - Pisa";
				isePisa.save();
			}
		}

	}
	*/
	
	/*
	private static void bootstrapTotalOvertimeHandler() {
		
		// EVOLUZIONE IRPI 
		Office irpi = Office.find("byCode", 3000).first();	//WARNING è un codice forse non univoco!!!
		if(irpi != null) {
			
			//Associare tutti gli oggetti TotalOvertime a ivv
			List<TotalOvertime> totalOvertimes = TotalOvertime.findAll();
			for(TotalOvertime totalOvertime : totalOvertimes) {
				if(totalOvertime.office == null) {
					totalOvertime.office = irpi;
					totalOvertime.save();
				}
			}
			
		}
		
		// EVOLUZIONE IVV 
		Office ivv = Office.find("byCode", 1000).first();	//WARNING è un codice forse non univoco!!!
		if(ivv != null) {
			
			//Associare tutti gli oggetti TotalOvertime a ivv
			List<TotalOvertime> totalOvertimes = TotalOvertime.findAll();
			for(TotalOvertime totalOvertime : totalOvertimes) {
				if(totalOvertime.office == null) {
					totalOvertime.office = ivv;
					totalOvertime.save();
				}
			}
			
		}
		
		// EVOLUZIONE ISE 
		Office ise = Office.find("byCode", 2000).first();	//WARNING è un codice forse non univoco!!!
		if(ise != null) {
			
			//Associare tutti gli oggetti TotalOvertime a ivv
			List<TotalOvertime> totalOvertimes = TotalOvertime.findAll();
			for(TotalOvertime totalOvertime : totalOvertimes) {
				if(totalOvertime.office == null) {
					totalOvertime.office = ise;
					totalOvertime.save();
				}
			}
			
		}
		
		// EVOLUZIONE IIT 
		Office iit = Office.find("byCode", 223400).first();	
		if(iit != null) {
			
			//Associare tutti gli oggetti TotalOvertime privi di office a iit
			List<TotalOvertime> totalOvertimes = TotalOvertime.findAll();
			for(TotalOvertime totalOvertime : totalOvertimes) {
				if(totalOvertime.office == null) {
					totalOvertime.office = iit;
					totalOvertime.save();
				}
			}
			
		}
		
	}
	*/
	
	/*
	private static void createContractStampProfile2() {
		
		if(ContractStampProfile.count() != 0){
		
			return;
		}
		
		List<Contract> contractList = Contract.findAll();
		for(Contract contract : contractList) {
			
			DateInterval contractInterval = contract.getContractDateInterval();

			List<StampProfile> spInvolved = Lists.newArrayList();
			
			for(StampProfile sp : contract.person.stampProfiles) {
				
				DateInterval spInterval = new DateInterval(sp.startFrom, sp.endTo);
				
				//Calcolo l'intersezione fra stamp profile e contractInterval
				DateInterval intersection = DateUtility.intervalIntersection(contractInterval, spInterval);
				if(intersection != null) {
					spInvolved.add(sp);
				}
			}
			
			StampProfile definitivo;
			ContractStampProfile csp = new ContractStampProfile();
			
			csp.startFrom = contract.beginContract;
			csp.endTo = contract.expireContract;
			if(contract.endContract!=null)
				csp.endTo = contract.endContract;
			
			csp.contract = contract;
			
			csp.fixedworkingtime = false;
			
			if(spInvolved.size()>0) {
				definitivo = spInvolved.get(0);
				csp.fixedworkingtime = definitivo.fixedWorkingTime;
			}
			
			csp.save();
			
//			System.out.println(" ContractId: "+ contract.id 
//					+ " ContractBegin: "+ contract.beginContract 
//					+ " Involved: "+spInvolved.size()
//					+ " Persona: "+contract.person.surname + " " + contract.person.name);
			
			
		}

		
//		List<StampProfile> spList = StampProfile.findAll();
//		for(StampProfile sp : spList) {
//			
//			sp.delete();
//		}
	}
	*/
	
	/*
	private static void insertDefaultStampModificationType() {
		
		//FIX d 
		StampModificationType smt = StampModificationType.find("byCode", "d").first();
		if(smt == null) {
			
			smt = new StampModificationType();
			smt.code = "d";
			smt.description = "Considerato presente se non ci sono codici di assenza (orario di lavoro autodichiarato)";
			smt.save();
		}
		
		
	}
	*/
	
	
}
