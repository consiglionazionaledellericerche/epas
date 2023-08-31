/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

package manager.services.helpdesk;

import dao.GeneralSettingDao;
import feign.Body;
import feign.Feign;
import feign.FeignException;
import feign.Headers;
import feign.QueryMap;
import feign.RequestLine;
import feign.RequestTemplate;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.services.AuthRequestInterceptor;
import models.exports.ReportData;

/**
 * Client REST per la comunicazione con il servizio helpdesk-service.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class HelpdeskServiceClient {

  private static final String HELPDESK_SERVICE_API_SEND_URL = "/rest/v1/reportcenter/send";
  private static final String HELPDESK_SERVICE_API_CONFIG_URL = "/rest/v1/reportcenter/config";

  private GeneralSettingDao generalSettingDao;

  @Inject
  public HelpdeskServiceClient(GeneralSettingDao generalSettingDao) {
    this.generalSettingDao = generalSettingDao;
  }

  /**
   * API per l'epas-helpdesk-service.
   */
  interface HelpdeskService {

    @Headers("Content-Type: application/json")
    @RequestLine("PUT " + HELPDESK_SERVICE_API_SEND_URL)
    void send(ReportData reportData);
    
    @Headers("Content-Type: application/json")
    @RequestLine("GET " + HELPDESK_SERVICE_API_CONFIG_URL)
    String config();
  }

  HelpdeskService helpdeskServiceClient() {
    return Feign.builder()
        .requestInterceptor(new AuthRequestInterceptor())
        .encoder(new GsonEncoder())
        .target(HelpdeskService.class, generalSettingDao.generalSetting()
            .getEpasHelpdeskServiceUrl());
  }

  /**
   * Invia la segnalazione via REST all'epas-helpdesk-service.
   */
  public boolean send(ReportData reportData) {
    try {
      helpdeskServiceClient().send(reportData);
      log.info("Inviato a {} report: {}", 
          generalSettingDao.generalSetting().getEpasHelpdeskServiceUrl(), reportData);
    } catch (FeignException e) {
      log.warn("Problema nell'invio della segnalazione", e);
      return false;
    }
    return true;
  }

  /**
   * Invia la segnalazione via REST all'epas-helpdesk-service.
   */
  public ServiceResponse config() {
    val response = ServiceResponse.builder().build();
    try {
      response.setResult(helpdeskServiceClient().config());
      log.info("Inviata richiesta configurazione a {}", 
          generalSettingDao.generalSetting().getEpasHelpdeskServiceUrl());
    } catch (FeignException e) {
      log.warn("Problema nella ricezione delle configurazione da epas-helpdesk-service", e);
      response.getProblems().add(e.toString());
    }
    return response;
  } 
}