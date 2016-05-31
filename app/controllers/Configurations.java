package controllers;


import com.google.common.base.Optional;
import com.google.common.base.Verify;

import dao.OfficeDao;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import manager.ConfigurationManager;
import manager.ConsistencyManager;
import manager.PeriodManager;
import manager.recaps.recomputation.RecomputeRecap;

import models.Configuration;
import models.Office;
import models.base.IPropertyInPeriod;
import models.enumerate.EpasParam.EpasParamValueType;
import models.enumerate.EpasParam.EpasParamValueType.IpList;
import models.enumerate.EpasParam.EpasParamValueType.LocalTimeInterval;

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
  private static OfficeDao officeDao;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static PeriodManager periodManager;
  @Inject
  private static SecurityRules rules;

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
        this.stringNewValue = EpasParamValueType.formatValue((MonthDay)configuration.parseValue());
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
  public static void update(Configuration configuration, 
      ConfigurationDto configurationDto, boolean confirmed) {
    
    notFoundIfNull(configuration);
    notFoundIfNull(configuration.office);
    
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
      MonthDay dayMonth = (MonthDay)EpasParamValueType
          .parseValue(EpasParamValueType.DAY_MONTH, configurationDto.stringNewValue);
      if (dayMonth != null) {
        newConfiguration = configurationManager.updateDayMonth(configuration.epasParam, 
            configuration.office, dayMonth.getDayOfMonth(), dayMonth.getMonthOfYear(),
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
        validation.addError("configurationDto.stringNewValue", "valore non valido. Formato accettato HH:mm-HH:mm");
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
            periodRecaps, Optional.<LocalDate>absent());
    recomputeRecap.configuration = configuration;
    
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
  

}
