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

import cnr.sync.dto.v2.GroupCreateDto;
import cnr.sync.dto.v2.GroupShowDto;
import cnr.sync.dto.v2.GroupShowTerseDto;
import cnr.sync.dto.v2.GroupUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.GroupDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API Rest per la gestione dei gruppi di persone.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
@With(Resecure.class)
public class Groups extends Controller {

  @Inject
  static GroupDao groupDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Elenco dei gruppi di una sede in formato JSON. 
   *
   * @param id id in ePAS dell'Ufficio.
   * @param code codice cds dell'ufficio
   * @param codeId sedeId di attestati
   */
  public static void list(Long id, String code, String codeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office = Offices.getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);

    val list = 
        office.getGroups().stream().map(group -> GroupShowTerseDto.build(group))
        .collect(Collectors.toSet());
    renderJSON(gsonBuilder.create().toJson(list));
  }

  /**
   * Restituisce il JSON con il gruppo cercato per id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null) {
      JsonResponse.notFound();
    }
    val group = groupDao.byId(id).orNull();
    if (group == null) {
      JsonResponse.notFound();
    }
    notFoundIfNull(id);
    rules.checkIfPermitted(group.getOffice());
    renderJSON(gsonBuilder.create().toJson(GroupShowDto.build(group)));
  }

  /**
   * Crea un gruppo con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.POST);

    log.debug("Create affiliation -> request.body = {}", body);

    val gson = gsonBuilder.create();
    val groupDto = gson.fromJson(body, GroupCreateDto.class); 
    val validationResult = validation.valid(groupDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    val group = GroupCreateDto.build(groupDto);
    if (!validation.valid(group).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato
    //nel DTO
    rules.checkIfPermitted(group.getOffice());

    group.save();

    log.info("Created group {} via REST", group);
    renderJSON(gson.toJson(GroupShowDto.build(group)));
  }

  /**
   * Aggiorna i dati di un gruppo individuato con i parametri HTTP
   * passati ed i valori passati nel body HTTP come JSON.
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  public static void update(Long id, String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.PUT);
    
    notFoundIfNull(id);
    log.debug("Update group -> request.body = {}", body);
    val group = groupDao.byId(id).orNull();
    notFoundIfNull(group);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona
    rules.checkIfPermitted(group.getOffice());

    val gson = gsonBuilder.create();
    val groupDto = gson.fromJson(body, GroupUpdateDto.class); 
    val validationResult = validation.valid(groupDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    groupDto.update(group);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato 
    //nel DTO
    rules.checkIfPermitted(group.getOffice());

    if (!validation.valid(group).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    group.save();

    log.info("Updated group {} via REST", group);
    renderJSON(gson.toJson(GroupShowDto.build(group)));
  }

  /**
   * Effettua la cancellazione di un gruppo individuato con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    RestUtils.checkMethod(request, HttpMethod.DELETE);
    val group = groupDao.byId(id).orNull();
    notFoundIfNull(group);
    rules.checkIfPermitted(group.getOffice());
    
    if (!group.getAffiliations().isEmpty()) {
      JsonResponse.conflict(
          String.format("Ci sono %d affiliazioni di persone associate a questo gruppo. "
              + "Cancellare prima le affiliazioni delle persone.", group.getAffiliations().size()));
    }

    group.delete();
    log.info("Deleted group {} via REST", group);
    JsonResponse.ok();
  }
}