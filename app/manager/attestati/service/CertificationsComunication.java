package manager.attestati.service;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import helpers.CacheValues;
import helpers.rest.ApiRequestException;

import lombok.extern.slf4j.Slf4j;

import manager.attestati.dto.drop.CancellazioneRigaAssenza;
import manager.attestati.dto.drop.CancellazioneRigaCompetenza;
import manager.attestati.dto.drop.CancellazioneRigaFormazione;
import manager.attestati.dto.insert.InserimentoRigaAssenza;
import manager.attestati.dto.insert.InserimentoRigaBuoniPasto;
import manager.attestati.dto.insert.InserimentoRigaCompetenza;
import manager.attestati.dto.insert.InserimentoRigaFormazione;
import manager.attestati.dto.show.CodiceAssenza;
import manager.attestati.dto.show.ListaDipendenti;
import manager.attestati.dto.show.RispostaAttestati;
import manager.attestati.dto.show.SeatCertification;

import models.Certification;
import models.Office;
import models.Person;

import edu.emory.mathcs.backport.java.util.Arrays;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Componente che si occupa di inviare e ricevere dati verso Nuovo Attestati.
 *
 * @author alessandro
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

  private static final String JSON_CONTENT_TYPE = "application/json";

  //OAuh
  private static final String OAUTH_CLIENT_SECRET = "mySecretOAuthSecret";
  private static final String OAUTH_CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String OAUTH_URL = "/oauth/token";
  private static final String OAUTH_AUTHORIZATION = "YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ=";
  private static final String OAUTH_GRANT_TYPE = "password";
  private static final String OAUTH_CLIENT_ID = "attestatiapp";

  private static final String OAUTH_TOKEN = "oauth.token.attestati";


  /**
   * Per l'ottenenere il Bearer Token:
   * curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded"
   * -H "Authorization: Basic YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ="
   * -d 'username=app.epas&password=.............
   * &grant_type=password&scope=read%20write
   * &client_secret=mySecretOAuthSecret&client_id=attestatiapp'
   * "http://as2dock.si.cnr.it/oauth/token"
   *
   * @return il token
   */
  public OauthToken getToken() {

    final String url;
    final String user;
    final String pass;

    try {
      url = AttestatiApis.getAttestatiBaseUrl();
      user = AttestatiApis.getAttestatiUser();
      pass = AttestatiApis.getAttestatiPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }


    final Map<String, String> parameters = new HashMap<>();
    parameters.put("username", user);
    parameters.put("password", pass);
    parameters.put("grant_type", OAUTH_GRANT_TYPE);
    parameters.put("client_secret", OAUTH_CLIENT_SECRET);
    parameters.put("client_id", OAUTH_CLIENT_ID);

    WSRequest req = WS.url(url + OAUTH_URL)
        .setHeader("Content-Type", OAUTH_CONTENT_TYPE)
        .setHeader("Authorization", "Basic " + OAUTH_AUTHORIZATION)
        .setParameters(parameters);

    HttpResponse response = req.post();
    Gson gson = new Gson();
    OauthToken accessToken = gson.fromJson(response.getJson(), OauthToken.class);

    log.debug("Prelevato token oauth dal server degli attestati");
    return accessToken;
  }


  /**
   * Costruisce una WSRequest predisposta alla comunicazione con le api attestati.
   *
   * @param token       token
   * @param url         url
   * @param contentType contentType
   */
  private WSRequest prepareOAuthRequest(String token, String url, String contentType) {

    final String baseUrl;

    try {
      baseUrl = AttestatiApis.getAttestatiBaseUrl();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    WSRequest wsRequest = WS.url(baseUrl + url)
        .setHeader("Content-Type", contentType)
        .setHeader("Authorization", "Bearer " + token);
    return wsRequest;
  }

  /**
   * Preleva la lista delle matricole da attestati.
   *
   * @param office sede
   * @param year   anno
   * @param month  mese
   * @return insieme di numbers
   */
  public Set<Integer> getPeopleList(Office office, int year, int month) throws ExecutionException {

    String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      CacheValues.oauthToken.invalidateAll();
      token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    }

    final String url = API_URL + API_URL_LISTA_DIPENDENTI + "/" + office.codeId
        + "/" + year + "/" + month;

    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();

    JsonObject body = httpResponse.getJson().getAsJsonObject();

    if (body.has("error") && "invalid_token".equals(body.get("error").getAsString())) {
      CacheValues.oauthToken.invalidateAll();
      throw new IllegalAccessError("Invalid Token: " + token);
    }
    if (body.has("message")) {
      throw new ApiRequestException(body.get("message").getAsString());
    }

    ListaDipendenti listaDipendenti = new Gson().fromJson(body, ListaDipendenti.class);

    log.info("Recuperata lista delle matricole da attestati per l'ufficio {} -  mese {}/{}",
        office, month, year);

    return listaDipendenti.dipendenti.stream().map(matricola -> matricola.matricola)
        .collect(Collectors.toSet());
  }

  /**
   * curl -X GET -H "Authorization: Bearer cf24c413-9cf7-485d-a10b-87776e5659c7"
   * -H "Content-Type: application/json"
   * http://as2dock.si.cnr.it/api/ext/attestato/{{CODICESEDE}}/{{MATRICOLA}}/{{ANNO}}/{{MESE}}
   *
   * @param person persona
   * @param month  mese
   * @param year   anno
   */
  public Optional<SeatCertification> getPersonSeatCertification(Person person,
      int month, int year) throws ExecutionException {

    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return Optional.absent();
    }

    try {
      String url = ATTESTATI_API_URL + "/" + person.office.codeId
          + "/" + person.number + "/" + year + "/" + month;

      WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
      HttpResponse httpResponse = wsRequest.get();

      final JsonObject body = httpResponse.getJson().getAsJsonObject();

      if (body.has("error") && "invalid_token".equals(body.get("error").getAsString())) {
        CacheValues.oauthToken.invalidateAll();
        throw new Exception("Invalid Token: " + token);
      }

      SeatCertification seatCertification = new Gson().fromJson(body, SeatCertification.class);

      Verify.verify(seatCertification.dipendenti.get(0).matricola == person.number);

      return Optional.of(seatCertification);

    } catch (Exception ex) {
      log.error("Errore di comunicazione col server degli Attestati {}", ex.getMessage());
    }

    return Optional.<SeatCertification>absent();

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
  public HttpResponse sendRigaAssenza(Certification certification) throws ExecutionException {

    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
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
      boolean update) throws ExecutionException {

    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return null;
    }

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
  public HttpResponse sendRigaFormazione(Certification certification) throws ExecutionException {
    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return null;
    }

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
  public HttpResponse sendRigaCompetenza(Certification certification) throws ExecutionException {
    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return null;
    }

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
  public HttpResponse deleteRigaAssenza(Certification certification) throws ExecutionException {
    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return null;
    }

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
  public HttpResponse deleteRigaFormazione(Certification certification) throws ExecutionException {
    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return null;
    }
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
  public HttpResponse deleteRigaCompetenza(Certification certification) throws ExecutionException {
    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return null;
    }

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

    final String token = CacheValues.oauthToken.get(OAUTH_TOKEN).access_token;
    if (token == null) {
      return Lists.newArrayList();
    }

    try {
      String url = API_URL + API_URL_ASSENZE_PER_CONTRATTO + "/" + "CL0609";

      WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
      HttpResponse httpResponse = wsRequest.get();


      JsonArray json = httpResponse.getJson().getAsJsonArray();

//      // TODO nel caso di risposta corretta il server restituisce un JsonArray,
//      // nel caso di token non valido un JsonObject, implementare i controlli necessari
//      final JsonObject body = httpResponse.getJson().getAsJsonObject();
//      if (body.has("error") && "invalid_token".equals(body.get("error").getAsString())) {
//        CacheValues.oauthToken.invalidateAll();
//        throw new Exception("Invalid Token: " + token);
//      }

      final CodiceAssenza[] lista = new Gson().fromJson(json, CodiceAssenza[].class);

      return Arrays.asList(lista);

    } catch (Exception ex) {
      log.error("Errore di comunicazione col server degli Attestati {}", ex.getMessage());
    }
    return Lists.newArrayList();
  }

}
