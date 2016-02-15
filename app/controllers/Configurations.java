package controllers;


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import dao.OfficeDao;

import it.cnr.iit.epas.DateUtility;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.ConfYearManager.MessageResult;
import manager.recaps.recomputation.RecomputeRecap;
import manager.ConfigurationManager;
import manager.PeriodManager;
import manager.SecureManager;

import models.ConfGeneral;
import models.ConfYear;
import models.Configuration;
import models.Office;
import models.base.IPropertyInPeriod;
import models.enumerate.Parameter;
import models.enumerate.EpasParam.EpasParamValueType;
import models.enumerate.EpasParam.EpasParamValueType.DayMonth;
import models.enumerate.EpasParam.EpasParamValueType.IpList;
import models.enumerate.EpasParam.EpasParamValueType.LocalTimeInterval;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
@Slf4j
public class Configurations extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecureManager secureManager;
  @Inject
  private static ConfGeneralManager confGeneralManager;
  @Inject
  private static ConfYearManager confYearManager;
  
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static PeriodManager periodManager;
  @Inject
  private static SecurityRules rules;

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

  /**
   * metodo che salva il giorno/mese del santo patrono in configurazione.
   * @param pk chiave della riga da modificare
   * @param value valore da assegnare
   */
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
    ConfYear lastYearDayExpiryVacationPastYear = confYearManager
        .getByField(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, previousYear);
    ConfYear lastYearMonthExpiryVacationPastYear = confYearManager
        .getByField(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, previousYear);
    ConfYear lastYearMonthExpireRecoveryDaysOneThree = confYearManager
        .getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13, office, previousYear);
    ConfYear lastYearMonthExpireRecoveryDaysFourNine = confYearManager
        .getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49, office, previousYear);
    ConfYear lastYearMaxRecoveryDaysOneThree = confYearManager
        .getByField(Parameter.MAX_RECOVERY_DAYS_13, office, previousYear);
    ConfYear lastYearMaxRecoveryDaysFourNine = confYearManager
        .getByField(Parameter.MAX_RECOVERY_DAYS_49, office, previousYear);
    ConfYear lastYearHourMaxToCalculateWorkTime = confYearManager
        .getByField(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, office, previousYear);

    //Parametri configurazione anno corrente
    ConfYear dayExpiryVacationPastYear = confYearManager
        .getByField(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, currentYear);
    ConfYear monthExpiryVacationPastYear = confYearManager
        .getByField(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, currentYear);
    ConfYear monthExpireRecoveryDaysOneThree = confYearManager
        .getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13, office, currentYear);
    ConfYear monthExpireRecoveryDaysFourNine = confYearManager
        .getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49, office, currentYear);
    ConfYear maxRecoveryDaysOneThree = confYearManager
        .getByField(Parameter.MAX_RECOVERY_DAYS_13, office, currentYear);
    ConfYear maxRecoveryDaysFourNine = confYearManager
        .getByField(Parameter.MAX_RECOVERY_DAYS_49, office, currentYear);
    ConfYear hourMaxToCalculateWorkTime = confYearManager
        .getByField(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, office, currentYear);

    render(currentYear, previousYear, lastYearDayExpiryVacationPastYear,
        lastYearMonthExpiryVacationPastYear, lastYearMonthExpireRecoveryDaysOneThree,
            lastYearMonthExpireRecoveryDaysFourNine, lastYearMaxRecoveryDaysOneThree,
            lastYearMaxRecoveryDaysFourNine, lastYearHourMaxToCalculateWorkTime,
            dayExpiryVacationPastYear, monthExpiryVacationPastYear,
            monthExpireRecoveryDaysOneThree, monthExpireRecoveryDaysFourNine,
            monthExpireRecoveryDaysFourNine, maxRecoveryDaysOneThree,
            maxRecoveryDaysFourNine, hourMaxToCalculateWorkTime, offices, office);

  }

  /**
   * Salva il nuovo valore per il field name. (Chiamata via ajax tramite X-editable)
   */
  public static void saveConfYear(String pk, String value) {

    ConfYear conf = confYearManager.getById(Long.parseLong(pk));

    Preconditions.checkNotNull(conf);

    MessageResult message = confYearManager.persistConfYear(conf, value);

    if (message.result == false) {
      response.status = 500;
      renderText(message.message);
    }
  }

  // FIXME al momento uso il campo name passato dall'x-editable per specificare
  // l'anno da cambiare
  // Ma quando il giorno e mese saranno in un unico parametro, conviene passare
  // direttamente l'id del confyear
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

  /**
   * metodo che renderizza la pagina di visualizzazione della configurazione periodica.
   * @param officeId id dell'ufficio
   */
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
  
  /**
   * Visualizzazioine nuova gestione configurazione.
   * @param officeId
   */
  public static void show(Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    
    rules.checkIfPermitted(office);
    
    List<Configuration> currentConfiguration = configurationManager
        .getOfficeConfigurationsByDate(office, LocalDate.now());
    
    render(office, currentConfiguration);
  }
  
  /**
   * Edit del parametro di configurazione.
   * @param configuration
   */
  public static void edit(Long configurationId) {
    
    Configuration configuration = Configuration.findById(configurationId);
    notFoundIfNull(configuration);
    rules.checkIfPermitted(configuration.office);
    
    ConfigurationDto configurationDto = new ConfigurationDto(configuration);
    
    render(configuration, configurationDto);
  }
  
  @Data
  public static class ConfigurationDto {

    public LocalDate validityBegin;
    public LocalDate validityEnd;
    
    public Integer validityYear;
    public Boolean toTheEnd = false;
    
    public Boolean booleanNewValue;
    public String stringNewValue;
    public Integer integerNewValue;
    public LocalDate localdateNewValue;
    
    
    /**
     * Default constructor.
     */
    public ConfigurationDto() {
      
    }
    
    /**
     * Constructor from configuration (contiene i valori del dto iniziale).
     * @param configuration
     */
    public ConfigurationDto(Configuration configuration) {
      if (configuration.epasParam.isGeneral()) {
        this.validityBegin = configuration.getBeginDate();
        this.validityEnd = configuration.calculatedEnd();
      }
      if (configuration.epasParam.isYearly()) {
        this.validityYear = LocalDate.now().getYear();
      }
      if (configuration.epasParam.isPeriodic()) {
        this.validityBegin = configuration.getBeginDate();
        this.validityEnd = configuration.calculatedEnd();
      }

      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.BOOLEAN)) {
        this.booleanNewValue = (Boolean)configuration.parseValue();
      }
      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER)) {
        this.integerNewValue = (Integer)configuration.parseValue();
      }
      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.IP_LIST)) {
        this.stringNewValue = EpasParamValueType.formatValue((IpList)configuration.parseValue());
      }
      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.LOCALDATE)) {
        this.localdateNewValue = (LocalDate)configuration.parseValue();
      }
      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.EMAIL)) {
        this.stringNewValue = (String)configuration.parseValue();
      }
      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH)) {
        this.stringNewValue = EpasParamValueType.formatValue((DayMonth)configuration.parseValue());
      }
      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.MONTH)) {
        this.integerNewValue = (Integer)configuration.parseValue();
      }
      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME)) {
        this.stringNewValue = EpasParamValueType.formatValue((LocalTime)configuration.parseValue());
      }
      if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME_INTERVAL)) {
        this.stringNewValue = EpasParamValueType
            .formatValue((LocalTimeInterval)configuration.parseValue());
      }
    }
  }
  
  /**
   * Aggiornamento di un parametro di configurazione.
   * @param configuration
   * @param validityYear
   * @param validityBegin
   * @param validityEnd
   * @param booleanNewValue
   * @param confirmed
   */
  public static void update(@Valid Configuration configuration, 
      ConfigurationDto configurationDto, boolean confirmed) {
    
    rules.checkIfPermitted(configuration.office);
    Configuration newConfiguration = null;
    if (configuration.epasParam.isGeneral()) {
      configurationDto.validityBegin = null;
      configurationDto.validityEnd = null;
    }
    if (configuration.epasParam.isYearly()) {
      configurationDto.validityBegin = configurationManager
          .officeYearBegin(configuration.office, configurationDto.validityYear);
      if (!configurationDto.toTheEnd) {
      configurationDto.validityEnd = configurationManager
          .officeYearEnd(configuration.office, configurationDto.validityYear);
      } 
      if (configurationDto.validityBegin == null) {
        validation.addError("configurationDto.validityYear", "anno non ammesso.");
      }
    }
    if (configuration.epasParam.isPeriodic()) {
      //validazione periodo
    }
    if (validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors: {}", validation.errorsMap());
      render("@edit", configuration, configurationDto);
    }
    

    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.BOOLEAN)) {
      if (configurationDto.booleanNewValue == null) {
        validation.addError("configurationDto.booleanNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateBoolean(configuration.epasParam, 
            configuration.office, configurationDto.booleanNewValue, 
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER)) {
      if (configurationDto.integerNewValue == null) {
        validation.addError("configurationDto.integerNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateInteger(configuration.epasParam, 
            configuration.office, configurationDto.integerNewValue, 
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.IP_LIST)) {
      if (configurationDto.stringNewValue != null) {
        IpList ipList = (IpList)EpasParamValueType.parseValue(
            configuration.epasParam.epasParamValueType, configurationDto.stringNewValue);
        // TODO: validazione sugli ip
        newConfiguration = configurationManager.updateIpList(configuration.epasParam, 
            configuration.office, ipList.ipList,
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.LOCALDATE)) {
      if (configurationDto.localdateNewValue == null) {
        validation.addError("configurationDto.localdateNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateLocalDate(configuration.epasParam, 
            configuration.office, configurationDto.localdateNewValue, 
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.EMAIL)) {
      if (configurationDto.stringNewValue != null) {
        // TODO: validazione sulla email
        newConfiguration = configurationManager.updateEmail(configuration.epasParam, 
            configuration.office, configurationDto.stringNewValue,
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH)) {
      DayMonth dayMonth = (DayMonth)EpasParamValueType
          .parseValue(EpasParamValueType.DAY_MONTH, configurationDto.stringNewValue);
      if (dayMonth != null) {
        newConfiguration = configurationManager.updateDayMonth(configuration.epasParam, 
            configuration.office, dayMonth.day, dayMonth.month,
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);

      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.MONTH)) {
      if (configurationDto.integerNewValue == null || configurationDto.integerNewValue < 0 
          || configurationDto.integerNewValue > 12) {
        validation.addError("configurationDto.integerNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateMonth(configuration.epasParam, 
            configuration.office, configurationDto.integerNewValue, 
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME)) {
      LocalTime localtime = (LocalTime)EpasParamValueType
          .parseValue(EpasParamValueType.LOCALTIME, configurationDto.stringNewValue);
      if (localtime != null) {
        newConfiguration = configurationManager.updateLocalTime(configuration.epasParam, 
            configuration.office, localtime, 
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (configuration.epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME_INTERVAL)) {
      LocalTimeInterval localtimeInterval = (LocalTimeInterval)EpasParamValueType
          .parseValue(EpasParamValueType.LOCALTIME_INTERVAL, configurationDto.stringNewValue);
      if (localtimeInterval != null) {
        newConfiguration = configurationManager.updateLocalTimeInterval(configuration.epasParam, 
            configuration.office, localtimeInterval.from, localtimeInterval.to, 
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }

    if (validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors: {}", validation.errorsMap());
      render("@edit", configuration, configurationDto);
    }

    Verify.verifyNotNull(newConfiguration);

    List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(newConfiguration, false);
    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(configuration.office.getBeginDate(),
            Optional.fromNullable(LocalDate.now()),
            periodRecaps);
    recomputeRecap.configuration = configuration;
    
    if (!confirmed) {
      
      response.status = 400;
      confirmed = true;
      render("@edit", confirmed, recomputeRecap, configuration, configurationDto);
    }
    
    periodManager.updatePeriods(newConfiguration, true);
    
    //TODO: ricalcoli
    
    flash.success("Parametro aggiornato correttamente.");
    
    show(configuration.office.id);
  }
  

}
