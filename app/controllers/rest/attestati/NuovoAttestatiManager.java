package controllers.rest.attestati;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import cnr.sync.dto.DepartmentDto;

import lombok.extern.slf4j.Slf4j;

import org.assertj.core.util.Lists;

import play.Play;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.Scheme;
import play.libs.WS.WSRequest;
import play.mvc.results.RenderJson;


import sun.net.www.http.HttpClient;

@Slf4j
public class NuovoAttestatiManager {

  private static final String URL_BASE = "http://as2dock.si.cnr.it/oauth/token";

  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String AUTHORIZATION = "YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ=";
  private static final String USERNAME = "app.epas";
  private static final String PASSWORD = "trapocolapuoicambiare";
  private static final String GRANT_TYPE = "password";
  private static final String CLIENT_SECRET = "mySecretOAuthSecret";
  private static final String CLIENT_ID = "attestatiapp";
  
  /**
   * Per l'ottenenere il Bearer Token:
   * curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" -H 
   * "Authorization: Basic YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ="  -d 'username=app.epas&password=.............
   * &grant_type=password&scope=read%20write&client_secret=mySecretOAuthSecret&client_id=attestatiapp' 
   * "http://as2dock.si.cnr.it/oauth/token"
   * @return
   */
  public String getToken(){
    log.debug("Entrato nel metodo di richiesta token");
    
    WSRequest req = WS.url(URL_BASE)
        .setHeader("Content-Type", CONTENT_TYPE)
        .setHeader("Authorization", "Basic "+ AUTHORIZATION)
        .body(String.format("username= %s&password=%s&grant_type=%s&client_secret=%s&client_id=%s", 
            USERNAME, PASSWORD, GRANT_TYPE, CLIENT_SECRET, CLIENT_ID));
    HttpResponse response = req.post();
    Gson gson = new Gson();
    //log.info(response.getJson().toString());
    TokenDTO token = gson.fromJson(response.getJson(), TokenDTO.class);
    
    return token.access_token;
  }
  
  
  public int inserisciAssenza(String token, int month, int year, int sedeId){
    log.debug("inserisci assenza");
    AttestatiDTO attestati = new AttestatiDTO();
    attestati.anno = year+"";
    attestati.mese = month+"";
    attestati.sedeID = sedeId+"";
    attestati.dipendenti = Lists.newArrayList();
    WSRequest req = WS.url("http://as2dock.si.cnr.it/api/ext/rigaAssenza")
        .setHeader("Content-Type", "application/json")
        .oauth(null, token, "mySecretOAuthSecret");
    req.body(attestati);
    HttpResponse response = req.post();
    
    return response.getStatus();
  }
}
