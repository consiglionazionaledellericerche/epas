/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

package controllers.rest;

import javax.inject.Inject;

import org.joda.time.LocalDateTime;

import com.google.common.base.Optional;

import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.OfficeDao;
import helpers.JsonResponse;
import it.cnr.iit.epas.JsonMissionBinder;
import lombok.extern.slf4j.Slf4j;
import manager.MissionManager;
import manager.NotificationManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Office;
import models.exports.MissionFromClient;
import play.cache.Cache;
import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;


/**
 * Controller per la ricezione via REST delle informazioni sulle nuove missioni.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@With(Resecure.class)
public class Missions extends Controller {
  
  @Inject
  private static MissionManager missionManager;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static NotificationManager notificationManager;
  
  private static void logInfo(String description, MissionFromClient body) {
    log.info(MissionManager.LOG_PREFIX + "{}. Messaggio: {}", description, body);
  }
  
  private static void logWarn(String description, MissionFromClient body) {
    log.warn(MissionManager.LOG_PREFIX + "{}. Messaggio: {}", description, body);
  }
  
  /**
   * Metodo che processa il messaggio ricevuto dal kraken-listener, oppure da qualche sistema
   * esterno che invia le missioni via REST.
   *
   * @param body il dto costruito a partire dal binder
   */
  @BasicAuth
  public static void amqpreceiver(@As(binder = JsonMissionBinder.class)MissionFromClient body) {

    logInfo("Ricevuto messaggio", body);
    
    // Malformed Json (400)
    if (body == null || body.dataInizio == null || body.dataFine == null) {
      logWarn("Messaggio, dataInizio o dataFine vuoti, messaggio scartato", body);
      JsonResponse.badRequest();
      return;
    }

    if (body.dataInizio.isAfter(body.dataFine)) {
      logWarn("Data di inizio successiva alla data di fine, messaggio scartato", body);
      JsonResponse.badRequest();
    }

    if (body.dataInizio.isBefore(LocalDateTime.now().minusMonths(6))) {
      logWarn("Data di inizio precedente di oltre sei mesi dalla data attuale, messaggio scartato", 
          body);
      JsonResponse.badRequest();
    }

    if (body.dataFine.isAfter(LocalDateTime.now().plusMonths(6))) {
      logWarn("Data di fine successiva di oltre 6 mesi dalla data attuale, messaggio scartato", 
          body);
      JsonResponse.badRequest();
    }

    // person not present (404)
    if (!missionManager.linkToPerson(body).isPresent()) {
      logWarn("Dipendente riferito nel messaggio non trovato, messaggio scartato", body);
      JsonResponse.notFound();
    }

    //Ufficio prelevato tramite il codice sede passato nel JSON
    Optional<Office> officeByMessage = officeDao.byCodeId(body.codiceSede);
    //Ufficio associato alla persona prelevata tramite la matricola passata nel JSON
    Office office = body.person.getOffice();

    if (!officeByMessage.isPresent()) {
      logWarn(
          String.format("Attenzione il codice sede %s non è presente su ePAS e il dipendente %s "
              + "è associato all'ufficio %s.", 
              body.codiceSede, body.person.getFullname(), office.getName()), 
          body);
    } else if (!body.codiceSede.equals(office.getCodeId())) {
      logWarn(
          String.format("Attenzione il codice sede %s è diverso dal codice sede di %s (%s), "
              + "sede associata a %s.", 
              body.codiceSede, office.getName(), office.getCodeId(), body.person.getFullname()), 
          body);
    }

    // Check if integration ePAS-Missions is enabled
    if (!(Boolean) configurationManager
        .configValue(office, EpasParam.ENABLE_MISSIONS_INTEGRATION)) {
      logInfo(String.format("Non verrà processato il messaggio in quanto la sede %s "
          + "cui appartiene il destinatario %s ha l'integrazione con Missioni disabilitata",
          office.getName(), body.person.fullName()), body);
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
      logWarn("Problemi durante l'inserimento del messaggio", body);
      String problematicMissionCacheKey = 
          String.format("mission.problematic.%s.%s.%s.%s", 
              body.tipoMissione, body.id, body.anno, body.numero);
      if (Cache.get(problematicMissionCacheKey) == null) {
        log.debug("Imposto la cache di missione problematica con valore true per {}", body);
        notificationManager.sendEmailMissionFromClientProblems(body);
        Cache.set(problematicMissionCacheKey, true, "1d");
      } else {
        logInfo("Missione problematica già segnalata all'utente. Email non inviata.", body);
      }
      JsonResponse.conflict();
    }

    // Success (200)
    JsonResponse.ok();
  }

}