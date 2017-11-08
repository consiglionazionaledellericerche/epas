package controllers.rest;

import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.Resecure.NoCheck;

import helpers.JsonResponse;

import it.cnr.iit.epas.JsonMissionBinder;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.MissionManager;
import manager.StampingManager;

import models.exports.MissionFromClient;

import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;


@Slf4j
@With(Resecure.class)
public class Missions extends Controller {
  
  @Inject
  private static SecurityRules rules;
  @Inject
  private static MissionManager missionManager;
  
  
  @BasicAuth
  public static void amqpreceiver(@As(binder = JsonMissionBinder.class) MissionFromClient body) {
    log.info("Arrivato messaggio da {} ", body);
    // Malformed Json (400)
    if (body == null) {
      JsonResponse.badRequest();
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
      default: 
        break;
    }
    // Mission already present (409)
    

    // Success (200)
    JsonResponse.ok();
  }

}
