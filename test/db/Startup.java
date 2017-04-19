package db;

import com.google.common.io.Resources;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;

import manager.services.absences.AbsenceService;

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

/**
 * Job di creazione del db per i test.
 * 
 * @author cristian
 *
 */
@OnApplicationStart
public class Startup extends Job<Void> {

  @Inject
  static AbsenceService absenceService;
  
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
        IDataSet dataSet = new FlatXmlDataSetBuilder()
            .setColumnSensing(true).build(url);
        operation.execute(new H2Connection(connection, ""), dataSet);
      } catch (DataSetException dse) {
        dse.printStackTrace();
      } catch (DatabaseUnitException due) {
        due.printStackTrace();
      } catch (SQLException sqle) {
        sqle.printStackTrace();
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

    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(
                Startup.class, "data/qualifications.xml")));

    //competenceCode
    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(Startup.class, "data/competence-codes.xml")));
    //competenceCode
    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(Startup.class, "data/stamp-modification-types.xml")));

    //competenceCode
    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(Startup.class, "data/roles.xml")));

    //office
    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(Startup.class, "data/office-with-deps.xml")));

    //workingTimeType workingTimeTypeDay
    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(Startup.class, "data/working-time-types.xml")));

    //lucchesi slim 2016-04
    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(Startup.class, "data/lucchesi-login-logout.xml")));
  }
}
