package controllers;


import com.google.common.base.Optional;
import com.google.common.base.Verify;

import dao.OfficeDao;
import dao.PersonDao;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
import manager.PeriodManager;
import manager.configurations.ConfigurationDto;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import manager.recaps.recomputation.RecomputeRecap;

import models.Configuration;
import models.Office;
import models.Person;
import models.PersonConfiguration;
import models.base.IPropertyInPeriod;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.util.List;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
@Slf4j
public class Configurations extends Controller {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static PeriodManager periodManager;
  @Inject
  private static SecurityRules rules;



  private static IPropertyInPeriod compute(IPropertyInPeriod configuration, EpasParam epasParam,
      ConfigurationDto configurationDto) {
    
    IPropertyInPeriod newConfiguration = null; 
    
    if (epasParam.isGeneral()) {
      configurationDto.validityBegin = null;
      configurationDto.validityEnd = null;
    }
    if (epasParam.isYearly()) {
      configurationDto.validityBegin = configurationManager
          .targetYearBegin(configuration.getOwner(), configurationDto.validityYear);
      if (!configurationDto.toTheEnd) {
      configurationDto.validityEnd = configurationManager
          .targetYearEnd(configuration.getOwner(), configurationDto.validityYear);
      } 
      if (configurationDto.validityBegin == null) {
        validation.addError("configurationDto.validityYear", "anno non ammesso.");
      }
    }
    if (epasParam.isPeriodic()) {
      //validazione periodo
    }
    if (validation.hasErrors()) {
      return newConfiguration;
    }

    if (epasParam.epasParamValueType.equals(EpasParamValueType.BOOLEAN)) {
      if (configurationDto.booleanNewValue == null) {
        validation.addError("configurationDto.booleanNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateBoolean(epasParam, 
            configuration.getOwner(), configurationDto.booleanNewValue, 
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER)) {
      if (configurationDto.integerNewValue == null) {
        validation.addError("configurationDto.integerNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateInteger(epasParam, 
            configuration.getOwner(), configurationDto.integerNewValue, 
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.IP_LIST)) {
      if (configurationDto.stringNewValue != null) {
        IpList ipList = (IpList)EpasParamValueType.parseValue(
            epasParam.epasParamValueType, configurationDto.stringNewValue);
        // TODO: validazione sugli ip
        newConfiguration = configurationManager.updateIpList(epasParam, 
            configuration.getOwner(), ipList.ipList,
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALDATE)) {
      if (configurationDto.localdateNewValue == null) {
        validation.addError("configurationDto.localdateNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateLocalDate(epasParam, 
            configuration.getOwner(), configurationDto.localdateNewValue, 
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.EMAIL)) {
      if (configurationDto.stringNewValue != null) {
        // TODO: validazione sulla email
        newConfiguration = configurationManager.updateEmail(epasParam, 
            configuration.getOwner(), configurationDto.stringNewValue,
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH)) {
      MonthDay dayMonth = (MonthDay)EpasParamValueType
          .parseValue(EpasParamValueType.DAY_MONTH, configurationDto.stringNewValue);
      if (dayMonth != null) {
        newConfiguration = configurationManager.updateDayMonth(epasParam, 
            configuration.getOwner(), dayMonth.getDayOfMonth(), dayMonth.getMonthOfYear(),
            Optional.fromNullable(configurationDto.validityBegin), 
            Optional.fromNullable(configurationDto.validityEnd), false);

      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.MONTH)) {
      if (configurationDto.integerNewValue == null || configurationDto.integerNewValue < 0 
          || configurationDto.integerNewValue > 12) {
        validation.addError("configurationDto.integerNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateMonth(epasParam, 
            configuration.getOwner(), configurationDto.integerNewValue, 
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME)) {
      LocalTime localtime = (LocalTime)EpasParamValueType
          .parseValue(EpasParamValueType.LOCALTIME, configurationDto.stringNewValue);
      if (localtime != null) {
        newConfiguration = configurationManager.updateLocalTime(epasParam, 
            configuration.getOwner(), localtime, 
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME_INTERVAL)) {
      LocalTimeInterval localtimeInterval = (LocalTimeInterval)EpasParamValueType
          .parseValue(EpasParamValueType.LOCALTIME_INTERVAL, configurationDto.stringNewValue);
      if (localtimeInterval != null) {
        newConfiguration = configurationManager.updateLocalTimeInterval(epasParam, 
            configuration.getOwner(), localtimeInterval.from, localtimeInterval.to, 
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue", "valore non valido. Formato accettato HH:mm-HH:mm");
      }
    }
    
    return newConfiguration;
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
    
    ConfigurationDto configurationDto = new ConfigurationDto(configuration.epasParam, 
        configuration.beginDate, configuration.calculatedEnd(), 
        configurationManager.parseValue(configuration.epasParam,  configuration.fieldValue));
    
    render(configuration, configurationDto);
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
  public static void update(Configuration configuration, 
      ConfigurationDto configurationDto, boolean confirmed) {
    
    notFoundIfNull(configuration);
    notFoundIfNull(configuration.office);
    
    rules.checkIfPermitted(configuration.office);
    
    Configuration newConfiguration = (Configuration)compute(configuration, 
        configuration.epasParam, configurationDto);

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
            periodRecaps, Optional.<LocalDate>absent());
    recomputeRecap.epasParam = configuration.epasParam;
    
    if (!confirmed) {
      
      response.status = 400;
      confirmed = true;
      render("@edit", confirmed, recomputeRecap, configuration, configurationDto);
    }
    
    periodManager.updatePeriods(newConfiguration, true);
    
    consistencyManager.performRecomputation(configuration.office, 
        configuration.epasParam.recomputationTypes, recomputeRecap.recomputeFrom);
    
    flash.success("Parametro aggiornato correttamente.");
    
    show(configuration.office.id);
  }
  
  /**
   * Visualizzazioine nuova gestione configurazione.
   * @param officeId
   */
  public static void personShow(Long personId) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    
    rules.checkIfPermitted(person.office);
    
    List<PersonConfiguration> currentConfiguration = configurationManager
        .getPersonConfigurationsByDate(person, LocalDate.now());
    
    render(person, currentConfiguration);
  }
  
  /**
   * Edit del parametro di configurazione.
   * @param configuration
   */
  public static void personEdit(Long configurationId) {
    
    PersonConfiguration configuration = PersonConfiguration.findById(configurationId);
    notFoundIfNull(configuration);
    rules.checkIfPermitted(configuration.person.office);
    
    ConfigurationDto configurationDto = new ConfigurationDto(configuration.epasParam, 
        configuration.beginDate, configuration.calculatedEnd(), 
        configurationManager.parseValue(configuration.epasParam,  configuration.fieldValue));
    
    render(configuration, configurationDto);
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
  public static void personUpdate(PersonConfiguration configuration, 
      ConfigurationDto configurationDto, boolean confirmed) {
    
    notFoundIfNull(configuration);
    notFoundIfNull(configuration.person);
    notFoundIfNull(configuration.person.office);
    
    rules.checkIfPermitted(configuration.person.office);
    
    PersonConfiguration newConfiguration = (PersonConfiguration)compute(configuration, 
        configuration.epasParam, configurationDto);
    
    if (!validation.hasErrors()) {
      if (configuration.epasParam.equals(EpasParam.OFF_SITE_STAMPING) 
          && !(Boolean)configurationManager.configValue(configuration.person.office, 
              EpasParam.WORKING_OFF_SITE)) {
        validation.addError("configurationDto.booleanNewValue",
            "per poter modifcare questo parametro, occorre prima impostare il parametro "
            + "di abilitazione alla timbratura fuori sede per i dipendenti.");
      }        
    }

    if (validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors: {}", validation.errorsMap());
      render("@personEdit", configuration, configurationDto);
    }
    
    Verify.verifyNotNull(newConfiguration);

    List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(newConfiguration, false);
    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(configuration.person.getBeginDate(),
            Optional.fromNullable(LocalDate.now()),
            periodRecaps, Optional.<LocalDate>absent());
    recomputeRecap.epasParam = configuration.epasParam;
    
    if (!confirmed) {
      
      response.status = 400;
      confirmed = true;
      render("@personEdit", confirmed, recomputeRecap, configuration, configurationDto);
    }
    
    periodManager.updatePeriods(newConfiguration, true);
    
    consistencyManager.performRecomputation(configuration.person, 
        configuration.epasParam.recomputationTypes, recomputeRecap.recomputeFrom);
    
    flash.success("Parametro aggiornato correttamente.");
    
    personShow(configuration.person.id);
  }


}
