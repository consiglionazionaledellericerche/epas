package db;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import play.Play;
import play.Play.Mode;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import com.google.common.io.Resources;

/**
 * @author cristian
 *
 */
@OnApplicationStart
public class Startup extends Job<Void> {
	
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

	@Override
	public void doJob() {
		if (Play.mode != Mode.DEV) {
			// come assicurazione del fatto che è nei test.
			return;
		}
		
		Session session = (Session) JPA.em().getDelegate();
		session.doWork(new DatasetImport(DatabaseOperation.INSERT, 
				Resources.getResource(Startup.class, "fulldataset-2014-03-phase1.xml")));		
		session.doWork(new DatasetImport(DatabaseOperation.INSERT, 
				Resources.getResource(Startup.class, "fulldataset-2014-03.xml")));
	}
}
