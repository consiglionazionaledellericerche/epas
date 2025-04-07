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

package controllers.rest.v2;

import cnr.sync.dto.v2.ChildrenCreateDto;
import cnr.sync.dto.v2.ChildrenShowDto;
import cnr.sync.dto.v2.ChildrenUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.PersonChildrenDao;
import dao.PersonDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.PersonChildren;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * Controller per la gestione dei figli/figlie dei dipendenti.
 */
@Slf4j
@With(Resecure.class)
public class Child extends Controller {

  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static PersonChildrenDao childrenDao;
  @Inject
  static PersonDao personDao;

  /**
   * Figli/figlie di un dipendente.
   * La persona è individuata tramite una delle chiavi della persona passate come
   * parametro (uniformemente ai metodi REST sulle persone). 
   */
  public static void byPerson(Long id, String email, String eppn, Long personPerseoId, 
      String fiscalCode, String number) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);

    rules.checkIfPermitted(person.getCurrentOffice().get());

    List<ChildrenShowDto> childs = 
        person.getPersonChildren().stream().map(c -> ChildrenShowDto.build(c))
        .collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(childs));
  }

  /**
   * Restituisce il JSON con il figlio/figlia cercato per id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val children = getChildrenFromRequest(id);
    renderJSON(gsonBuilder.create().toJson(ChildrenShowDto.build(children)));
  }

  /**
   * Crea un figlio/figlia con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.POST);
    log.debug("Create children -> request.body = {}", body);
    if (body == null) {
      JsonResponse.badRequest();
    }
    val gson = gsonBuilder.create();
    val childrenDto = gson.fromJson(body, ChildrenCreateDto.class); 
    val validationResult = validation.valid(childrenDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    val children = ChildrenCreateDto.build(childrenDto);
    if (!validation.valid(children).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office associato alla
    //persona indicata nel DTO

    rules.checkIfPermitted(children.getPerson().getCurrentOffice().get());

    children.setPerson(personDao.getPersonById(childrenDto.getPersonId()));
    children.save();

    log.info("Created children {} via REST", children);
    renderJSON(gson.toJson(ChildrenShowDto.build(children)));
  }

  /**
   * Aggiorna i dati di un figlio/figlia individuato per id
   * con i valori passati nel body HTTP come JSON.
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  public static void update(Long id, String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.PUT);
    val children = getChildrenFromRequest(id);
    if (body == null) {
      JsonResponse.badRequest();
    }

    val gson = gsonBuilder.create();
    val childrenDto = gson.fromJson(body, ChildrenUpdateDto.class); 
    val validationResult = validation.valid(childrenDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office associato alla
    //persona indicata nel DTO

    rules.checkIfPermitted(children.getPerson().getCurrentOffice().get());

    if (!validation.valid(children).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    childrenDto.update(children);
    children.save();

    log.info("Updated children {} via REST", children);
    renderJSON(gson.toJson(ChildrenShowDto.build(children)));
  }

  /**
   * Effettua la cancellazione di un figlio/figlia individuata con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    RestUtils.checkMethod(request, HttpMethod.DELETE);
    val children = getChildrenFromRequest(id);

    children.delete();
    log.info("Deleted children {} via REST", children);
    JsonResponse.ok();
  }

  /**
   * Cerca il/la figlio/a in funzione del id passato.
   *
   * @return il/la figlio/a se trovato, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   */
  @Util
  private static PersonChildren getChildrenFromRequest(Long id) {
    if (id == null) {
      JsonResponse.notFound();
    }

    val children = childrenDao.getById(id);

    if (children == null) {
      JsonResponse.notFound();
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona

    rules.checkIfPermitted(children.getPerson().getCurrentOffice().get());

    return children;
  }
}
