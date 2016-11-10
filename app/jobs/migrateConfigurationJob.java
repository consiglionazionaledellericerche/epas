package jobs;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;

import dao.OfficeDao;

import lombok.extern.slf4j.Slf4j;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.PeriodManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;

import models.Office;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.Play;
import play.jobs.Job;

import java.util.List;

import javax.inject.Inject;

/**
 * @author daniele
 * @since 14/07/16.
 */
@Slf4j
public class migrateConfigurationJob extends Job<Void> {

  // La migrateConfiguration và rimossa (e con lei anche tabelle e compagnia inerenti la vecchia
  // gestione delle configurazioni) appena verrà effettuato l'aggiornamento dell'ise.

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

  @Override
  public void doJob() throws Exception {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

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
}
