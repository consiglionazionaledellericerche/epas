/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jobs;

import dao.UserDao;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Institute;
import models.Qualification;
import models.WorkingTimeType;
import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.jdbc.Work;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;


/**
 * Carica nel database dell'applicazione i dati iniziali predefiniti nel caso questi non siano già
 * presenti.
 *
 * @author Cristian Lucchesi
 */
@OnApplicationStart
@Slf4j
public class Bootstrap extends Job<Void> {

  static final String JOBS_CONF = "jobs.active";

  @Inject
  static UserDao userDao;
  
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

    //Crea un utente admin con il ruolo di developer se non presente.
    //Utile per il primo setup dell'applicazione.
    if (userDao.getUsersWithRoleDeveloper().isEmpty()) {
      Fixtures.loadModels("../db/import/developer.yml");
    }

    if (Institute.count() == 0) {
      Fixtures.loadModels("../db/import/fakeInstituteAndOffice.yml");
    }

    if (Qualification.count() == 0) {
      Fixtures.loadModels("../db/import/qualifications.yml");
    }

    //impostare il campo tipo orario orizzontale si/no effettuando una euristica
    List<WorkingTimeType> wttList = WorkingTimeType.findAll();
    for (WorkingTimeType wtt : wttList) {

      if (wtt.getHorizontal() == null) {
        wtt.setHorizontal(wtt.horizontalEuristic());
        wtt.save();
      }
    }

  }

  /**
   * Classe di utilità.
   *
   * @author dario
   *
   */
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
