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

import cnr.sync.dto.v2.AffiliationCreateDto;
import cnr.sync.dto.v2.AffiliationShowDto;
import cnr.sync.dto.v2.AffiliationUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.AffiliationDao;
import dao.GroupDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.flows.Affiliation;
import org.testng.collections.Lists;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * API Rest per la gestione dei gruppi di persone.
 *
 * @author cristian
 *
 */
@Slf4j
@With(Resecure.class)
public class Affiliations extends Controller {

  @Inject
  static GroupDao groupDao; 
  @Inject
  static AffiliationDao affiliationDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Affiliazioni al gruppo.
   *
   * @param id id del gruppo.
   * @param includeInactive se true include anche quelle non attive in 
   *     questo momento, se false (il default) le include tutte.
   */
  public static void byGroup(Long id, boolean includeInactive) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null) {
      JsonResponse.notFound();
    }
    val group = groupDao.byId(id).orNull();
    if (group == null) {
      JsonResponse.notFound();
    }
    rules.checkIfPermitted(group.getOffice());
    List<AffiliationShowDto> affiliations = Lists.newArrayList();
    if (!includeInactive) {
      affiliations = group.getAffiliations().stream()
          .filter(a -> a.isActive())
          .map(a -> AffiliationShowDto.build(a))
          .collect(Collectors.toList());
    } else {
      affiliations = group.getAffiliations().stream()
          .map(a -> AffiliationShowDto.build(a))
          .collect(Collectors.toList());
    }
    renderJSON(gsonBuilder.create().toJson(affiliations));
  }

  /**
   * Affiliazioni alla persona.
   * La persona è individuate tramite una delle chiavi della persona passate come
   * parametro (uniformemente agli metodi REST sulle persone).
   *
   * @param includeInactive se true include anche quelle non attive in 
   *     questo momento, se false (il default) le include tutte.
   */
  public static void byPerson(Long id, String email, String eppn, Long personPerseoId, 
      String fiscalCode, String number, boolean includeInactive) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);

    rules.checkIfPermitted(person.getCurrentOffice().get());

    List<AffiliationShowDto> affiliations = Lists.newArrayList();
    if (!includeInactive) {
      affiliations = person.getAffiliations().stream()
          .filter(a -> a.isActive())
          .map(a -> AffiliationShowDto.build(a))
          .collect(Collectors.toList());
    } else {
      affiliations = person.getAffiliations().stream()
          .map(a -> AffiliationShowDto.build(a))
          .collect(Collectors.toList());
    }
    renderJSON(gsonBuilder.create().toJson(affiliations));
  }

  /**
   * Restituisce il JSON con l'affiliazione cercata per id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val affiliation = getAffiliationFromRequest(id);
    renderJSON(gsonBuilder.create().toJson(AffiliationShowDto.build(affiliation)));
  }

  /**
   * Crea una affiliazione con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.POST);
    log.debug("Create affiliation -> request.body = {}", body);
    if (body == null) {
      JsonResponse.badRequest();
    }
    val gson = gsonBuilder.create();
    val affiliationDto = gson.fromJson(body, AffiliationCreateDto.class); 
    val validationResult = validation.valid(affiliationDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    val affiliation = AffiliationCreateDto.build(affiliationDto);
    if (!validation.valid(affiliation).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato
    //nel DTO
    rules.checkIfPermitted(affiliation.getGroup().getOffice());
    rules.checkIfPermitted(affiliation.getPerson().getCurrentOffice().get());

    affiliation.save();

    log.info("Created affiliation {} via REST", affiliation);
    renderJSON(gson.toJson(AffiliationShowDto.build(affiliation)));
  }

  /**
   * Aggiorna i dati di una affiliazione individuata per id
   * con i valori passati nel body HTTP come JSON.
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  public static void update(Long id, String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.PUT);
    log.debug("Update affiliation -> request.body = {}", body);
    val affiliation = getAffiliationFromRequest(id);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona
    rules.checkIfPermitted(affiliation.getGroup().getOffice());

    rules.checkIfPermitted(affiliation.getPerson().getCurrentOffice().get());

    val gson = gsonBuilder.create();
    val affiliationDto = gson.fromJson(body, AffiliationUpdateDto.class); 
    val validationResult = validation.valid(affiliationDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    affiliationDto.update(affiliation);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato 
    //nel DTO
    rules.checkIfPermitted(affiliation.getGroup().getOffice());

    rules.checkIfPermitted(affiliation.getPerson().getCurrentOffice().get());

    if (!validation.valid(affiliation).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    affiliation.save();

    log.info("Updated affiliation {} via REST", affiliation);
    renderJSON(gson.toJson(AffiliationShowDto.build(affiliation)));
  }

  /**
   * Effettua la cancellazione di una affiliazione individuata con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    RestUtils.checkMethod(request, HttpMethod.DELETE);
    val affiliation = getAffiliationFromRequest(id);

    affiliation.delete();
    log.info("Deleted affiliation {} via REST", affiliation);
    JsonResponse.ok();
  }
  
  /**
   * Cerca l'affiliazione in funzione del id passato.
   *
   * @return l'l'affiliazione se trovata, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   * 
   */
  @Util
  private static Affiliation getAffiliationFromRequest(Long id) {
    if (id == null) {
      JsonResponse.notFound();
    }

    val affiliation = affiliationDao.byId(id).orElse(null);

    if (affiliation == null) {
      JsonResponse.notFound();
    }    

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //del gruppo a cui appartiene l'affiliazione
    rules.checkIfPermitted(affiliation.getGroup().getOffice());
    return affiliation;
  }
}