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

package manager.telework.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.dto.TeleworkDto;
import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Http;

/**
 * Classe che si occupa di gestire le comunicazioni Rest con il
 * servizio di gestione delle timbrature in telelavoro.
 */
@Slf4j
public class TeleworkComunication {

  private static final String TELEWORK_API_URL = "/api/v1/stamping/";
  private static final String ERROR = "Missing required parameter: ";
  private static final String TELEWORK_BASE_URL = "telework.base";
  private static final String TELEWORK_PASS = "telework.pass";
  private static final String TELEWORK_USER = "telework.user";
  
  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final String POST_TIMEOUT = "5min";
  private static final String LIST = "list/";

  @Inject
  GsonBuilder gsonBuilder;

  /**
   * L'url base dell'applicazione Telework.
   *
   * @return l'url base dell'applicazione.
   * @throws NoSuchFieldException eccezione su mancanza del campo
   */
  private String getTeleworkBaseUrl() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(TELEWORK_BASE_URL))) {
      throw new NoSuchFieldException(ERROR + TELEWORK_BASE_URL);
    }
    return Play.configuration.getProperty(TELEWORK_BASE_URL);
  }

  /**
   * L'user che si collega all'applicazione Telework.
   *
   * @return l'user che si collega all'applicazione Telework.
   * @throws NoSuchFieldException eccezione su mancanza del campo
   */
  private String getTeleworkUser() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(TELEWORK_USER))) {
      throw new NoSuchFieldException(ERROR + TELEWORK_USER);
    }
    return Play.configuration.getProperty(TELEWORK_USER);
  }

  /**
   * La password con cui l'utente si collega all'applicazione Telework.
   *
   * @return la password con cui l'utente si collega all'applicazione Telework.
   * @throws NoSuchFieldException eccezione su mancanza del campo
   */
  private String getTeleworkPass() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(TELEWORK_PASS))) {
      throw new NoSuchFieldException(ERROR + TELEWORK_PASS);
    }
    return Play.configuration.getProperty(TELEWORK_PASS);
  }

  /**
   * Preleva la timbratura in telelavoro relativa ad uno specifico id.
   *
   * @param stampingId l'identificativo della timbratura in telelavoro da recuperare
   * @return La timbratura in telelavoro.
   */
  public TeleworkDto get(long stampingId)
      throws NoSuchFieldException, ExecutionException {

    final String url = TELEWORK_API_URL + stampingId;        

    WSRequest wsRequest = prepareOAuthRequest(url, JSON_CONTENT_TYPE);
    wsRequest.setParameter("id", stampingId);
    HttpResponse httpResponse = wsRequest.get();

    // Caso di utente non autorizzato
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {      
      log.error("Utente non autorizzato: {}", wsRequest.username);      
    }

    val gson = gsonBuilder.create();
    TeleworkDto teleworkStamping = 
        gson.fromJson(httpResponse.getJson(), TeleworkDto.class);

    log.trace("Recuperata lista delle timbrature in telelavoro ");

    return teleworkStamping;
  }

  /**
   * Preleva la lista delle timbrature in telelavoro relative ad uno specifico personDay.
   *
   * @param personDayId l'identificativo del personDay di cui recuperare la lista di 
   *     timbrature in telelavoro
   * @return La lista di timbrature in telelavoro.
   */
  public List<TeleworkDto> getList(long personDayId)
      throws NoSuchFieldException, ExecutionException {

    final String url = TELEWORK_API_URL + LIST;        
    HttpResponse httpResponse;
    WSRequest wsRequest = prepareOAuthRequest(url, JSON_CONTENT_TYPE);
    wsRequest.setParameter("personDayId", personDayId);
    try {
      httpResponse = wsRequest.get();  
    } catch (Exception ex) {
      log.warn("Applicazione telework-stamping non risponde.");
      return Lists.newArrayList();
    }

    // Caso di utente non autorizzato
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Utente non autorizzato: {}", wsRequest.username);
    }

    if (httpResponse.getJson().isJsonArray() 
        && httpResponse.getJson().getAsJsonArray().size() == 0) {
      log.trace("httpResponse.json = {}", httpResponse.getJson());
    } else {
      log.debug("httpResponse.json = {}", httpResponse.getJson());
    }

    val gson = gsonBuilder.create();
    List<TeleworkDto> teleworkStampings = 
        gson.fromJson(
            httpResponse.getJson(),
            new TypeToken<List<TeleworkDto>>() {}.getType());

    log.trace("Recuperata lista delle timbrature in telelavoro ");

    return teleworkStampings;
  }

  /**
   * Salva l'oggetto sull'applicazione Telework.
   *
   * @param dto l'oggetto da andare a salvare 
   * @throws NoSuchFieldException eccezione di assenza di un campo nel metodo che crea il messaggio
   *     da inviare all'applicazione Telework.
   */
  public int save(TeleworkDto dto) throws NoSuchFieldException {

    final String url = TELEWORK_API_URL;
    WSRequest wsRequest = prepareOAuthRequest(url, JSON_CONTENT_TYPE);
    val gson = gsonBuilder.create();
    String json = gson.toJson(dto);

    wsRequest.body = json;
    HttpResponse httpResponse = wsRequest.post();
    
    // Caso di utente non autorizzato
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Utente non autorizzato: {}", wsRequest.username);
    } else if (httpResponse.getStatus() == Http.StatusCode.INTERNAL_ERROR) {
      log.error("Errore nella procedura di inserimento della timbratura su sistema esterno");
    } else if (httpResponse.getStatus() == Http.StatusCode.BAD_REQUEST
        || httpResponse.getStatus() == Http.StatusCode.NOT_FOUND) {
      log.error("Parametri passati non corretti o malformati");
    } else {
      log.info("Timbratura {} in telelavoro inserita correttamente", dto.toString());
    }

    return httpResponse.getStatus();
  }

  /**
   * Comunica con la applicazione telework-stamping la richiesta di modifica di una 
   * timbratura in telelavoro.
   *
   * @param dto l'oggetto dto da inviare per permettere la modifica della timbratura relativa
   * @return il risultato dell'operazione.
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   */
  public int update(TeleworkDto dto) throws NoSuchFieldException {
    final String url = TELEWORK_API_URL;
    WSRequest wsRequest = prepareOAuthRequest(url, JSON_CONTENT_TYPE);
    wsRequest.body = dto;
    HttpResponse httpResponse = wsRequest.put();

    // Caso di utente non autorizzato
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {      
      log.error("Utente non autorizzato: {}", wsRequest.username);      
    } else {
      log.info("Timbratura {} in telelavoro modificata correttamente", dto.toString());
    }
        
    return httpResponse.getStatus();
  }

  /**
   * Metodo che cancella la timbratura su applicazione esterna.
   *
   * @param stampingId l'identificativo della timbratura da eliminare
   * @return 204 se l'eliminazione è andata a buon fine
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   */
  public int delete(long stampingId) throws NoSuchFieldException {
    final String url = TELEWORK_API_URL + stampingId;
    WSRequest wsRequest = prepareOAuthRequest(url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.delete();
    // Caso di utente non autorizzato
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {      
      log.error("Utente non autorizzato: {}", wsRequest.username);      
    } else {
      log.info("Timbratura {} in telelavoro eliminata correttamente", stampingId);
    }
        
    return httpResponse.getStatus();
  }
  
  /**
   * Costruisce una WSRequest predisposta alla comunicazione con le api Telework.
   *
   * @param url url
   * @param contentType contentType
   */
  private WSRequest prepareOAuthRequest(String url, String contentType)
      throws NoSuchFieldException {

    final String baseUrl = getTeleworkBaseUrl();
    final String user = getTeleworkUser();
    final String password = getTeleworkPass();
    WSRequest wsRequest = WS.url(baseUrl + url)
        .setHeader("Content-Type", contentType);
    wsRequest.authenticate(user, password);        

    wsRequest.timeout(POST_TIMEOUT);
    return wsRequest;
  }
}
