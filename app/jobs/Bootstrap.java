package jobs;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import com.google.common.base.Optional;
import com.google.common.io.Resources;

import dao.UserDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import models.Contract;
import models.Person;
import models.Qualification;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.WorkingTimeType;
import play.Play;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;


/**
 * Carica nel database dell'applicazione i dati iniziali predefiniti nel caso questi non siano già
 * presenti
 *
 * @author cristian
 */
@OnApplicationStart
@Slf4j
public class Bootstrap extends Job<Void> {

  private final static String JOBS_CONF = "jobs.active";

  @Inject
  static FixUserPermission fixUserPermission;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static UserDao userDao;

  public void doJob() throws IOException {

    if (Play.runingInTestMode()) {
      log.info("Application in test mode, default boostrap job not started");
      return;
    }

//		in modo da inibire l'esecuzione dei job in base alla configurazione
    if ("false".equals(Play.configuration.getProperty(JOBS_CONF))) {
      log.info("Bootstrap Interrotto. Disattivato dalla configurazione.");
      return;
    }

    Session session = (Session) JPA.em().getDelegate();

    if (Qualification.count() == 0) {

      //qualification absenceType absenceTypeQualification absenceTypeGroup
      session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
              .getResource("../db/import/absence-type-and-qualification-phase1.xml")));

      session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
              .getResource("../db/import/absence-type-and-qualification-phase2.xml")));
    }

    if (User.find("byUsername", "developer").fetch().isEmpty()) {
      Fixtures.loadModels("../db/import/developer.yml");
    }

//		Allinea tutte le sequenze del db
    Fixtures.executeSQL(Play.getFile("db/import/fix_sequences.sql"));

    fixUserPermission.doJob();

    //prendere tutte le persone che a oggi non hanno inizializzazione e crearne una vuota
    //Tutte le persone con contratto iniziato dopo alla data di inizializzazione
    // devono avere la inizializzazione al giorno prima.
    List<Person> persons = Person.findAll();
    for (Person person : persons) {

      //Contratto attuale
      Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
      if (!contract.isPresent()) {
        continue;
      }

      IWrapperContract wrContract = wrapperFactory.create(contract.get());
      if (wrContract.initializationMissing()) {

        log.info("Bootstrap contract scan: il contratto di {} iniziato il {} non è initializationMissing",
                person.fullName(), contract.get().beginDate);
                /*
                Contract c = contract.get();
				c.sourceDateResidual = new LocalDate(wrContract.dateForInitialization());
				c.sourcePermissionUsed = 0;
				c.sourceRecoveryDayUsed = 0;
				c.sourceRemainingMealTicket = 0;
				c.sourceRemainingMinutesCurrentYear = 0;
				c.sourceRemainingMinutesLastYear = 0;
				c.sourceVacationCurrentYearUsed = 0;
				c.sourceVacationLastYearUsed = 0;
				c.sourceByAdmin = false;
				c.save();

				consistencyManager.updatePersonSituation(person.id, c.sourceDateResidual);
				*/
      }
    }

    //impostare il campo tipo orario orizzondale si/no effettuando una euristica
    List<WorkingTimeType> wttList = WorkingTimeType.findAll();
    for (WorkingTimeType wtt : wttList) {

      if (wtt.horizontal == null) {
        wtt.horizontal = wtt.horizontalEuristic();
        wtt.save();
      }
    }

    //L'utente admin non deve disporre del ruolo di amminstratore del personale. FIX
    User user = userDao.byUsername("admin");
    if (user != null) {
      for (UsersRolesOffices uro : user.usersRolesOffices) {
        if (uro.role.name.equals(Role.PERSONNEL_ADMIN)
                || uro.role.name.equals(Role.PERSONNEL_ADMIN_MINI)) {
          uro.delete();
        }
      }
    } else {
      //BOH
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
      } catch (DataSetException e) {
        e.printStackTrace();
      } catch (DatabaseUnitException e) {
        e.printStackTrace();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
