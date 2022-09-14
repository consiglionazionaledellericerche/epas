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

package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import common.security.SecurityRules;
import dao.GeneralSettingDao;
import dao.OfficeDao;
import dao.PersonDao;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.PeriodManager;
import manager.configurations.ConfigurationDto;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamCategory;
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
import play.data.validation.Validation;
import play.db.jpa.Blob;
import play.libs.MimeTypes;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle configurazioni.
 */
@With({Resecure.class})
@Slf4j
public class Configurations extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static SecurityRules rules;
  @Inject
  static GeneralSettingDao generalSettingDao;

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
        Validation.addError("configurationDto.validityYear", "anno non ammesso.");
      }
    }
    if (epasParam.isPeriodic()) {
      //validazione periodo
    }
    if (Validation.hasErrors()) {
      return newConfiguration;
    }

    if (epasParam.epasParamValueType.equals(EpasParamValueType.BOOLEAN)) {
      if (configurationDto.booleanNewValue == null) {
        Validation.addError("configurationDto.booleanNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateBoolean(epasParam,
            configuration.getOwner(), configurationDto.booleanNewValue,
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER)) {
      if (configurationDto.integerNewValue == null) {
        Validation.addError("configurationDto.integerNewValue", "valore non valido.");
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
        Validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALDATE)) {
      if (configurationDto.localdateNewValue == null) {
        Validation.addError("configurationDto.localdateNewValue", "valore non valido.");
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
        Validation.addError("configurationDto.stringNewValue", "valore non valido.");
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
        Validation.addError("configurationDto.stringNewValue", "valore non valido.");
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.MONTH)) {
      if (configurationDto.integerNewValue == null || configurationDto.integerNewValue < 0
          || configurationDto.integerNewValue > 12) {
        Validation.addError("configurationDto.integerNewValue", "valore non valido.");
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
        Validation.addError("configurationDto.stringNewValue", "valore non valido.");
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
        Validation.addError("configurationDto.stringNewValue",
            "valore non valido. Formato accettato HH:mm-HH:mm");
      }
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.ENUM)) {
      if (configurationDto.blockTypeNewValue == null) {
        Validation.addError("configurationDto.blockTypeNewValue", "valore non valido.");
      } else {
        newConfiguration = configurationManager.updateEnum(epasParam,
            configuration.getOwner(), configurationDto.blockTypeNewValue,
            Optional.fromNullable(configurationDto.validityBegin),
            Optional.fromNullable(configurationDto.validityEnd), false);
      }
    }

    return newConfiguration;
  }

  /**
   * Visualizzazioine nuova gestione configurazione.
   *
   * @param officeId l'id della sede di cui visualizzare le configurazioni.
   */
  public static void show(Long officeId, EpasParamCategory paramCategory) {

    final Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    
    if (paramCategory == null) {
      paramCategory = EpasParam.EpasParamCategory.GENERAL;
    }

    final List<Configuration> configurations = configurationManager
        .getOfficeConfigurationsByDate(office, LocalDate.now());

    final List<Configuration> generals = configurations.stream()
        .filter(conf -> conf.epasParam.category == EpasParam.EpasParamCategory.GENERAL)
        .collect(Collectors.toList());

    final List<Configuration> yearlies = configurations.stream()
        .filter(conf -> conf.epasParam.category == EpasParam.EpasParamCategory.YEARLY)
        .collect(Collectors.toList());

    final List<Configuration> periodics = configurations.stream()
        .filter(conf -> conf.epasParam.category == EpasParam.EpasParamCategory.PERIODIC)
        .collect(Collectors.toList());

    final List<Configuration> autocertifications = configurations.stream()
        .filter(conf -> conf.epasParam.category == EpasParam.EpasParamCategory.AUTOCERTIFICATION)
        .collect(Collectors.toList());

    List<Configuration> flows = configurations.stream()
        .filter(conf -> conf.epasParam.category == EpasParam.EpasParamCategory.FLOWS)
        .collect(Collectors.toList());
    
    if (!generalSettingDao.generalSetting().enableAbsenceTopLevelAuthorization) {
      flows = flows.stream().filter(conf -> 
          !conf.epasParam.equals(EpasParam.COMPENSATORY_REST_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED) &&
          !conf.epasParam.equals(EpasParam.COMPENSATORY_REST_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED) &&
          !conf.epasParam.equals(EpasParam.VACATION_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED) &&
          !conf.epasParam.equals(EpasParam.VACATION_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED))
        .collect(Collectors.toList());
    } else {
      flows = flows.stream().filter(conf -> 
          !conf.epasParam.equals(EpasParam.ABSENCE_TOP_LEVEL_GROUP_MANAGER_NOTIFICATION) &&
          !conf.epasParam.equals(EpasParam.ABSENCE_TOP_LEVEL_OFFICE_HEAD_NOTIFICATION) &&
          !conf.epasParam.equals(EpasParam.ABSENCE_TOP_LEVEL_OF_GROUP_MANAGER_OFFICE_HEAD_NOTIFICATION))
        .collect(Collectors.toList());
    }
    final List<Configuration> competenceFlows = configurations.stream()
        .filter(conf -> conf.epasParam.category == EpasParam.EpasParamCategory.COMPETENCE_FLOWS)
        .collect(Collectors.toList());
    
    final List<Configuration> informationFlows = configurations.stream()
        .filter(conf -> conf.epasParam.category == EpasParam.EpasParamCategory.INFORMATION_FLOWS)
        .collect(Collectors.toList());
    
    // id relativo all'allegato di autorizzazione per l'attivazione dell'autocertificazione
    final Attachment autocert = office.attachments.stream()
        .filter(attachment -> attachment.type == AttachmentType.TR_AUTOCERTIFICATION).findFirst()
        .orElse(null);

    render(office, paramCategory, generals, yearlies, periodics, 
        autocertifications, autocert, flows, competenceFlows, informationFlows);
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
   * Modifica la configurazione.
   *
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

    if (Validation.hasErrors()) {
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
    
    show(configuration.office.id, configuration.epasParam.category);
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
   * Modifica la configurazione della persona.
   *
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

    if (Validation.hasErrors()) {
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

  /**
   * Aggiorna il file allegato.
   *
   * @param officeId id dell'ufficio a cui associare l'allegato.
   * @param file file allegato.
   * @throws FileNotFoundException in casi di problemi con il file allegato.
   */
  public static void uploadAttachment(Long officeId, File file) throws FileNotFoundException {

    final Office office = officeDao.getOfficeById(officeId);

    Preconditions.checkState(office.isPersistent());
    Preconditions.checkNotNull(file);

    rules.checkIfPermitted(office);

    final Attachment attachment = new Attachment();

    attachment.filename = file.getName();
    attachment.type = AttachmentType.TR_AUTOCERTIFICATION;
    Blob blob = new Blob();
    blob.set(new FileInputStream(file), MimeTypes.getContentType(file.getName()));
    attachment.file = blob;
    attachment.office = office;
    attachment.save();

    show(officeId, EpasParam.EpasParamCategory.AUTOCERTIFICATION);
  }

  /**
   * Permette di rimuovere l'allegato all'autocertificazione per le timbrature.
   *
   * @param attachmentId l'identificativo dell'allegato da rimuovere
   */
  public static void removeAttachment(Long attachmentId) {

    final Attachment attachment = Attachment.findById(attachmentId);

    notFoundIfNull(attachment);

    rules.checkIfPermitted(attachment);

    final Long officeId = attachment.office.id;

    attachment.delete();

    show(officeId, EpasParam.EpasParamCategory.AUTOCERTIFICATION);
  }

  /**
   * Ritorna l'allegato con identificativo attachmentId.
   *
   * @param attachmentId l'identificativo dell'allegato da ritornare
   */
  public static void getAttachment(Long attachmentId) {

    final Attachment attachment = Attachment.findById(attachmentId);

    notFoundIfNull(attachment);

    rules.checkIfPermitted(attachment);

    renderBinary(attachment.file.get(), attachment.filename, false);
  }

}