/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package controllers.rest.v3;

import cnr.sync.dto.v3.StampingCreateDto;
import cnr.sync.dto.v3.StampingShowDto;
import cnr.sync.dto.v3.StampingUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.GeneralSettingDao;
import dao.StampingDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.io.IOException;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.ConsistencyManager;
import manager.StampingManager;
import models.exports.StampingFromClient;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API Rest per la gestione delle timbrature.
 *
 * @author Cristian Lucchesi
 * @since version 3
 */
@With(Resecure.class)
@Slf4j
public class Stampings extends Controller {

  @Inject
  static StampingManager stampingManager;
  @Inject
  static StampingDao stampingDao;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GeneralSettingDao generalSettingDao;

  /**
   * Restituisce il JSON con la timbratura cercata per id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null) {
      JsonResponse.notFound();
    }
    val stamping = stampingDao.getStampingById(id);
    if (stamping == null) {
      JsonResponse.notFound();
    }

    rules.checkIfPermitted(stamping.getPersonDay().getPerson().getCurrentOffice().get());

    renderJSON(gsonBuilder.create().toJson(StampingShowDto.build(stamping)));
  }


  /**
   * Inserimento di una nuova timbratura con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  public static void create(String body)
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.POST);
    log.debug("Create stamping -> request.body = {}", body);
    
    // Malformed Json (400)
    if (body == null) {
      JsonResponse.badRequest();
    }

    val gson = gsonBuilder.create();
    val stampingCreateDto = gson.fromJson(body, StampingCreateDto.class); 
    val validationResult = validation.valid(stampingCreateDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    // Controlla che la timbratura da inserire non sia troppo nel passato.
    val maxDaysInPastForRestStampings = generalSettingDao.generalSetting()
        .getMaxDaysInPastForRestStampings();
    if (stampingCreateDto.getDateTime()
        .compareTo(LocalDateTime.now().minusDays(maxDaysInPastForRestStampings)) < 0) {
      JsonResponse.badRequest("Timbratura con data troppo nel passato");
    }

    // Badge number not present or invalid (404)
    val person = 
        stampingManager.getPersonFromBadgeAndCurrentBadgeReader(stampingCreateDto.getBadgeNumber());
    if (!person.isPresent()) {
      JsonResponse.notFound();
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti corretti sull'office associato alla persona
    //a cui si riferisce la timbratura

    rules.checkIfPermitted(person.get().getCurrentOffice().get());

    val stampingFromClient = StampingFromClient.build(stampingCreateDto);
    stampingFromClient.person = person.get();    
    log.trace("stampingFromClient = {}", stampingFromClient);

    val stamping = 
        stampingManager.createStampingFromClient(stampingFromClient, true);

    // Stamping already present (409)
    if (!stamping.isPresent()) {
      JsonResponse.conflict();
    }

    // Success (200)
    log.info("Created stamping {} via REST. Person {}", stamping.get(), person.get());
    renderJSON(gson.toJson(StampingShowDto.build(stamping.get())));
  }


  /**
   * Aggiorna i dati di una timbratura individuata con i parametri HTTP
   * passati ed i valori passati nel body HTTP come JSON.
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  public static void update(Long id, String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.PUT);
    log.debug("Update stamping -> id = {}, request.body = {}", id, body);
    if (id == null) {
      JsonResponse.notFound();
    }    
    val stamping = stampingDao.getStampingById(id);
    if (stamping == null) {
      JsonResponse.notFound();
    }
    
    //Controlla anche che l'utente corrente abbia
    //i diritti corretti sull'office attuale 
    //della persona

    rules.checkIfPermitted(stamping.getOwner().getCurrentOffice().get());

    val gson = gsonBuilder.create();
    val stampingDto = gson.fromJson(body, StampingUpdateDto.class); 
    val validationResult = validation.valid(stampingDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    stampingDto.update(stamping);

    val person = stamping.getPersonDay().getPerson();
    
    //Controlla anche che l'utente corrente abbia
    //i diritti corretti sull'office della persona associata 
    //alla timbratura

    rules.checkIfPermitted(person.getCurrentOffice().get());

    if (!validation.valid(stamping).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    stamping.save();
    
    consistencyManager
      .updatePersonSituation(person.id, stamping.getPersonDay().getDate());

    log.info("Updated stamping {} via REST. Person {}", stamping, person);
    renderJSON(gson.toJson(StampingShowDto.build(stamping)));
  }


  /**
   * Effettua la cancellazione di una timbratura individuata con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    RestUtils.checkMethod(request, HttpMethod.DELETE);
    log.debug("Delete stamping -> id = {}", id);

    val stamping = stampingDao.getStampingById(id);
    if (stamping == null) {
      JsonResponse.notFound();
    }

    val person = stamping.getPersonDay().getPerson();
    rules.checkIfPermitted(person.getCurrentOffice().get());

    stamping.delete();
    consistencyManager.updatePersonSituation(
        person.id, stamping.getPersonDay().getDate());

    log.info("Deleted stamping {} via REST. Person = {}", stamping, person);
    JsonResponse.ok();
  }
  
}
