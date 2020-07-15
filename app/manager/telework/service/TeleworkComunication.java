package manager.telework.service;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import helpers.rest.ApiRequestException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.dto.show.ListaDipendenti;
import manager.attestati.service.AttestatiApis;
import models.Office;
import models.User;
import models.dto.TeleworkDto;
import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Http;

@Slf4j
public class TeleworkComunication {

  private static final String TELEWORK_API_URL = "/api/v1/stamping/";
  private static final String ERROR = "Missing required parameter: ";
  private static final String TELEWORK_BASE_URL = "telework.base";
  private static final String TELEWORK_PASS = "telework.pass";
  private static final String TELEWORK_USER = "telework.user";
  
  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final String POST_TIMEOUT = "5min";
  
//  private static final String SHOW = "/show";
//  private static final String UPDATE = "/update";
//  private static final String DELETE = "/delete";
//  private static final String LIST = "/list";
//  private static final String SAVE = "/save";
  
  
  /**
   * L'url base dell'applicazione Telework.
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


    final String url = TELEWORK_API_URL + "/" + stampingId;
        

    WSRequest wsRequest = prepareOAuthRequest(url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();

    // Caso di utente non autorizzato
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      
      log.error("Utente non autorizzato: {}", wsRequest.username);
      
    }

    TeleworkDto teleworkStamping= new Gson()
        .fromJson(httpResponse.getJson(), TeleworkDto.class);

    log.info("Recuperata lista delle timbrature in telelavoro ");

    return teleworkStamping;
  }
  
  /**
   * Preleva la lista delle timbrature in telelavoro relative ad uno specifico personDay.
   *
   * @param personDayId l'identificativo del personDay di cui recuperare la lista di 
   *     timbrature in telelavoro
   * @return La lista di timbrature in telelavoro.
   */
  public List<TeleworkDto> getList(long personDayId, User user)
      throws NoSuchFieldException, ExecutionException {


    final String url = TELEWORK_API_URL + "/" + personDayId;
        

    WSRequest wsRequest = prepareOAuthRequest(url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();

    // Caso di utente non autorizzato
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      
      log.error("Utente non autorizzato: {}", wsRequest.username);
      
    }

    List<TeleworkDto> teleworkStampings = new Gson()
        .fromJson(httpResponse.getJson(), List.class);

    log.info("Recuperata lista delle timbrature in telelavoro ");

    return teleworkStampings;
  }
  
  /**
   * Salva l'oggetto sull'applicazione Telework.
   * @param dto l'oggetto da andare a salvare 
   * @throws NoSuchFieldException eccezione di assenza di un campo nel metodo che crea il messaggio
   *     da inviare all'applicazione Telework.
   */
  public int save(TeleworkDto dto) throws NoSuchFieldException {
    
    final String url = TELEWORK_API_URL;
    WSRequest wsRequest = prepareOAuthRequest(url, JSON_CONTENT_TYPE);
    String json = new Gson().toJson(dto);
    
    wsRequest.body = json;
    HttpResponse httpResponse = wsRequest.post();
    
    // Caso di utente non autorizzato
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      
      log.error("Utente non autorizzato: {}", wsRequest.username);
      
    } else {
      log.info("Timbratura {} in telelavoro inserita correttamente", dto.toString());
    }
        
    return httpResponse.getStatus();
  }
  
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
  
  public int delete(long stampingId) throws NoSuchFieldException {
    final String url = TELEWORK_API_URL + "/" + stampingId;
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
