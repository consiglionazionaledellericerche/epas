package jobs;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.io.Resources;

import dao.OfficeDao;
import dao.UserDao;

import lombok.extern.slf4j.Slf4j;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.PeriodManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;

import models.Office;
import models.Qualification;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.WorkingTimeType;
import models.enumerate.Parameter;

import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

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

import javax.inject.Inject;


/**
 * Carica nel database dell'applicazione i dati iniziali predefiniti nel caso questi non siano già
 * presenti.
 *
 * @author cristian
 */
@OnApplicationStart
@Slf4j
public class Bootstrap extends Job<Void> {

  private static final String JOBS_CONF = "jobs.active";

  @Inject
  static FixUserPermission fixUserPermission;
  @Inject
  static UserDao userDao;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static ConfYearManager confYearManager;
  @Inject
  static ConfGeneralManager confGeneralManager;
  @Inject
  static PeriodManager periodManager;

  /**
   * Procedura abilitata solo all'utente developer per compiere la migrazione alla nuova gestione
   * dei paraemtri di configurazione.
   */
  public static void migrateConfiguration() {

    //migrazione nuova configurazione
    List<Office> offices = officeDao.allOffices().list();
    for (Office office : offices) {

      if (!office.configurations.isEmpty()) {
        continue;
      }

      log.info("Inizio migrazione parametri generali {}", office.name);

      Integer day = confGeneralManager
          .getIntegerFieldValue(Parameter.DAY_OF_PATRON, office);
      Integer month = confGeneralManager
          .getIntegerFieldValue(Parameter.MONTH_OF_PATRON, office);

      configurationManager.updateDayMonth(EpasParam.DAY_OF_PATRON, office, day, month,
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);

      Boolean web = confGeneralManager
          .getBooleanFieldValue(Parameter.WEB_STAMPING_ALLOWED, office);

      configurationManager.updateBoolean(EpasParam.WEB_STAMPING_ALLOWED, office, web,
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);

      // Verificare il vecchio splitter e ad esempio rimuovere le quadre.
      List<String> ipList = Splitter.on("-")
          .splitToList(confGeneralManager.getFieldValue(Parameter.ADDRESSES_ALLOWED, office));
      configurationManager.updateIpList(EpasParam.ADDRESSES_ALLOWED, office, ipList,
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);

      Integer integer = confGeneralManager
          .getIntegerFieldValue(Parameter.NUMBER_OF_VIEWING_COUPLE, office);

      configurationManager.updateInteger(EpasParam.NUMBER_OF_VIEWING_COUPLE, office, integer,
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);

      Optional<LocalDate> mealTicket = confGeneralManager
          .getLocalDateFieldValue(Parameter.DATE_START_MEAL_TICKET, office);
      LocalDate date = new LocalDate(EpasParam.DATE_START_MEAL_TICKET.defaultValue);
      if (mealTicket.isPresent()) {
        date = mealTicket.get();
      }
      configurationManager.updateLocalDate(EpasParam.DATE_START_MEAL_TICKET, office, date,
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);

      Boolean sendEmail = confGeneralManager
          .getBooleanFieldValue(Parameter.SEND_EMAIL, office);

      configurationManager.updateBoolean(EpasParam.SEND_EMAIL, office, sendEmail,
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);

      String email = confGeneralManager.getFieldValue(Parameter.EMAIL_TO_CONTACT, office);

      configurationManager.updateEmail(EpasParam.EMAIL_TO_CONTACT, office, email,
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);

      Integer year = office.beginDate.getYear();
      while (year != null) {

        log.info("Inizio migrazione parametri annuali {} {}", year, office.name);

        day = confYearManager
            .getIntegerFieldValue(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, year);
        month = confYearManager
            .getIntegerFieldValue(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, year);
        configurationManager.updateYearlyDayMonth(EpasParam.EXPIRY_VACATION_PAST_YEAR, office,
            day, month, year, true, true);

        month = confYearManager
            .getIntegerFieldValue(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13, office, year);
        configurationManager.updateYearlyMonth(EpasParam.MONTH_EXPIRY_RECOVERY_DAYS_13, office,
            month, year, true, true);

        month = confYearManager
            .getIntegerFieldValue(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49, office, year);
        configurationManager.updateYearlyMonth(EpasParam.MONTH_EXPIRY_RECOVERY_DAYS_49, office,
            month, year, true, true);

        integer = confYearManager
            .getIntegerFieldValue(Parameter.MAX_RECOVERY_DAYS_13, office, year);
        configurationManager.updateYearlyInteger(EpasParam.MAX_RECOVERY_DAYS_13, office,
            integer, year, true, true);

        integer = confYearManager
            .getIntegerFieldValue(Parameter.MAX_RECOVERY_DAYS_49, office, year);
        configurationManager.updateYearlyInteger(EpasParam.MAX_RECOVERY_DAYS_49, office,
            integer, year, true, true);


        if ((year == LocalDate.now().getYear())
            || (office.calculatedEnd() != null && year == office.calculatedEnd().getYear())) {
          year = null;
        } else {
          year++;
        }
      }

      log.info("Inizio migrazione parametri periodici {}", office.name);

      String hour = confYearManager
          .getFieldValue(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, office,
              LocalDate.now().getYear());

      if (hour.split(":").length == 1) {
        configurationManager.updateLocalTime(EpasParam.HOUR_MAX_TO_CALCULATE_WORKTIME, office,
            new LocalTime(new Integer(hour.split(":")[0]), 0),
            Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);
      } else {
        configurationManager.updateLocalTime(EpasParam.HOUR_MAX_TO_CALCULATE_WORKTIME, office,
            new LocalTime(new Integer(hour.split(":")[0]), new Integer(hour.split(":")[1])),
            Optional.of(office.beginDate), Optional.fromNullable(office.endDate), true);
      }


      Integer mealTimeStartHour = confGeneralManager
          .getIntegerFieldValue(Parameter.MEAL_TIME_START_HOUR, office);
      Integer mealTimeStartMinute = confGeneralManager
          .getIntegerFieldValue(Parameter.MEAL_TIME_START_MINUTE, office);
      Integer mealTimeEndHour = confGeneralManager
          .getIntegerFieldValue(Parameter.MEAL_TIME_END_HOUR, office);
      Integer mealTimeEndMinute = confGeneralManager
          .getIntegerFieldValue(Parameter.MEAL_TIME_END_MINUTE, office);
      LocalTime startLunch = new LocalTime()
          .withHourOfDay(mealTimeStartHour)
          .withMinuteOfHour(mealTimeStartMinute);
      LocalTime endLunch = new LocalTime()
          .withHourOfDay(mealTimeEndHour)
          .withMinuteOfHour(mealTimeEndMinute);

      configurationManager.updateLocalTimeInterval(EpasParam.LUNCH_INTERVAL, office,
          startLunch, endLunch, Optional.of(office.beginDate),
          Optional.fromNullable(office.endDate), true);

      log.info("Migrazione configurazione {} terminata!!!", office.name);
    }

    //hotfix dei periodi
    for (Office office : offices) {
      log.info("Hotfix dei periodi di configurazione {}.", office.name);
      periodManager.updatePropertiesInPeriodOwner(office);
    }
    log.info("Hotfix dei periodi di configurazione terminata!!!");
  }

  public void doJob() throws IOException {

    if (Play.runingInTestMode()) {
      log.info("Application in test mode, default boostrap job not started");
      return;
    }

    // in modo da inibire l'esecuzione dei job in base alla configurazione
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

    // Allinea tutte le sequenze del db
    Fixtures.executeSQL(Play.getFile("db/import/fix_sequences.sql"));

    fixUserPermission.doJob();

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

    // La migrateConfiguration và rimossa (e con lei anche tabelle e compagnia inerenti la vecchia 
    // gestione delle configurazioni) appena verrà effettuato l'aggiornamento dell'ise.
    migrateConfiguration();
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
