package manager.attestati.service;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.gson.Gson;
import com.google.inject.Inject;

import manager.attestati.dto.drop.CancellazioneRigaAssenza;
import manager.attestati.dto.drop.CancellazioneRigaCompetenza;
import manager.attestati.dto.drop.CancellazioneRigaFormazione;
import manager.attestati.dto.insert.InserimentoRigaAssenza;
import manager.attestati.dto.insert.InserimentoRigaBuoniPasto;
import manager.attestati.dto.insert.InserimentoRigaCompetenza;
import manager.attestati.dto.insert.InserimentoRigaFormazione;
import manager.attestati.dto.show.RispostaAttestati;
import manager.attestati.dto.show.SeatCertification;

import models.Certification;
import models.Person;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

/**
 * Componente che si occupa di inviare e ricevere dati verso Nuovo Attestati.
 * @author alessandro
 *
 */
public class CertificationsComunication {

  //Attestati api
  private final static String BASE_URL = "http://as2dock.si.cnr.it";
  private final static String ATTESTATO_URL = "/api/ext/attestato";
  private final static String API_URL = "/api/ext";
  private final static String API_URL_ASSENZA = "/rigaAssenza";
  private final static String API_URL_BUONI_PASTO = "/rigaBuoniPasto";
  private final static String API_URL_FORMAZIONE = "/rigaFormazione";
  private final static String API_URL_COMPETENZA = "/rigaCompetenza";
  private final static String JSON_CONTENT_TYPE = "application/json";
  
  //OAuh
  private final static String OAUTH_CLIENT_SECRET = "mySecretOAuthSecret";
  private final static String OAUTH_CONTENT_TYPE = "application/x-www-form-urlencoded";
  private final static String OAUTH_URL = "/oauth/token";
  private final static String OAUTH_AUTHORIZATION = "YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ=";
  private final static String OAUTH_USERNAME = "app.epas";
  private final static String OAUTH_PASSWORD = "trapocolapuoicambiare";
  private final static String OAUTH_GRANT_TYPE = "password";
  private final static String OAUTH_CLIENT_ID= "attestatiapp";
  
  @Inject
  public CertificationsComunication() {
    
  }
  
  /**
   * Per l'ottenenere il Bearer Token:
   * curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" -H 
   * "Authorization: Basic YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ="  -d 'username=app.epas&password=.............
   * &grant_type=password&scope=read%20write&client_secret=mySecretOAuthSecret&client_id=attestatiapp' 
   * "http://as2dock.si.cnr.it/oauth/token"
   * @return
   */
  public Optional<String> getToken(){

    try {
      
      String body = String.format("username=%s&password=%s&grant_type=%s&client_secret=%s&client_id=%s", 
          OAUTH_USERNAME, OAUTH_PASSWORD, OAUTH_GRANT_TYPE, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_ID);
      
      WSRequest req = WS.url(BASE_URL + OAUTH_URL)
          .setHeader("Content-Type", OAUTH_CONTENT_TYPE)
          .setHeader("Authorization", "Basic "+ OAUTH_AUTHORIZATION)
          .body(body);
      HttpResponse response = req.post();
      Gson gson = new Gson();
      TokenDTO token = gson.fromJson(response.getJson(), TokenDTO.class);

      return Optional.fromNullable(token.access_token);
    } catch(Exception e) {
      return Optional.<String>absent();
    }
  }
  
  private Optional<String> reloadToken(Optional<String> token) {
    if (!token.isPresent()) {
      token = getToken();
      if (!token.isPresent()) {
        return Optional.<String>absent();
      }
    }
    return token;
  }
  
  /**
   * Costruisce una WSRequest predisposta alla comunicazione con le api attestati.
   * @param token
   * @param url
   * @param contentType
   * @return
   */
  private WSRequest prepareOAuthRequest(String token, String url, String contentType) {
    WSRequest wsRequest = WS.url( BASE_URL + url)
        .setHeader("Content-Type", contentType)
        .setHeader("Authorization", "Bearer "+ token);
    return wsRequest;
  }
  
  /**
   * curl -X GET -H "Authorization: Bearer cf24c413-9cf7-485d-a10b-87776e5659c7" 
   * -H "Content-Type: application/json" 
   * http://as2dock.si.cnr.it/api/ext/attestato/{{CODICESEDE}}/{{MATRICOLA}}/{{ANNO}}/{{MESE}}
   * @param person
   * @param month
   * @param year
   * @param token
   */
  public Optional<SeatCertification> getPersonSeatCertification(Person person, 
      int month, int year, Optional<String> token) {
   
    if (!reloadToken(token).isPresent()) {
      return Optional.<SeatCertification>absent();
    }

    try {
      String url = ATTESTATO_URL + "/" + person.office.codeId 
          + "/" + person.number + "/" + year + "/" + month;

      WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);
      HttpResponse httpResponse = wsRequest.get();

      SeatCertification seatCertification = 
          new Gson().fromJson(httpResponse.getJson(), SeatCertification.class);
      
      Verify.verify(seatCertification.dipendenti.get(0).matricola == person.number);
      
      return Optional.fromNullable(seatCertification);

    } catch (Exception ex) {}

