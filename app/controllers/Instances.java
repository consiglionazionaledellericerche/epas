/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cnr.sync.dto.v3.BadgeCreateDto;
import cnr.sync.dto.v3.ConfigurationOfficeDto;
import cnr.sync.dto.v3.ContractTerseDto;
import cnr.sync.dto.v3.MealTicketResidualDto;
import cnr.sync.dto.v3.OfficeShowTerseDto;
import cnr.sync.dto.v3.PersonConfigurationList;
import cnr.sync.dto.v3.PersonConfigurationShowDto;
import cnr.sync.dto.v3.PersonResidualDto;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.rest.ApiRequestException;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.PeriodManager;
import manager.attestati.dto.show.ListaDipendenti;
import manager.configurations.ConfigurationDto;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import manager.recaps.recomputation.RecomputeRecap;
import models.Configuration;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonConfiguration;
import models.base.IPropertyInPeriod;
import models.enumerate.BlockType;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

@Slf4j
@With({Resecure.class})
public class Instances extends Controller {

  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final String LIST = "list";
  private static final String RESIDUAL_LIST = "residualList";
  private static final String CONTRACT_LIST = "contractList";
  private static final String OFFICE_CONFIGURATION = "officeConfiguration";
  private static final String PEOPLE_CONFIGURATIONS = "peopleConfiguration";
  private static final String PATH = "/rest/v3/instances";


  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static PersonDao personDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static ContractManager contractManager;

  public static void importInstance() {
    render();
  }

  public static void importSeat(String instance) {
    if (Strings.isNullOrEmpty(instance)) {
      flash.error("Inserisci un indirizzo valido");
      importInstance();
    }
    WSRequest wsRequest = WS.url(instance+PATH+"/"+LIST)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<OfficeShowTerseDto> list = (List<OfficeShowTerseDto>) new Gson()
        .fromJson(httpResponse.getJson(), OfficeShowTerseDto.class);
    render(list, instance);
  }

  public static void importInfo(String instance, String code) {
    if (Strings.isNullOrEmpty(instance)) {
      flash.error("Inserisci un indirizzo valido");
      importInstance();
    }
  }

