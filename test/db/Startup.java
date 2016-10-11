package db;

import com.google.common.io.Resources;

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

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

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

    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(Startup.class, "data/absence-type-and-qualification-phase1.xml")));
    session.doWork(
        new DatasetImport(
            DatabaseOperation.INSERT,
            Resources.getResource(Startup.class, "data/absence-type-and-qualification-phase2.xml")));

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
            Resources.getResource(Startup.class, "data/lucchesi-situation-2016-04.xml")));
//
//    //santerini slim 2014-03
//    session.doWork(
//        new DatasetImport(
//            DatabaseOperation.INSERT,
//            Resources.getResource(Startup.class, "santerini-situation-slim-2014-03.xml")));
//
//    //martinelli slim 2014-03
//    session.doWork(
//        new DatasetImport(
//            DatabaseOperation.INSERT,
//            Resources.getResource(Startup.class, "martinelli-situation-slim-2014-03.xml")));
//
//    //succurro slim 2014-03
//    session.doWork(
//        new DatasetImport(
//            DatabaseOperation.INSERT,
//            Resources.getResource(Startup.class, "succurro-situation-slim-2014-03.xml")));
//
//    //abba slim 2014-03
//    session.doWork(
//        new DatasetImport(
//            DatabaseOperation.INSERT,
//            Resources.getResource(Startup.class, "abba-situation-slim-2014-03.xml")));

  }
}
