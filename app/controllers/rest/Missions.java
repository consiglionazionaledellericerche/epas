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
  
  /**
   * metodo che processa il messaggio ricevuto dal kraken-listener.
   * @param body il dto costruito a partire dal binder
   */
  @BasicAuth
  public static void amqpreceiver(@As(binder = JsonMissionBinder.class) MissionFromClient body) {

    log.info("Arrivato messaggio da {} ", body);
    // Malformed Json (400)
    if (body == null || body.dataInizio == null || body.dataFine == null) {
      JsonResponse.badRequest();
    }
    
    if (body.dataInizio.isAfter(body.dataFine)) {
      JsonResponse.badRequest();
      return;
    }

    //log.info("Arrivato {} ", body.toString());
    Optional<Office> office = officeDao.byCodeId(body.codiceSede + "");
    
    // Check if integration ePAS-Missions is enabled
    if (!(Boolean)configurationManager
        .configValue(office.get(), EpasParam.ENABLE_MISSIONS_INTEGRATION)) {
      log.info("Non verrà processato il messaggio dalla piattaforma Missioni in quanto "
          + "la sede {} cui appartiene il destinatario {} "
          + "ha l'integrazione con Missioni disabilitata",
          office.get().name, body.person.fullName());
      JsonResponse.ok();
    }    

    // person not present (404)
    if (!missionManager.linkToPerson(body).isPresent()) {
      JsonResponse.notFound();
    }
    
    switch (body.tipoMissione) {
      case "ORDINE":
        if (!missionManager.createMissionFromClient(body, true)) {
          JsonResponse.conflict();
        }
        break;
      case "RIMBORSO":
        if (!missionManager.manageMissionFromClient(body, true)) {
          JsonResponse.notFound();
        }
        break;
      case "ANNULLAMENTO":
        if (!missionManager.deleteMissionFromClient(body, true)) {
          JsonResponse.badRequest();
        }
        break;
      default: 
        break;
    }

    // Success (200)
    JsonResponse.ok();
  }

}
