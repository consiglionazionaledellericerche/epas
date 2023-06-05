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

package manager.attestati.service;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import helpers.CacheValues;
import helpers.rest.ApiRequestException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.dto.drop.CancellazioneRigaAssenza;
import manager.attestati.dto.drop.CancellazioneRigaCompetenza;
import manager.attestati.dto.drop.CancellazioneRigaFormazione;
import manager.attestati.dto.insert.InserimentoRigaAssenza;
import manager.attestati.dto.insert.InserimentoRigaBuoniPasto;
import manager.attestati.dto.insert.InserimentoRigaCompetenza;
import manager.attestati.dto.insert.InserimentoRigaFormazione;
import manager.attestati.dto.internal.CruscottoDipendente;
import manager.attestati.dto.internal.PeriodoDipendente;
import manager.attestati.dto.internal.StatoAttestatoMese;
import manager.attestati.dto.internal.TipoBlocchettoSede;
import manager.attestati.dto.show.CodiceAssenza;
import manager.attestati.dto.show.ListaDipendenti;
import manager.attestati.dto.show.RispostaAttestati;
import manager.attestati.dto.show.SeatCertification;
import models.Certification;
import models.Office;
import models.Person;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Http;

/**
 * Componente che si occupa di inviare e ricevere dati verso Nuovo Attestati.
 *
 * @author Alessandro Martelli
 */
@Slf4j
public class CertificationsComunication {

  //Attestati api
  private static final String ATTESTATI_API_URL = "/api/ext/attestato";
  private static final String API_URL = "/api/ext";
  private static final String API_URL_LISTA_DIPENDENTI = "/listaDipendenti";
  private static final String API_URL_ASSENZA = "/rigaAssenza";
  private static final String API_URL_BUONI_PASTO = "/rigaBuoniPasto";
  private static final String API_URL_FORMAZIONE = "/rigaFormazione";
  private static final String API_URL_COMPETENZA = "/rigaCompetenza";

  private static final String API_URL_ASSENZE_PER_CONTRATTO = "/contratto/codiciAssenza";
  //http://attestativ2.rm.cnr.it/api/ext/contratto/codiciAssenza/{CODICE_CONTRATTO}

  //Attestati api internal
  private static final String API_URL_INT = "/api/rest";
  private static final String API_INT_STATO_ATTESTATO_MESE = "/sede/listaDipendenti";
  private static final String API_INT_PERIODO_DIPENDENTE = "/dipendente/periodo";  // /145872
  private static final String API_INT_CRUSCOTTO = "/dipendente/stato/cruscotto";   // /11028/2017"
  
  private static final String JSON_CONTENT_TYPE = "application/json";

  //OAuth
  private static final String OAUTH_CLIENT_SECRET = "mySecretOAuthSecret";
  private static final String OAUTH_CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String OAUTH_URL = "/oauth/token";
  private static final String OAUTH_AUTHORIZATION = "YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ=";
  private static final String TOKEN_GRANT_TYPE = "password";
  private static final String REFRESHTOKEN_GRANT_TYPE = "refresh_token";

  private static final String OAUTH_CLIENT_ID = "attestatiapp";

  private static final String OAUTH_TOKEN = "oauth.token.attestati";

  private static final String POST_TIMEOUT = "5min";

  @Inject
  private CacheValues cacheValues;

  /**
   * Per l'ottenenere il Bearer Token: curl -s -X POST -H "Content-Type:
   * application/x-www-form-urlencoded" 
   * -H "Authorization: Basic YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ="
   * -d 'username=app.epas&password=............. &grant_type=password&scope=read%20write
   * &client_secret=mySecretOAuthSecret&client_id=attestatiapp' "http://as2dock.si.cnr.it/oauth/token"
   *
   * @return il token
   */
  public OauthToken getToken() throws NoSuchFieldException {

    final String url = AttestatiApis.getAttestatiBaseUrl();
    final String user = AttestatiApis.getAttestatiUser();
    final String pass = AttestatiApis.getAttestatiPass();

    final Map<String, String> parameters = new HashMap<>();
    parameters.put("username", user);
    parameters.put("password", pass);
    parameters.put("grant_type", TOKEN_GRANT_TYPE);
    parameters.put("client_secret", OAUTH_CLIENT_SECRET);
    parameters.put("client_id", OAUTH_CLIENT_ID);

    WSRequest req = WS.url(url + OAUTH_URL)
        .setHeader("Content-Type", OAUTH_CONTENT_TYPE)
        .setHeader("Authorization", "Basic " + OAUTH_AUTHORIZATION)
        .setParameters(parameters);

    HttpResponse response = req.post();

    if (response.getStatus() != Http.StatusCode.OK) {
      log.warn("Errore durante la richiesta del Token Oauth: {}; {}",
          response.getStatus(), response.getString());
      throw new ApiRequestException("Impossibile ottenere Token Oauth dal server");
    }

    OauthToken accessToken = new Gson().fromJson(response.getJson(), OauthToken.class);

    log.info("Ottenuto access-token dal server degli attestati: {}", response.getString());
    return accessToken;
  }

