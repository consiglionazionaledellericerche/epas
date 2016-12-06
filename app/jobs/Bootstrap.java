package jobs;

import com.google.common.io.Resources;

import lombok.extern.slf4j.Slf4j;

import models.Qualification;
import models.User;
import models.WorkingTimeType;

import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import play.Play;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


/**
 * Carica nel database dell'applicazione i dati iniziali predefiniti nel caso questi non siano già
 * presenti.
 *
 * @author cristian
 */
@OnApplicationStart
@Slf4j
public class Bootstrap extends Job<Void> {

  static final String JOBS_CONF = "jobs.active";

  //Aggiunto qui perché non più presente nella classe Play dalla versione >= 1.4.3
  public static boolean runingInTestMode() {
    return Play.id.matches("test|test-?.*");
  }

  @Override
  public void doJob() throws IOException {

    if (runingInTestMode()) {
      log.info("Application in test mode, default boostrap job not started");
      return;
    }

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    Session session = (Session) JPA.em().getDelegate();

    if (Qualification.count() == 0) {

      session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
          .getResource("../db/import/absence-type-and-qualification-phase1.xml")));

      session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
          .getResource("../db/import/absence-type-and-qualification-phase2.xml")));
    }
    
    log.info("Conclusa migrazione assenze!");

    if (User.find("byUsername", "developer").fetch().isEmpty()) {
      Fixtures.loadModels("../db/import/developer.yml");
    }

    // Allinea tutte le sequenze del db
    Fixtures.executeSQL(Play.getFile("db/import/fix_sequences.sql"));

    //impostare il campo tipo orario orizzondale si/no effettuando una euristica
    List<WorkingTimeType> wttList = WorkingTimeType.findAll();
    for (WorkingTimeType wtt : wttList) {

      if (wtt.horizontal == null) {
        wtt.horizontal = wtt.horizontalEuristic();
        wtt.save();
      }
    }

  }

  public static class DatasetImport implements Work {

    private final URL url;
    private DatabaseOperation operation;

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
      } catch (DataSetException dse) {
        dse.printStackTrace();
      } catch (DatabaseUnitException due) {
        due.printStackTrace();
      } catch (SQLException sqle) {
        sqle.printStackTrace();
      }
    }
  }
}
