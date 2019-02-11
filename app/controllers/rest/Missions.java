package controllers.rest;

import com.google.common.base.Optional;

import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.Resecure.NoCheck;

import dao.OfficeDao;

import helpers.JsonResponse;

import it.cnr.iit.epas.JsonMissionBinder;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.MissionManager;
import manager.StampingManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;

import models.Office;
import models.exports.MissionFromClient;

import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;


@Slf4j
@With(Resecure.class)
public class Missions extends Controller {
  
  @Inject
  private static MissionManager missionManager;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static OfficeDao officeDao;
  
  private static void logInfo(String description, MissionFromClient body) {
    log.info("Integrazione Missioni: {}. Messaggio: {}", description, body);
  }
  
  private static void logWarn(String description, MissionFromClient body) {
    log.warn("Integrazione Missioni: {}. Messaggio: {}", description, body);
  }
  
  private static void logError(String description, MissionFromClient body) {
    log.error("Integrazione Missioni: {}. Messaggio: {}", description, body);
  }
  
  /**
   * metodo che processa il messaggio ricevuto dal kraken-listener.
   * @param body il dto costruito a partire dal binder
   */
  @BasicAuth
  public static void amqpreceiver(@As(binder = JsonMissionBinder.class)MissionFromClient body) {

    logInfo("Ricevuto messaggio", body);
    
    // Malformed Json (400)
    if (body == null || body.dataInizio == null || body.dataFine == null) {
      logWarn("Messaggio, dataInizio o dataFine vuoti, messaggio scartato", body);
      JsonResponse.badRequest();
    }
    
    if (body.dataInizio.isAfter(body.dataFine)) {
      logWarn("Data di inizio successiva alla data di fine, messaggio scartato", body);
      JsonResponse.badRequest();
    }
    
    // person not present (404)
    if (!missionManager.linkToPerson(body).isPresent()) {
      logWarn("Dipendente riferito nel messaggio non trovato, messaggio scartato", body);
      JsonResponse.notFound();
    }

    Optional<Office> office = officeDao.byCodeId(body.codiceSede + "");
    
    // Check if integration ePAS-Missions is enabled
    if (!(Boolean)configurationManager
        .configValue(office.get(), EpasParam.ENABLE_MISSIONS_INTEGRATION)) {
      logInfo(String.format("Non verrà processato il messaggio in quanto la sede %s "
          + "cui appartiene il destinatario %s ha l'integrazione con Missioni disabilitata",
          office.get().name, body.person.fullName()), body);
      JsonResponse.ok();
    }
    
    boolean success = false;
    switch (body.tipoMissione) {
      case "ORDINE":
        success = missionManager.createMissionFromClient(body, true); 
        break;
      case "RIMBORSO":
        success =  missionManager.manageMissionFromClient(body, true);
        break;
      case "ANNULLAMENTO":
        success = missionManager.deleteMissionFromClient(body, true);
        break;
      default:
        break;
    }
    
    if (success) {
      logInfo("Messaggio inserito con successo", body);
    } else {
      JsonResponse.conflict();
      logError("Non è stato possibile inserire il messaggio", body);        
    }

    // Success (200)
    JsonResponse.ok();
  }

}
