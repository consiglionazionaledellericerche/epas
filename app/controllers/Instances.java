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
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cnr.sync.dto.v3.BadgeCreateDto;
import cnr.sync.dto.v3.ConfigurationOfficeDto;
import cnr.sync.dto.v3.ContractTerseDto;
import cnr.sync.dto.v3.OfficeShowTerseDto;
import cnr.sync.dto.v3.PersonConfigurationList;
import cnr.sync.dto.v3.PersonConfigurationShowDto;
import dao.PersonDao;
import helpers.rest.ApiRequestException;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.PeriodManager;
import manager.attestati.dto.show.ListaDipendenti;
import manager.configurations.ConfigurationDto;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.recaps.recomputation.RecomputeRecap;
import models.Configuration;
import models.Person;
import models.PersonConfiguration;
import models.base.IPropertyInPeriod;
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

  public static void importInstance() {
    render();
  }
  
  public static void importSeat(String instance) {
    if (Strings.isNullOrEmpty(instance)) {
      flash.error("Inserisci un indirizzo valido");
      importInstance();
    }
    WSRequest wsRequest = WS.url(instance+PATH+"/"+LIST).setHeader("Content-Type", JSON_CONTENT_TYPE)
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
    WSRequest wsRequest = WS.url(instance+PATH+"/"+PEOPLE_CONFIGURATIONS).setHeader("Content-Type", JSON_CONTENT_TYPE)
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
        Optional<PersonConfiguration> configuration = configurationManager.getConfigurtionByPersonAndType(person, epasParam);
        if (configuration.isPresent()) {
          newConfiguration = (PersonConfiguration) configurationManager.updateBoolean(epasParam,
              person, Boolean.getBoolean(pcs.getFieldValue()),
              com.google.common.base.Optional.fromNullable(pcs.getBeginDate()),
              com.google.common.base.Optional.fromNullable(pcs.getEndDate()), false);
          
          List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(newConfiguration, false);
          
          RecomputeRecap recomputeRecap =
              periodManager.buildRecap(configuration.get().getPerson().getBeginDate(),
                  com.google.common.base.Optional.fromNullable(LocalDate.now()),
                  periodRecaps, com.google.common.base.Optional.<LocalDate>absent());
          recomputeRecap.epasParam = configuration.get().getEpasParam();
          periodManager.updatePeriods(newConfiguration, true);

          consistencyManager.performRecomputation(configuration.get().getPerson(),
              configuration.get().getEpasParam().recomputationTypes, recomputeRecap.recomputeFrom);
        }
        log.debug("Aggiornato valore del parametro {} per {}", epasParam.name, person.getFullname());
      }
    }
  }
  
  public static void importOfficeConfiguration(String instance, String code) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+OFFICE_CONFIGURATION).setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");
    
    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<ConfigurationOfficeDto> list = (List<ConfigurationOfficeDto>) new Gson()
        .fromJson(httpResponse.getJson(), ConfigurationOfficeDto.class);
    Configuration newConfiguration = null;
    EpasParam epasParam = null;
    for (ConfigurationOfficeDto dto : list) {
      epasParam = EpasParam.valueOf(dto.getEpasParam());
     switch(epasParam.epasParamValueType) {
       case BOOLEAN:
         break;
       case DAY_MONTH:
         break;
       case EMAIL:
         break;
       case ENUM:
         break;
       case INTEGER:
         break;
       case IP_LIST:
         break;
       case LOCALDATE:
         break;
       case LOCALTIME:
         break;
       case LOCALTIME_INTERVAL:
         break;
       case MONTH:
         break;
         default:
           break;
     }
      List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(newConfiguration, false);
      RecomputeRecap recomputeRecap =
          periodManager.buildRecap(configuration.getOffice().getBeginDate(),
              Optional.fromNullable(LocalDate.now()),
              periodRecaps, Optional.<LocalDate>absent());
      recomputeRecap.epasParam = configuration.getEpasParam();
      periodManager.updatePeriods(newConfiguration, true);

      consistencyManager.performRecomputation(configuration.getOffice(),
          configuration.getEpasParam().recomputationTypes, recomputeRecap.recomputeFrom);
    }
  }
  
  public static void importContracts(String instance, String code) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+CONTRACT_LIST).setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");
    
    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<ContractTerseDto> list = (List<ContractTerseDto>) new Gson()
        .fromJson(httpResponse.getJson(), ContractTerseDto.class);
  }
  
  public static void importResidual(String instance, String code) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+RESIDUAL_LIST).setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");
    
    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
  }
}
