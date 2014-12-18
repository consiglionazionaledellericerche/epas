
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
	
	
}
