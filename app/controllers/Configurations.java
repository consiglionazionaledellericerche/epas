package controllers;


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import dao.OfficeDao;
import dao.PersonDao;

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

import models.Attachment;
import models.Configuration;
import models.Office;
import models.Person;
import models.PersonConfiguration;
import models.base.IPropertyInPeriod;
import models.enumerate.AttachmentType;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;

import play.db.jpa.Blob;
import play.libs.MimeTypes;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

@With({Resecure.class})
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
        IpList ipList = (IpList) EpasParamValueType.parseValue(
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
      MonthDay dayMonth = (MonthDay) EpasParamValueType
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
      LocalTime localtime = (LocalTime) EpasParamValueType
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
      LocalTimeInterval localtimeInterval = (LocalTimeInterval) EpasParamValueType
          .parseValue(EpasParamValueType.LOCALTIME_INTERVAL, configurationDto.stringNewValue);
      if (localtimeInterval != null) {
        newConfiguration = configurationManager.updateLocalTimeInterval(epasParam,
            configuration.getOwner(), localtimeInterval.from, localtimeInterval.to,
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      } else {
        validation.addError("configurationDto.stringNewValue",
            "valore non valido. Formato accettato HH:mm-HH:mm");
      }
    }

    return newConfiguration;
  }

  /**
   * Visualizzazioine nuova gestione configurazione.
   *
   * @param officeId l'id della sede di cui visualizzare le configurazioni.
   */
  public static void show(Long officeId) {

    final Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    final List<Configuration> configurations = configurationManager
        .getOfficeConfigurationsByDate(office, LocalDate.now());

    final List<Configuration> generals = configurations.stream()
        .filter(conf -> conf.epasParam.epasParamTimeType == EpasParam.EpasParamTimeType.GENERAL &&
            conf.epasParam != EpasParam.TR_AUTOCERTIFICATION).collect(Collectors.toList());

    final List<Configuration> yearlies = configurations.stream()
        .filter(conf -> conf.epasParam.epasParamTimeType == EpasParam.EpasParamTimeType.YEARLY)
        .collect(Collectors.toList());

    final List<Configuration> periodics = configurations.stream()
        .filter(conf -> conf.epasParam.epasParamTimeType == EpasParam.EpasParamTimeType.PERIODIC)
        .collect(Collectors.toList());

    final List<Configuration> autocertifications = configurations.stream()
        .filter(conf -> conf.epasParam == EpasParam.TR_AUTOCERTIFICATION)
        .collect(Collectors.toList());

    // id relativo all'allegato di autorizzazione per l'attivazione dell'autocertificazione
    final Attachment autocert = office.attachments.stream()
        .filter(attachment -> attachment.type == AttachmentType.TR_AUTOCERTIFICATION).findFirst()
        .orElse(null);

    render(office, generals, yearlies, periodics, autocertifications, autocert);
  }

  /**
   * Edit del parametro di configurazione.
   *
   * @param configurationId l'id della configurazione da editare.
   */
  public static void edit(Long configurationId) {

    Configuration configuration = Configuration.findById(configurationId);
    notFoundIfNull(configuration);
    rules.checkIfPermitted(configuration);

    ConfigurationDto configurationDto = new ConfigurationDto(configuration.epasParam,
        configuration.beginDate, configuration.calculatedEnd(),
        configurationManager.parseValue(configuration.epasParam, configuration.fieldValue));

    render(configuration, configurationDto);
  }

  /**
   * @param configuration    la configurazione da modificare.
   * @param configurationDto l'oggetto contenente la nuova configurazione.
   * @param confirmed        se siamo nel caso della conferma o no.
   */
  public static void update(Configuration configuration,
      ConfigurationDto configurationDto, boolean confirmed) {

    notFoundIfNull(configuration);
    notFoundIfNull(configuration.office);

    rules.checkIfPermitted(configuration);

    Configuration newConfiguration = (Configuration) compute(configuration,
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
   *
   * @param personId l'id della persona di cui vedere la configurazione.
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
   *
   * @param configurationId l'id della configurazione della persona da editare.
   */
  public static void personEdit(Long configurationId) {

    PersonConfiguration configuration = PersonConfiguration.findById(configurationId);
    notFoundIfNull(configuration);
    rules.checkIfPermitted(configuration.person.office);

    ConfigurationDto configurationDto = new ConfigurationDto(configuration.epasParam,
        configuration.beginDate, configuration.calculatedEnd(),
        configurationManager.parseValue(configuration.epasParam, configuration.fieldValue));

    render(configuration, configurationDto);
  }

  /**
   * @param configuration    la configurazione della persona da modificare.
   * @param configurationDto l'oggetto contenente la configurazione nuova.
   * @param confirmed        se siamo nello stato di conferma delle operazioni.
   */
  public static void personUpdate(PersonConfiguration configuration,
      ConfigurationDto configurationDto, boolean confirmed) {

    notFoundIfNull(configuration);
    notFoundIfNull(configuration.person);
    notFoundIfNull(configuration.person.office);

    rules.checkIfPermitted(configuration.person.office);

    PersonConfiguration newConfiguration = (PersonConfiguration) compute(configuration,
        configuration.epasParam, configurationDto);

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

    if (configuration.epasParam.equals(EpasParam.OFF_SITE_STAMPING)
        && !(Boolean) configurationManager.configValue(configuration.person.office,
        EpasParam.WORKING_OFF_SITE)) {
      response.status = 400;
      flash.error("Prima abilitare la timbratura per lavoro fuori sede per i dipendenti "
          + "tra i parametri della sede.");
      personShow(configuration.person.id);
    }

    periodManager.updatePeriods(newConfiguration, true);

    consistencyManager.performRecomputation(configuration.person,
        configuration.epasParam.recomputationTypes, recomputeRecap.recomputeFrom);

    flash.success("Parametro aggiornato correttamente.");

    personShow(configuration.person.id);
  }

  public static void uploadAttachment(Long officeId, File file) throws FileNotFoundException {

    final Office office = officeDao.getOfficeById(officeId);

    Preconditions.checkState(office.isPersistent());
    Preconditions.checkNotNull(file);

    final Attachment attachment = new Attachment();

    attachment.filename = file.getName();
    attachment.type = AttachmentType.TR_AUTOCERTIFICATION;
    Blob blob = new Blob();
    blob.set(new FileInputStream(file), MimeTypes.getContentType(file.getName()));
    attachment.file = blob;
    attachment.office = office;
    attachment.save();

    show(officeId);
  }

  public static void removeAttachment(Long attachmentId) {

    final Attachment attachment = Attachment.findById(attachmentId);

    notFoundIfNull(attachment);

    rules.checkIfPermitted(attachment);

    final Long officeId = attachment.office.id;

    attachment.delete();

    show(officeId);
  }

  public static void getAttachment(Long attachmentId) {

    final Attachment attachment = Attachment.findById(attachmentId);

    notFoundIfNull(attachment);

    rules.checkIfPermitted(attachment);

    renderBinary(attachment.file.get(), attachment.filename, false);
  }

}
