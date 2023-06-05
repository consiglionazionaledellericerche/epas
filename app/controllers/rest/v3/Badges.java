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

import cnr.sync.dto.v3.BadgeCreateDto;
import cnr.sync.dto.v3.BadgeShowDto;
import cnr.sync.dto.v3.BadgeShowTerseDto;
import cnr.sync.dto.v3.BadgeUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.rest.v2.Offices;
import controllers.rest.v2.Persons;
import dao.BadgeDao;
import dao.BadgeSystemDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Badge;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * API Rest per la gestione dei badge.
 *
 * @author Cristian Lucchesi
 * @since version 3
 */
@Slf4j
@With(Resecure.class)
public class Badges extends Controller {

  @Inject
  static SecurityRules rules;

  @Inject
  static BadgeDao badgeDao;

  @Inject
  static BadgeSystemDao badgeSystemDao;
  
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Badge dei dipendenti di un ufficio.
   * L'ufficio è individuato tramite una delle chiavi del ufficio passate come
   * parametro (uniformemente ai metodi REST sugli uffici). 
   */
  public void byOffice(Long id, String code, String codeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);

    val office = Offices.getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);

    val badges = badgeDao.byOffice(office);
    renderJSON(gsonBuilder.create().toJson(
        badges.stream().map(BadgeShowTerseDto::build).collect(Collectors.toSet())));
  }

  /**
   * Badge associati ad un gruppo badge (badgeSystem).
   * Il gruppo badge è individuato tramite il suo id.
   */
  public void byBadgeSystem(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);

    notFoundIfNull(id);
    val badgeSystem = badgeSystemDao.byId(id);
    notFoundIfNull(badgeSystem);
    rules.checkIfPermitted(badgeSystem.getOffice());

    val badges = badgeSystemDao.badges(badgeSystem);
    renderJSON(gsonBuilder.create().toJson(
        badges.stream().map(BadgeShowTerseDto::build).collect(Collectors.toSet())));
  }
  
  /**
   * Badge di un dipendente.
   * La persona è individuata tramite una delle chiavi della persona passate come
   * parametro (uniformemente ai metodi REST sulle persone). 
   */
  public static void byPerson(Long id, String email, String eppn, Long personPerseoId, 
      String fiscalCode, String number) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    rules.checkIfPermitted(person.getOffice());
    List<BadgeShowDto> badges = 
        person.getBadges().stream().map(c -> BadgeShowDto.build(c))
        .collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(badges));
  }

  /**
   * Restituisce il JSON con il badge cercato per id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val badge = getBadgeFromRequest(id);
    renderJSON(gsonBuilder.create().toJson(BadgeShowDto.build(badge)));
  }

  /**
   * Crea un badge con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.POST);
    log.debug("Create badge -> request.body = {}", body);
    if (body == null) {
      JsonResponse.badRequest();
    }
    val gson = gsonBuilder.create();
    val badgeDto = gson.fromJson(body, BadgeCreateDto.class); 
    val validationResult = validation.valid(badgeDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    val badge = BadgeCreateDto.build(badgeDto);
    if (!validation.valid(badge).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office associato alla
    //persona indicata nel DTO
    rules.checkIfPermitted(badge.getPerson().getOffice());

    badge.save();

    log.info("Created badge {} via REST", badge);
    renderJSON(gson.toJson(BadgeShowDto.build(badge)));
  }

  /**
   * Aggiorna i dati di badge individuato per id
   * con i valori passati nel body HTTP come JSON.
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  public static void update(Long id, String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.PUT);
    val badge = getBadgeFromRequest(id);
    if (body == null) {
      JsonResponse.badRequest();
    }

    val gson = gsonBuilder.create();
    val badgeDto = gson.fromJson(body, BadgeUpdateDto.class); 
    val validationResult = validation.valid(badgeDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office associato alla
    //persona indicata nel DTO
    rules.checkIfPermitted(badge.getPerson().getOffice());

    if (!validation.valid(badge).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    badgeDto.update(badge);
    badge.save();

    log.info("Updated badge {} via REST", badge);
    renderJSON(gson.toJson(BadgeShowDto.build(badge)));
  }

  /**
   * Effettua la cancellazione di un badge individuato con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    RestUtils.checkMethod(request, HttpMethod.DELETE);
    val badge = getBadgeFromRequest(id);

    badge.delete();
    log.info("Deleted badge {} via REST", badge);
    JsonResponse.ok();
  }

  /**
   * Cerca il badge in funzione del id passato.
   *
   * @return il badge se trovato, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   */
  @Util
  private static Badge getBadgeFromRequest(Long id) {
    if (id == null) {
      JsonResponse.notFound();
    }

    val badge = badgeDao.byId(id);

    if (badge == null) {
      JsonResponse.notFound();
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona
    rules.checkIfPermitted(badge.getPerson().getOffice());
    return badge;
  }
}
