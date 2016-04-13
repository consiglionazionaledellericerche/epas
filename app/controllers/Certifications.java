package controllers;

import com.google.common.base.Optional;
import com.google.gson.Gson;

import dao.OfficeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

import lombok.extern.slf4j.Slf4j;

import manager.attestati.dto.SeatCertification;
import manager.attestati.dto.TokenDTO;

import models.Office;

import org.joda.time.YearMonth;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import javax.inject.Inject;

/**
 * Il controller per l'invio dei dati certificati al nuovo attestati.
 * @author alessandro
 *
 */
@Slf4j
@With({Resecure.class, RequestInit.class})
public class Certifications extends Controller {
  
  //Attestati api
  private final static String BASE_URL = "http://as2dock.si.cnr.it";
  private final static String ATTESTATO_URL = "/api/ext/attestato";
  private final static String API_URL = "/api/ext";
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
  
  //Test
  private final static int NUMBER = 9891;
  private final static int SEAT = 603240;
  
  @Inject
  private static SecurityRules rules;
  
  @Inject
  private static OfficeDao officeDao;
  
  @Inject
  private static IWrapperFactory factory;
  
  public static void newAttestati(Long officeId){
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    Optional<String> token = getToken();
    if (!token.isPresent()) {
      flash.error("Impossibile autenticarsi a attestati.");
      UploadSituation.uploadData(officeId);
    }
    
    IWrapperOffice wrOffice = factory.create(office);
    Optional<YearMonth> monthToUpload = wrOffice.nextYearMonthToUpload();
    
    render(wrOffice, monthToUpload, token);
  }
   
  /**
   * Per l'ottenenere il Bearer Token:
   * curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" -H 
   * "Authorization: Basic YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ="  -d 'username=app.epas&password=.............
   * &grant_type=password&scope=read%20write&client_secret=mySecretOAuthSecret&client_id=attestatiapp' 
   * "http://as2dock.si.cnr.it/oauth/token"
   * @return
   */
  private static Optional<String> getToken(){

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
  
  /**
   * curl -X GET -H "Authorization: Bearer cf24c413-9cf7-485d-a10b-87776e5659c7" 
   * -H "Content-Type: application/json" 
   * http://as2dock.si.cnr.it/api/ext/attestato/{{CODICESEDE}}/{{MATRICOLA}}/{{ANNO}}/{{MESE}}
   * @param office
   * @param month
   * @param year
   * @param token
   */
  public static void personCertificated(Office office, int month, int year, String token){
    
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    String url = ATTESTATO_URL + "/" + SEAT + "/" + NUMBER + "/" + year + "/" + month;
    
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();
    
    SeatCertification seatCertification = 
        new Gson().fromJson(httpResponse.getJson(), SeatCertification.class);
    
    renderText(seatCertification.toString());
    
  }
  
  
  /**
   * Costruisce una WSRequest predisposta alla comunicazione con le api attestati.
   * @param token
   * @param url
   * @param contentType
   * @return
   */
  private static WSRequest prepareOAuthRequest(String token, String url, String contentType) {
    
  
    WSRequest wsRequest = WS.url( BASE_URL + url)
        .setHeader("Content-Type", contentType)
        .setHeader("Authorization", "Bearer "+ token);
    return wsRequest;
    
  }
  
  
}