  /**
   * Nuovo token con validità estesa.
   *
   * @param token token precedente (già ottenuto dal server).
   * @return Un nuovo token Oauth con validità estesa
   */
  public OauthToken refreshToken(OauthToken token) throws NoSuchFieldException {

    final String url = AttestatiApis.getAttestatiBaseUrl();

    final Map<String, String> parameters = new HashMap<>();
    parameters.put("grant_type", REFRESHTOKEN_GRANT_TYPE);
    parameters.put("client_secret", OAUTH_CLIENT_SECRET);
    parameters.put("client_id", OAUTH_CLIENT_ID);
    parameters.put("refresh_token", token.refresh_token);

    WSRequest req = WS.url(url + OAUTH_URL)
        .setHeader("Content-Type", OAUTH_CONTENT_TYPE)
        .setHeader("Authorization", "Basic " + OAUTH_AUTHORIZATION)
        .setParameters(parameters);

    log.info("Invio richiesta Refresh-Token ad attestati. Token precedente: {}", token
        .refresh_token);
    HttpResponse response = req.post();

    if (response.getStatus() != Http.StatusCode.OK) {
      log.warn("Errore durante la richiesta del Refresh-Token Oauth: {}; {}",
          response.getStatus(), response.getString());
      // Data l'inaffidabilita' del refresh-token dato da attestati (scade dopo non si sa quanto
      // tempo e in maniera totalmente scorrelata dalla durata dell'access-token attraverso il quale
      // viene fornito) nel caso la risposta sia negativa effettuo una richiesta per un nuovo
      // access token
      return getToken();
    }

    OauthToken accessToken = new Gson().fromJson(response.getJson(), OauthToken.class);

    log.info("Ottenuto refresh-token oauth dal server degli attestati: {}", response.getString());
    return accessToken;
  }


  /**
   * Costruisce una WSRequest predisposta alla comunicazione con le api attestati.
   *
   * @param token token
   * @param url url
   * @param contentType contentType
   */
  private WSRequest prepareOAuthRequest(String token, String url, String contentType)
      throws NoSuchFieldException {

    final String baseUrl = AttestatiApis.getAttestatiBaseUrl();

    WSRequest wsRequest = WS.url(baseUrl + url)
        .setHeader("Content-Type", contentType)
        .setHeader("Authorization", "Bearer " + token);

    wsRequest.timeout(POST_TIMEOUT);
    return wsRequest;
  }

  /**
   * Preleva la lista delle matricole da attestati.
   *
   * @param office sede
   * @param year anno
   * @param month mese
   * @return L'insieme delle matricole relative alla sede richiesta
   */
  public Set<String> getPeopleList(Office office, int year, int month)
      throws NoSuchFieldException, ExecutionException {

    String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    final String url = API_URL + API_URL_LISTA_DIPENDENTI + "/" + office.getCodeId()
        + "/" + year + "/" + month;

    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();

    // Caso di token non valido
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      cacheValues.oauthToken.invalidateAll();
      log.error("Token Oauth non valido: {}", token);
      throw new ApiRequestException("Invalid token");
    }

    ListaDipendenti listaDipendenti = new Gson()
        .fromJson(httpResponse.getJson(), ListaDipendenti.class);

    log.info("Recuperata lista delle matricole da attestati per l'ufficio {} -  mese {}/{}",
        office, month, year);

