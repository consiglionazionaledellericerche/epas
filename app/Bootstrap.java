
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

import models.Office;
import models.Permission;
import models.Person;
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
import org.joda.time.LocalDate;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.libs.Codec;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import controllers.Security;
import dao.OfficeDao;
import dao.PersonDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

/**
 * Carica nel database dell'applicazione i dati iniziali predefiniti nel caso questi non siano già presenti
 *
 * @author cristian
 *
 */
@OnApplicationStart
public class Bootstrap extends Job<Void> {

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static UsersRolesOfficesDao usersRolesOfficesDao;
	@Inject
	private static UserDao userDao;
	@Inject
	private static IWrapperFactory wrapperFactory;

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

	public void doJob() throws IOException {

		if (Play.id.equals("test")) {
			Logger.info("Application in test mode, default boostrap job not started");
			return;
		}

		Session session = (Session) JPA.em().getDelegate();

		if(Qualification.count() == 0 ) {
			
			//qualification absenceType absenceTypeQualification absenceTypeGroup
			session.doWork(new DatasetImport(DatabaseOperation.INSERT,Resources
					.getResource("../db/import/absence-type-and-qualification-phase1.xml")));

			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/absence-type-and-qualification-phase2.xml")));

			//competenceCode
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/competence-codes.xml")));

			//stampModificationType
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/stamp-modification-types.xml")));

			//stampType
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/stamp-types.xml")));

			//vacationCode
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/vacation-codes.xml")));

			//workingTimeType workingTimeTypeDay
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/working-time-types.xml")));

			// History
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class,
					"../db/import/history/part1_history.xml")));
			// History
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class,
					"../db/import/history/part2_history.xml")));
			
			
						
			
			// riallinea le sequenze con i valori presenti sul db.
			for (String sql : Files.readLines(new File(Play.applicationPath,
					"db/import/fix_sequences.sql"), Charsets.UTF_8)) {
				JPA.em().createNativeQuery(sql).getSingleResult();
			}
		}

		if(User.count() == 0) {
			User admin = new User();
			admin.username = "admin";
			admin.password = Codec.hexMD5("personnelEpasNewVersion");
			admin.save();
		}

		bootstrapEmployeeRoleCreation();

		bootstrapStampingCreateHandler();

		bootstrapPermissionsHandler();

		bootstrapSuperAdminCreation();

		restUsersCreation();

	}

	/**
	 * Crea il ruolo Employee che deve essere associato al dipendente nell'ufficio di appartenenza
	 */
	private static void bootstrapEmployeeRoleCreation() {

		if (Office.count() == 0 || Person.count() == 0)
			return;

		Role role = Role.find("byName",  Role.EMPLOYEE).first();
		if(role == null) {
			role = new Role();
			role.name = Role.EMPLOYEE;
			role.save();

			Permission permission = Permission.find("byDescription", Security.EMPLOYEE).first();
			if(permission == null) {
				permission = new Permission();
				permission.description = Security.EMPLOYEE;
				permission.save();
			}

			role.permissions.add(permission);
			role.save();
		}

		//Creo il ruolo per i dipendenti se non esiste
		List<Person> personList = personDao.list(Optional.<String>absent(),
				Sets.newHashSet(officeDao.getAllOffices()), false, LocalDate.now(),
				LocalDate.now(), false).list();
		for(Person person : personList) {

			boolean exist = false;
			//Cerco se esiste già e controllo che sia relativo all'office di appartentenza

			for(UsersRolesOffices uro : person.user.usersRolesOffices ) {
				//Rimuovo ruolo role se non appartiene più all'office
				if(uro.role.name.equals(role.name)){
					if(uro.office.code.equals(person.office.code)) {
						exist = true;
					}
					else {
						uro.delete();
					}
				}
			}

			if(!exist) {
				UsersRolesOffices uro = new UsersRolesOffices();
				uro.user = person.user;
				uro.office = person.office;
				uro.role = role;
				uro.save();
			}
		}
	}

	/**
	 * Crea il ruolo SuperAdmin con il permesso Developer.
	 * Per adesso viene assegnato ad ogni sede.
	 */
	private static void bootstrapSuperAdminCreation() {

		//1) Creazione Ruolo con permesso
		Role role = Role.find("byName",  Role.SUPER_ADMIN).first();
		if(role == null) {
			role = new Role();
			role.name = Role.SUPER_ADMIN;
			role.save();
			Permission permission = new Permission();
			permission.description = Security.DEVELOPER;
			permission.save();
			role.permissions.add(permission);
			role.save();
		}

		User admin = User.find("byUsername", "admin").first();

		//2) Assegno il permesso ad admin ad ogni office
		List<Office> officeList = Office.findAll();
		for(Office office : officeList) {

			IWrapperOffice wOffice = wrapperFactory.create(office);

			if( !wOffice.isSeat() )
				continue;

			boolean hasSuperAdminRole = false;

			for(UsersRolesOffices uro : office.usersRolesOffices) {
				if(uro.role.name.equals(Role.SUPER_ADMIN)) {
					hasSuperAdminRole = true;
					break;
				}
			}
			if(hasSuperAdminRole) {
				continue;
			}

			UsersRolesOffices uro = new UsersRolesOffices();
			uro.user = admin;
			uro.office = office;
			uro.role = role;
			uro.save();
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

			IWrapperOffice wOffice = wrapperFactory.create(office);

			if( !wOffice.isSeat() )
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


		//		//4)TEST PISA E COSENZA
		//		Office pisa = Office.find("byCode", 223400).first();
		//		Office cosenza = Office.find("byCode", 223410).first();
		//		User pisaBadge = User.find("byUsername", "pisaBadge").first();
		//		if(pisaBadge == null){
		//			pisaBadge = new User();
		//			pisaBadge.username = "pisaBadge";
		//			pisaBadge.password = Codec.hexMD5("pisaBadge");
		//			pisaBadge.save();
		//		}
		//		User cosenzaBadge = User.find("byUsername", "cosenzaBadge").first();
		//		if(cosenzaBadge == null){
		//			cosenzaBadge = new User();
		//			cosenzaBadge.username = "cosenzaBadge";
		//			cosenzaBadge.password = Codec.hexMD5("cosenzaBadge");
		//			cosenzaBadge.save();
		//		}
		//
		//		for(UsersRolesOffices uro : pisa.usersRolesOffices) {
		//			if(uro.role.name.equals(Role.BADGE_READER)) {
		//				uro.delete();
		//			}
		//		}
		//		for(UsersRolesOffices uro : cosenza.usersRolesOffices) {
		//			if(uro.role.name.equals(Role.BADGE_READER)) {
		//				uro.delete();
		//			}
		//		}
		//
		//		UsersRolesOffices uro = new UsersRolesOffices();
		//		uro.office = pisa;
		//		uro.user = pisaBadge;
		//		uro.role = role;
		//		uro.save();
		//
		//		uro = new UsersRolesOffices();
		//		uro.office = cosenza;
		//		uro.user = cosenzaBadge;
		//		uro.role = role;
		//		uro.save();
		//
		//		uro = new UsersRolesOffices();
		//		uro.office = pisa;
		//		uro.user = cosenzaBadge;
		//		uro.role = role;
		//		uro.save();
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

	private void restUsersCreation(){

		Role restRole = Role.find("byName", Role.REST_CLIENT).first();
		if(restRole == null){
			restRole = new Role();
			restRole.name = Role.REST_CLIENT;
			restRole.save();
			Permission permission = new Permission();
			permission.description = Security.REST;
			permission.save();
			restRole.permissions.add(permission);
			restRole.save();
		}

		String protimeUser = Play.configuration.getProperty("rest.protime.user");
		User protime = userDao.getUserByUsernameAndPassword(protimeUser, Optional.<String>absent());

		if(protime == null){
			protime = new User();
			protime.username = protimeUser;
			protime.password = Codec.hexMD5(Play.configuration.getProperty("rest.protime.password"));
			protime.save();
		}

		List<Office> areas = officeDao.getAreas();

		for(Office area : areas){
			if(!usersRolesOfficesDao.getUsersRolesOffices(protime, restRole, area).isPresent()){
				UsersRolesOffices uro = new UsersRolesOffices();
				uro.user = protime;
				uro.office = area;
				uro.role = restRole;
				uro.save();
			}
		}
	}

}
