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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import controllers.SecurityTokens;
import dao.GeneralSettingDao;
import feign.FeignException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.exports.ReportData;
import play.libs.WS;

/**
 * Gestisce la chiamate al componente esterno epas-helpdesk-service.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class HelpdeskServiceManager {

  public static final  String API_CONFIG = "/rest/v1/reportcenter/config";

  private final GeneralSettingDao generalSettingDao;
  private final HelpdeskServiceClient helpdeskServiceClient;

  @Inject
  public HelpdeskServiceManager(GeneralSettingDao generalSettingDao,
      HelpdeskServiceClient helpdeskServiceClient) {
    this.generalSettingDao = generalSettingDao;
    this.helpdeskServiceClient = helpdeskServiceClient;
  }

  public String getServiceUrl() {
    return generalSettingDao.generalSetting().getEpasHelpdeskServiceUrl();
  }

  public String getServiceConfigUrl() throws MalformedURLException {
    return new URL(new URL(getServiceUrl()), API_CONFIG).toString();
  }

  public boolean sendReport(ReportData reportData) {
    log.debug("Sending report to {} -> {}", getServiceUrl(), reportData);
    return helpdeskServiceClient.send(reportData);
  }

  /**
   * Risposta alla richiesta di prelevare la configurazione
   * del epas-helpdesk-service.
   */
  public ServiceResponse getConfig() {
    Optional<String> currentJwt = SecurityTokens.getCurrentJwt();
    val response = ServiceResponse.builder().build();
    if (!currentJwt.isPresent()) {
      response.getProblems().add("JWT non presente, impossibile autenticarsi sul servizio esterno");
    } else {
      try { 
        val serviceResponse = helpdeskServiceClient.config();
        response.getProblems().addAll(serviceResponse.getProblems());
        response.setResult(serviceResponse.getResult());
      } catch (FeignException e) {
        response.getProblems().add(e.toString());
      }
    }

    return response;
  }

}