    return listaDipendenti.dipendenti.stream().map(matricola -> matricola.matricola)
        .collect(Collectors.toSet());
  }

  /**
   * curl -X GET -H "Authorization: Bearer cf24c413-9cf7-485d-a10b-87776e5659c7" -H "Content-Type:
   * application/json" http://as2dock.si.cnr.it/api/ext/attestato/{{CODICESEDE}}/{{MATRICOLA}}/{{ANNO}}/{{MESE}}
   *
   * @param person persona
   * @param month mese
   * @param year anno
   */
  public Optional<SeatCertification> getPersonSeatCertification(Person person,
      int month, int year) throws ExecutionException {

    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return Optional.absent();
    }

    try {
      String url = ATTESTATI_API_URL + "/" + person.getOffice().getCodeId()
          + "/" + person.getNumber() + "/" + year + "/" + month;

      WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
      HttpResponse httpResponse = wsRequest.get();

      // Caso di token non valido
      if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
        cacheValues.oauthToken.invalidateAll();
        log.error("Token Oauth non valido: {}", token);
        throw new ApiRequestException("Invalid token");
      }

      SeatCertification seatCertification =
          new Gson().fromJson(httpResponse.getJson(), SeatCertification.class);

      Verify.verify(Objects.equals(seatCertification.dipendenti.get(0).matricola, 
          person.getNumber()));

      return Optional.of(seatCertification);

    } catch (Exception ex) {
      log.error("Errore di comunicazione col server degli Attestati {}", ex.getMessage());
    }

    return Optional.absent();

  }

  /**
   * Conversione del json di risposta da attestati.
   *
   * @param httpResponse risposta
   * @return rispostaAttestati
   */
  Optional<RispostaAttestati> parseRispostaAttestati(HttpResponse httpResponse) {
    try {
      return Optional.fromNullable(new Gson()
          .fromJson(httpResponse.getJson(), RispostaAttestati.class));
    } catch (Exception ex) {
      return Optional.absent();
    }
  }


  /**
   * Invia la riga di assenza ad attestati.
   *
   * @param certification attestato
   * @return risposta
   */
  public HttpResponse sendRigaAssenza(Certification certification)
      throws ExecutionException, NoSuchFieldException {

    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return null;
    }

    String url = API_URL + API_URL_ASSENZA;
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);

    InserimentoRigaAssenza riga = new InserimentoRigaAssenza(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.post();
  }

  /**
   * Invia la riga di assenza ad attestati.
   *
   * @param certification attestato
   * @return risposta
   */
  public HttpResponse sendRigaBuoniPasto(Certification certification,
      boolean update) throws ExecutionException, NoSuchFieldException {

    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    String url = API_URL + API_URL_BUONI_PASTO;
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);

    InserimentoRigaBuoniPasto riga = new InserimentoRigaBuoniPasto(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    if (update) {
      return wsRequest.put();
    }
    return wsRequest.post();
  }

  /**
   * Invia la riga di assenza ad attestati.
   *
   * @param certification attestato
   * @return risposta
   */
  public HttpResponse sendRigaFormazione(Certification certification)
      throws ExecutionException, NoSuchFieldException {
    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    String url = API_URL + API_URL_FORMAZIONE;
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);

    InserimentoRigaFormazione riga = new InserimentoRigaFormazione(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.post();
  }

  /**
   * Invia la riga di assenza ad attestati.
   *
   * @param certification attestato
   * @return risposta
   */
  public HttpResponse sendRigaCompetenza(Certification certification)
      throws ExecutionException, NoSuchFieldException {
    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    String url = API_URL + API_URL_COMPETENZA;
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);

    InserimentoRigaCompetenza riga = new InserimentoRigaCompetenza(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.post();
  }

  /**
   * Invia la riga di assenza ad attestati.
   *
   * @param certification attestato
   * @return risposta
   */
  public HttpResponse deleteRigaAssenza(Certification certification)
      throws ExecutionException, NoSuchFieldException {
    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    String url = API_URL + API_URL_ASSENZA;
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);

    CancellazioneRigaAssenza rigaAssenza = new CancellazioneRigaAssenza(certification);
    String json = new Gson().toJson(rigaAssenza);
    wsRequest.body(json);

    return wsRequest.delete();
  }

  /**
   * Invia la riga di assenza ad attestati.
   *
   * @param certification attestato
   * @return risposta
   */
  public HttpResponse deleteRigaFormazione(Certification certification)
      throws ExecutionException, NoSuchFieldException {
    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    String url = API_URL + API_URL_FORMAZIONE;
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);

    CancellazioneRigaFormazione riga = new CancellazioneRigaFormazione(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.delete();
  }

  /**
   * Invia la riga di assenza ad attestati.
   *
   * @param certification attestato
   * @return risposta
   */
  public HttpResponse deleteRigaCompetenza(Certification certification)
      throws ExecutionException, NoSuchFieldException {
    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    String url = API_URL + API_URL_COMPETENZA;
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);

    CancellazioneRigaCompetenza riga = new CancellazioneRigaCompetenza(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.delete();
  }

  /**
   * Preleva da attestati la lista dei codici assenza (per il tipo contratto CL0609).
   *
   * @return lista dei codici assenza
   */
  public List<CodiceAssenza> getAbsencesList() throws ExecutionException {

    final String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return Lists.newArrayList();
    }

    try {
      String url = API_URL + API_URL_ASSENZE_PER_CONTRATTO + "/" + "CL0609";

      WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
      HttpResponse httpResponse = wsRequest.get();

      // Caso di token non valido
      if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
        cacheValues.oauthToken.invalidateAll();
        log.error("Token Oauth non valido: {}", token);
        throw new ApiRequestException("Invalid token");
      }

      final CodiceAssenza[] lista = new Gson().fromJson(httpResponse.getJson(),
          CodiceAssenza[].class);

      return Arrays.asList(lista);

    } catch (Exception ex) {
      log.error("Errore di comunicazione col server degli Attestati {}", ex.getMessage());
    }
    return Lists.newArrayList();
  }

  /**
   * Preleva la lista sui periodi di attività dei dipendenti in attestati, per sede anno e mese
   * selezionati.
   *
   * @param office sede
   * @param year anno
   * @param month mese
   * @return i contratti attivi nel mese dipendenti
   */
  public List<StatoAttestatoMese> getStatoAttestatoMese(Office office, int year, int month)
      throws NoSuchFieldException, ExecutionException {

    String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    final String url = API_URL_INT + API_INT_STATO_ATTESTATO_MESE + "/" + office.getCodeId()
        + "/" + year + "/" + month;

    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();

    // Caso di token non valido
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      cacheValues.oauthToken.invalidateAll();
      log.error("Token Oauth non valido: {}", token);
      throw new ApiRequestException("Invalid token");
    }

    StatoAttestatoMese[] statoAttestatiArray = new Gson()
        .fromJson(httpResponse.getJson(), StatoAttestatoMese[].class);

    log.info("Recuperata lista sullo stato attestati meseper l'ufficio {} -  mese {}/{}",
        office, month, year);

    return Arrays.asList(statoAttestatiArray);
  }


  /**
   * Preleva il periodo dipendente con quell'id.
   *
   * @param periodoId il periodoId
   * @return periodoDipendente
   */
  public PeriodoDipendente getPeriodoDipendente(int periodoId)
      throws NoSuchFieldException, ExecutionException {

    String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    final String url = API_URL_INT + API_INT_PERIODO_DIPENDENTE + "/" + periodoId;

    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();

    // Caso di token non valido
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      cacheValues.oauthToken.invalidateAll();
      log.error("Token Oauth non valido: {}", token);
      throw new ApiRequestException("Invalid token");
    }

    PeriodoDipendente periodoDipendente = new Gson()
        .fromJson(httpResponse.getJson(), PeriodoDipendente.class);

    log.info("Recuperato il PeriodoDipendente con id  {}", periodoId);

    return periodoDipendente;
  }

  /**
   * Preleva il cruscotto annuale del dipendente.
   *
   * @param dipendenteId il dipendenteId
   * @return cruscottoDipendente
   */
  public CruscottoDipendente getCruscotto(int dipendenteId, int year)
      throws NoSuchFieldException, ExecutionException {

    String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;

    final String url = API_URL_INT + API_INT_CRUSCOTTO + "/" + dipendenteId + "/" + year;

    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();

    // Caso di token non valido
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      cacheValues.oauthToken.invalidateAll();
      log.error("Token Oauth non valido: {}", token);
      throw new ApiRequestException("Invalid token");
    }

    CruscottoDipendente cruscottoDipendente = new Gson()
        .fromJson(httpResponse.getJson(), CruscottoDipendente.class);

    log.info("Recuperato il CruscottoDipendente con id  {} e anno {}", dipendenteId, year);

    return cruscottoDipendente;
  }
  
  /**
   * Metodo rest per chiedere il tipo di blocchetti di buono pasto usati nella sede.
   *
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param office la sede di riferimento
   * @return un oggetto contenente il tipo di blocchetto usato nella sede.
   */
  public TipoBlocchettoSede getTipoBlocchetto(int year, int month, Office office) 
      throws ExecutionException, NoSuchFieldException {
    
    String token = cacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    
    final String url = ATTESTATI_API_URL + "/sede" + "/" + office.getCodeId()
        + "/" + year + "/" + month;
    
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();
    
    // Caso di token non valido
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      cacheValues.oauthToken.invalidateAll();
      log.error("Token Oauth non valido: {}", token);
      throw new ApiRequestException("Invalid token");      
    }
    TipoBlocchettoSede tipoBlocchetto = new Gson()
        .fromJson(httpResponse.getJson(), TipoBlocchettoSede.class);
    
    log.info("Recuperato la tipologia di blocchetto utilizzato per la sede", office.getName());
    
    return tipoBlocchetto;
  }

}
