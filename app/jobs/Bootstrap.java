package jobs;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import models.CompetenceCode;
import models.Qualification;
import models.Role;
import models.StampModificationType;
import models.StampType;
import models.User;
import models.VacationCode;

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
import play.test.Fixtures;

import com.google.common.io.Resources;

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

			//workingTimeType workingTimeTypeDay
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/working-time-types.xml")));

//			// History
//			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class,
//					"../db/import/history/part1_history.xml")));
//			// History
//			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class,
//					"../db/import/history/part2_history.xml")));

		}
		
		if(User.find("byUsername", "developer").fetch().isEmpty()) {
			Fixtures.loadModels("../db/import/developer.yml");
		}
		if(Role.count() == 0){
			Fixtures.loadModels("../db/import/rolesAndPermission.yml");
		}
		if(VacationCode.count() == 0){
			Fixtures.loadModels("../db/import/vacationCode.yml");
		}
		if(StampType.count() == 0){
			Fixtures.loadModels("../db/import/stampType.yml");
		}
		if(StampModificationType.count() == 0){
			Fixtures.loadModels("../db/import/stampModificationType.yml");
		}
		if(CompetenceCode.count() == 0){
			Fixtures.loadModels("../db/import/competenceCode.yml");
		}
		
//		Allinea tutte le sequenze del db
		Fixtures.executeSQL(Play.getFile("db/import/fix_sequences.sql"));
		
		new FixUserPermission().now();
	}

}