  public static void importPeopleConfigurations(String instance, String code) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+PEOPLE_CONFIGURATIONS)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<PersonConfigurationList> list = (List<PersonConfigurationList>) new Gson()
        .fromJson(httpResponse.getJson(), PersonConfigurationList.class);
    EpasParam epasParam = null;
    PersonConfiguration newConfiguration = null;
    for (PersonConfigurationList pcl : list) {
      Person person = personDao.getPersonByNumber(pcl.getNumber());
      for (PersonConfigurationShowDto pcs : pcl.getList()) {
        epasParam = EpasParam.valueOf(pcs.getEpasParam());        
        Optional<PersonConfiguration> configuration = configurationManager
            .getConfigurtionByPersonAndType(person, epasParam);
        if (configuration.isPresent()) {
          newConfiguration = (PersonConfiguration) configurationManager.updateBoolean(epasParam,
              person, Boolean.getBoolean(pcs.getFieldValue()),
              com.google.common.base.Optional.fromNullable(pcs.getBeginDate()),
              com.google.common.base.Optional.fromNullable(pcs.getEndDate()), false);

          List<IPropertyInPeriod> periodRecaps = periodManager
              .updatePeriods(newConfiguration, false);

          RecomputeRecap recomputeRecap =
              periodManager.buildRecap(configuration.get().getPerson().getBeginDate(),
                  com.google.common.base.Optional.fromNullable(LocalDate.now()),
                  periodRecaps, com.google.common.base.Optional.<LocalDate>absent());
          recomputeRecap.epasParam = configuration.get().getEpasParam();
          periodManager.updatePeriods(newConfiguration, true);

          consistencyManager.performRecomputation(configuration.get().getPerson(),
              configuration.get().getEpasParam().recomputationTypes, recomputeRecap.recomputeFrom);
        }
        log.debug("Aggiornato valore del parametro {} per {}", 
            epasParam.name, person.getFullname());
      }
    }
  }

  public static void importOfficeConfiguration(String instance, String code) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+OFFICE_CONFIGURATION)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<ConfigurationOfficeDto> list = (List<ConfigurationOfficeDto>) new Gson()
        .fromJson(httpResponse.getJson(), ConfigurationOfficeDto.class);
    com.google.common.base.Optional<Office> office = officeDao.byCode(code);
    Configuration newConfiguration = null;
    EpasParam epasParam = null;
    if (office.isPresent()) {

      for (ConfigurationOfficeDto dto : list) {
        epasParam = EpasParam.valueOf(dto.getEpasParam());
        Optional<Configuration> configuration = configurationManager
            .getConfigurationByOfficeAndType(office.get(), epasParam);
        switch(epasParam.epasParamValueType) {
          case BOOLEAN:
            newConfiguration = (Configuration) configurationManager.updateBoolean(epasParam,
                office.get(), Boolean.getBoolean(dto.getFieldValue()),
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case DAY_MONTH:
            MonthDay dayMonth = (MonthDay) EpasParamValueType
                .parseValue(EpasParamValueType.DAY_MONTH, dto.getFieldValue());
            newConfiguration = (Configuration) configurationManager.updateDayMonth(epasParam,
                office.get(), dayMonth.getDayOfMonth(), dayMonth.getMonthOfYear(),
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case EMAIL:
            newConfiguration = (Configuration) configurationManager.updateEmail(epasParam,
                office.get(), dto.getFieldValue(),
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case ENUM:
            newConfiguration = (Configuration) configurationManager.updateEnum(epasParam,
                office.get(), BlockType.valueOf(dto.getFieldValue()),
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case INTEGER:
            newConfiguration = (Configuration) configurationManager.updateInteger(epasParam,
                office.get(), Integer.getInteger(dto.getFieldValue()),
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case IP_LIST:
            IpList ipList = (IpList) EpasParamValueType.parseValue(
                epasParam.epasParamValueType, dto.getFieldValue());
            newConfiguration = (Configuration) configurationManager.updateIpList(epasParam,
                office.get(), ipList.ipList,
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case LOCALDATE:
            newConfiguration = (Configuration) configurationManager.updateLocalDate(epasParam,
                office.get(), LocalDate.parse(dto.getFieldValue()),
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case LOCALTIME:
            LocalTime localtime = (LocalTime) EpasParamValueType
            .parseValue(EpasParamValueType.LOCALTIME, dto.getFieldValue());
            newConfiguration = (Configuration) configurationManager.updateLocalTime(epasParam,
                office.get(), localtime,
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case LOCALTIME_INTERVAL:
            LocalTimeInterval localtimeInterval = (LocalTimeInterval) EpasParamValueType
            .parseValue(EpasParamValueType.LOCALTIME_INTERVAL, dto.getFieldValue());
            newConfiguration = (Configuration) configurationManager
                .updateLocalTimeInterval(epasParam,
                office.get(), localtimeInterval.from, localtimeInterval.to,
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          case MONTH:
            newConfiguration = (Configuration) configurationManager.updateMonth(epasParam,
                office.get(), Integer.getInteger(dto.getFieldValue()),
                com.google.common.base.Optional.fromNullable(dto.getBeginDate()),
                com.google.common.base.Optional.fromNullable(dto.getEndDate()), false);
            break;
          default:
            break;
        }
        List<IPropertyInPeriod> periodRecaps = periodManager
            .updatePeriods(newConfiguration, false);
        RecomputeRecap recomputeRecap =
            periodManager.buildRecap(office.get().getBeginDate(),
                com.google.common.base.Optional.fromNullable(LocalDate.now()),
                periodRecaps, com.google.common.base.Optional.<LocalDate>absent());
        recomputeRecap.epasParam = configuration.get().getEpasParam();
        periodManager.updatePeriods(newConfiguration, true);

        consistencyManager.performRecomputation(office.get(),
            configuration.get().getEpasParam().recomputationTypes, recomputeRecap.recomputeFrom);
      }
    }
  }

  public static void importContracts(String instance, String code) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+CONTRACT_LIST)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<ContractTerseDto> list = (List<ContractTerseDto>) new Gson()
        .fromJson(httpResponse.getJson(), ContractTerseDto.class);
    Person person = null;
    Contract newContract = null;
    for (ContractTerseDto dto : list) {
      person = personDao.getPersonByNumber(dto.getNumber());
      if (person != null) {
        IWrapperPerson wrPerson = wrapperFactory.create(person);
        com.google.common.base.Optional<Contract> actualContract = wrPerson.getCurrentContract();
        if (actualContract.isPresent()) {
          newContract = new Contract();
          newContract.setBeginDate(dto.getBeginDate());
          newContract.setEndDate(dto.getEndDate());
          newContract.isOnCertificate();
          newContract.setPerson(person);
          
          boolean esito = contractManager.properContractCreate(newContract, 
              com.google.common.base.Optional.absent(), true);
         if (esito) {
           log.debug("Salvato nuovo contratto per {} con date {} - {}", 
               person.getFullname(), newContract.getBeginDate(), newContract.getEndDate()); 
         } else {
           log.debug("Non c'è stato bisogno di salvare un nuovo contratto per {}", 
               person.getFullname());
         }
          
        }
      }
    }
  }

  public static void importResidual(String instance, String code) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+RESIDUAL_LIST)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<PersonResidualDto> list = (List<PersonResidualDto>) new Gson()
        .fromJson(httpResponse.getJson(), PersonResidualDto.class);
    Person person = null;
    for (PersonResidualDto dto : list) {
      person = personDao.getPersonByNumber(dto.getNumber());
      IWrapperPerson wrPerson = wrapperFactory.create(person);
      com.google.common.base.Optional<Contract> actualContract = wrPerson.getCurrentContract();
      if (actualContract.isPresent()) {
        actualContract.get().setSourceDateResidual(dto.getDate());
        actualContract.get().setSourceRemainingMinutesCurrentYear(dto.getResidual());
        actualContract.get().save();
        log.debug("Salvato residuo di {} minuti per {}",dto.getResidual(), person.getFullname());
      }
    }
  }
  
  public static void importMealTicketResidual(String instance, String code) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+RESIDUAL_LIST)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<MealTicketResidualDto> list = (List<MealTicketResidualDto>) new Gson()
        .fromJson(httpResponse.getJson(), MealTicketResidualDto.class);
    Person person = null;
    for (MealTicketResidualDto dto : list) {
      person = personDao.getPersonByNumber(dto.getNumber());
      IWrapperPerson wrPerson = wrapperFactory.create(person);
      com.google.common.base.Optional<Contract> actualContract = wrPerson.getCurrentContract();
      if (actualContract.isPresent()) {
        actualContract.get().setSourceDateMealTicket(dto.getDateOfResidual());
        actualContract.get().setSourceRemainingMealTicket(dto.getMealTicketResidual().intValue());
        actualContract.get().save();
        log.debug("Salvato residuo di {} buoni pasto per {}",
            dto.getMealTicketResidual(), person.getFullname());
      }
    }
  }
}