    return Optional.<SeatCertification>absent();

  }
  
  /**
   * Conversione del json di risposta da attestati.
   * @param httpResponse
   * @return
   */
  public RispostaAttestati parseRispostaAttestati(HttpResponse httpResponse) {
    return new Gson().fromJson(httpResponse.getJson(), RispostaAttestati.class);
  }
  
  
  /**
   * Invia la riga di assenza ad attestati.
   * @param token
   * @param certification
   * @return
   */
  public HttpResponse sendRigaAssenza(Optional<String> token, Certification certification) {
    if (!reloadToken(token).isPresent()) {
      return null;
    }

    String url = API_URL + API_URL_ASSENZA;
    WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);

    InserimentoRigaAssenza riga = new InserimentoRigaAssenza(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.post();
  }
  
  /**
   * Invia la riga di assenza ad attestati.
   * @param token
   * @param certification
   * @return
   */
  public HttpResponse sendRigaBuoniPasto(Optional<String> token, Certification certification, boolean update) {
    if (!reloadToken(token).isPresent()) {
      return null;
    }

    String url = API_URL + API_URL_BUONI_PASTO;
    WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);

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
   * @param token
   * @param certification
   * @return
   */
  public HttpResponse sendRigaFormazione(Optional<String> token, Certification certification) {
    if (!reloadToken(token).isPresent()) {
      return null;
    }

    String url = API_URL + API_URL_FORMAZIONE;
    WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);

    InserimentoRigaFormazione riga = new InserimentoRigaFormazione(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.post();
  }
  
  /**
   * Invia la riga di assenza ad attestati.
   * @param token
   * @param certification
   * @return
   */
  public HttpResponse sendRigaCompetenza(Optional<String> token, Certification certification) {
    if (!reloadToken(token).isPresent()) {
      return null;
    }

    String url = API_URL + API_URL_COMPETENZA;
    WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);

    InserimentoRigaCompetenza riga = new InserimentoRigaCompetenza(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.post();
  }
  
  /**
   * Invia la riga di assenza ad attestati.
   * @param token
   * @param certification
   * @return
   */
  public HttpResponse deleteRigaAssenza(Optional<String> token, Certification certification) {
    if (!reloadToken(token).isPresent()) {
      return null;
    }

    String url = API_URL + API_URL_ASSENZA;
    WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);

    CancellazioneRigaAssenza rigaAssenza = new CancellazioneRigaAssenza(certification);
    String json = new Gson().toJson(rigaAssenza);
    wsRequest.body(json);

    return wsRequest.delete();
  }
  
//  /**
//   * Invia la riga di assenza ad attestati.
//   * @param token
//   * @param certification
//   * @return
//   */
//  public HttpResponse deleteRigaBuoniPasto(Optional<String> token, Certification certification) {
//    if (!reloadToken(token).isPresent()) {
//      return null;
//    }
//
//    String url = API_URL + API_URL_BUONI_PASTO;
//    WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);
//
//    CancellazioneRigaBuoniPasto riga = new CancellazioneRigaBuoniPasto(certification);
//    String json = new Gson().toJson(riga);
//    wsRequest.body(json);
//
//    return wsRequest.post();
//  }
  
  /**
   * Invia la riga di assenza ad attestati.
   * @param token
   * @param certification
   * @return
   */
  public HttpResponse deleteRigaFormazione(Optional<String> token, Certification certification) {
    if (!reloadToken(token).isPresent()) {
      return null;
    }

    String url = API_URL + API_URL_FORMAZIONE;
    WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);

    CancellazioneRigaFormazione riga = new CancellazioneRigaFormazione(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.delete();
  }
  
  /**
   * Invia la riga di assenza ad attestati.
   * @param token
   * @param certification
   * @return
   */
  public HttpResponse deleteRigaCompetenza(Optional<String> token, Certification certification) {
    if (!reloadToken(token).isPresent()) {
      return null;
    }

    String url = API_URL + API_URL_COMPETENZA;
    WSRequest wsRequest = prepareOAuthRequest(token.get(), url, JSON_CONTENT_TYPE);

    CancellazioneRigaCompetenza riga = new CancellazioneRigaCompetenza(certification);
    String json = new Gson().toJson(riga);
    wsRequest.body(json);

    return wsRequest.delete();
  }

}
