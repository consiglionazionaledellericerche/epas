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
import javax.inject.Inject;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cnr.sync.dto.v3.BadgeCreateDto;
import cnr.sync.dto.v3.ConfigurationOfficeDto;
import cnr.sync.dto.v3.ContractTerseDto;
import cnr.sync.dto.v3.OfficeShowTerseDto;
import cnr.sync.dto.v3.PersonConfigurationList;
import helpers.rest.ApiRequestException;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.dto.show.ListaDipendenti;
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
