package controllers;


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.ConfYearDao;
import dao.OfficeDao;

import it.cnr.iit.epas.DateUtility;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.ConfYearManager.MessageResult;
import manager.SecureManager;

import models.ConfGeneral;
import models.ConfYear;
import models.Office;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.Set;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class Configurations extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecureManager secureManager;
  @Inject
  private static ConfGeneralManager confGeneralManager;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static ConfYearManager confYearManager;
  @Inject
  private static ConfYearDao confYearDao;

  /**
   * Visualizza la pagina di configurazione generale dell'office.
   */
  public static void showConfGeneral(Long officeId) {

    Office office = null;

    Set<Office> offices = secureManager.officesSystemAdminAllowed(Security.getUser().get());
    if (officeId != null) {
      office = officeDao.getOfficeById(officeId);
    } else {
      //TODO se offices è vuota capire come comportarsi
      office = offices.iterator().next();
    }

    ConfGeneral initUseProgram = confGeneralManager
        .getConfGeneral(Parameter.INIT_USE_PROGRAM, office);

    ConfGeneral dayOfPatron = confGeneralManager
        .getConfGeneral(Parameter.DAY_OF_PATRON, office);
    ConfGeneral monthOfPatron = confGeneralManager
        .getConfGeneral(Parameter.MONTH_OF_PATRON, office);

    ConfGeneral webStampingAllowed = confGeneralManager
        .getConfGeneral(Parameter.WEB_STAMPING_ALLOWED, office);
    ConfGeneral addressesAllowed = confGeneralManager
        .getConfGeneral(Parameter.ADDRESSES_ALLOWED, office);

    ConfGeneral urlToPresence = confGeneralManager
        .getConfGeneral(Parameter.URL_TO_PRESENCE, office);
    ConfGeneral userToPresence = confGeneralManager
        .getConfGeneral(Parameter.USER_TO_PRESENCE, office);
    ConfGeneral passwordToPresence = confGeneralManager
        .getConfGeneral(Parameter.PASSWORD_TO_PRESENCE, office);

    ConfGeneral numberOfViewingCouple = confGeneralManager
        .getConfGeneral(Parameter.NUMBER_OF_VIEWING_COUPLE, office);

    ConfGeneral dateStartMealTicket = confGeneralManager
        .getConfGeneral(Parameter.DATE_START_MEAL_TICKET, office);
    ConfGeneral sendEmail = confGeneralManager.getConfGeneral(Parameter.SEND_EMAIL, office);

    render(initUseProgram, dayOfPatron, monthOfPatron, webStampingAllowed, 
        addressesAllowed, urlToPresence, userToPresence,
            passwordToPresence, numberOfViewingCouple, dateStartMealTicket, 
            sendEmail, offices, office);

  }

  /**
   * Salva il nuovo valore per il field name. (Chiamata via ajax tramite X-editable)
   */
  public static void saveConfGeneral(Long pk, String value) {

    ConfGeneral conf = confGeneralManager.getById(pk).orNull();

    if (conf != null) {
      rules.checkIfPermitted(conf.office);

      Parameter param = Parameter.getByDescription(conf.field);
      confGeneralManager.saveConfGeneral(param, conf.office, Optional.fromNullable(value));
    }
  }

  public static void savePatron(Long pk, String value) {

    Office office = officeDao.getOfficeById(pk);

    rules.checkIfPermitted(office);

    LocalDate dayMonth = DateUtility.dayMonth(value, Optional.<String>absent());

    confGeneralManager.saveConfGeneral(Parameter.DAY_OF_PATRON, office,
            Optional.fromNullable(dayMonth.dayOfMonth().getAsString()));

    confGeneralManager.saveConfGeneral(Parameter.MONTH_OF_PATRON, office,
            Optional.fromNullable(dayMonth.monthOfYear().getAsString()));
  }

  /**
   * Visualizza la pagina di configurazione annuale dell'office.
   */
  public static void showConfYear(Long officeId) {

    Office office = null;
    Set<Office> offices = secureManager.officesSystemAdminAllowed(Security.getUser().get());
    if (officeId != null) {
      office = officeDao.getOfficeById(officeId);
    } else {
      office = offices.iterator().next();
    }

    Integer currentYear = LocalDate.now().getYear();
    Integer previousYear = currentYear - 1;

    //Parametri configurazione anno passato
    ConfYear lastYearDayExpiryVacationPastYear = confYearManager.getByField(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, previousYear);
    ConfYear lastYearMonthExpiryVacationPastYear = confYearManager.getByField(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, previousYear);
    ConfYear lastYearMonthExpireRecoveryDaysOneThree = confYearManager.getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13, office, previousYear);
    ConfYear lastYearMonthExpireRecoveryDaysFourNine = confYearManager.getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49, office, previousYear);
    ConfYear lastYearMaxRecoveryDaysOneThree = confYearManager.getByField(Parameter.MAX_RECOVERY_DAYS_13, office, previousYear);
    ConfYear lastYearMaxRecoveryDaysFourNine = confYearManager.getByField(Parameter.MAX_RECOVERY_DAYS_49, office, previousYear);
    ConfYear lastYearHourMaxToCalculateWorkTime = confYearManager.getByField(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, office, previousYear);

    //Parametri configurazione anno corrente
    ConfYear dayExpiryVacationPastYear = confYearManager.getByField(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, currentYear);
    ConfYear monthExpiryVacationPastYear = confYearManager.getByField(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, currentYear);
    ConfYear monthExpireRecoveryDaysOneThree = confYearManager.getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13, office, currentYear);
    ConfYear monthExpireRecoveryDaysFourNine = confYearManager.getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49, office, currentYear);
    ConfYear maxRecoveryDaysOneThree = confYearManager.getByField(Parameter.MAX_RECOVERY_DAYS_13, office, currentYear);
    ConfYear maxRecoveryDaysFourNine = confYearManager.getByField(Parameter.MAX_RECOVERY_DAYS_49, office, currentYear);
    ConfYear hourMaxToCalculateWorkTime = confYearManager.getByField(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, office, currentYear);

    render(currentYear, previousYear, lastYearDayExpiryVacationPastYear, lastYearMonthExpiryVacationPastYear, lastYearMonthExpireRecoveryDaysOneThree,
            lastYearMonthExpireRecoveryDaysFourNine, lastYearMaxRecoveryDaysOneThree, lastYearMaxRecoveryDaysFourNine,
            lastYearHourMaxToCalculateWorkTime, dayExpiryVacationPastYear, monthExpiryVacationPastYear,
            monthExpireRecoveryDaysOneThree, monthExpireRecoveryDaysFourNine, monthExpireRecoveryDaysFourNine, maxRecoveryDaysOneThree,
            maxRecoveryDaysFourNine, hourMaxToCalculateWorkTime, offices, office);

  }

  /**
   * Salva il nuovo valore per il field name. (Chiamata via ajax tramite X-editable)
   */
  public static void saveConfYear(String pk, String value) {

    ConfYear conf = confYearDao.getById(Long.parseLong(pk));

    Preconditions.checkNotNull(conf);

    MessageResult message = confYearManager.persistConfYear(conf, value);

    if (message.result == false) {
      response.status = 500;
      renderText(message.message);
    }
  }

  //	FIXME al momento uso il campo name passato dall'x-editable per specificare l'anno da cambiare
//  Ma quando il giorno e mese saranno in un unico parametro, conviene passare direttamente l'id del confyear
  public static void savePastYearVacationLimit(Long pk, String value, String name) {

    Office office = officeDao.getOfficeById(pk);

    rules.checkIfPermitted(office);

    Integer year = Integer.parseInt(name);

    LocalDate dayMonth = DateUtility.dayMonth(value, Optional.<String>absent());

    confYearManager.saveConfYear(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR,
            office, year, Optional.of(dayMonth.dayOfMonth().getAsString()));

    confYearManager.saveConfYear(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR,
            office, year, Optional.of(dayMonth.monthOfYear().getAsString()));

  }

  public static void showConfPeriod(Long officeId) {

    Office office = null;

    Set<Office> offices = secureManager.officesSystemAdminAllowed(Security.getUser().get());
    if (officeId != null) {
      office = officeDao.getOfficeById(officeId);
    } else {
      //TODO se offices è vuota capire come comportarsi
      office = offices.iterator().next();
    }

    render(office, offices);
  }

}
