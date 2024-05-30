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

import cnr.sync.dto.v2.PersonCreateDto;
import cnr.sync.dto.v2.PersonShowDto;
import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v2.PersonUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.rest.v3.Offices;
import dao.PersonDao;
import helpers.JodaConverters;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.io.IOException;
import java.time.LocalDate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.PersonManager;
import manager.UserManager;
import models.Person;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * API Rest per la gestione delle persone.
 *
 * @author Cristian Lucchesi
 *
 */
@With(Resecure.class)
@Slf4j
public class Persons extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static UserManager userManager;
  @Inject
  static PersonManager personManager;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Lista JSON delle persone che appartengono alla sede
   * individuata con i parametri passati. 
   */
  @BasicAuth
  public static void list(Long id, String code, String codeId, LocalDate atDate, Boolean terse) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (atDate == null) {
      atDate = LocalDate.now();
    }

    val office = Offices.getOfficeFromRequest(id, code, codeId);
    val persons = personDao.list(Optional.<String>absent(), Sets.newHashSet(office), false, 
        JodaConverters.javaToJodaLocalDate(atDate), 
        JodaConverters.javaToJodaLocalDate(atDate), false).list();
    if (terse != null && terse) {
      val list = 
          persons.stream().map(p -> PersonShowTerseDto.build(p)).collect(Collectors.toList());
      renderJSON(gsonBuilder.create().toJson(list));
    } else {
      val list = 
          persons.stream().map(p -> PersonShowDto.build(p)).collect(Collectors.toList());
      renderJSON(gsonBuilder.create().toJson(list));  
    }
  }

  /**
   * Restituisce il JSON con i dati della persona individuata con i parametri
   * passati. 
   */
  public static void show(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode, String number) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);

    rules.checkIfPermitted(person.getOffice());

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(PersonShowDto.build(person)));
  }

  /**
   * Crea una persona con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  @BasicAuth
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.POST);

    log.debug("Create person -> request.body = {}", body);

    val gson = gsonBuilder.create();
    val personDto = gson.fromJson(body, PersonCreateDto.class); 
    val validationResult = validation.valid(personDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    val person = PersonCreateDto.build(personDto);
    if (!validation.valid(person).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    
    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato
    //nel DTO
    rules.checkIfPermitted(person.getOffice());
    
    personManager.properPersonCreate(person);
    person.save();

    log.info("Created person {} via REST", person);
    renderJSON(gson.toJson(PersonShowDto.build(person)));
  }

  /**
   * Aggiorna i dati di una persona individuata con i parametri HTTP
   * passati ed i valori passati nel body HTTP come JSON.
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  @BasicAuth
  public static void update(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode,
      String number, String body) throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.PUT);

    log.debug("Update person -> request.body = {}", body);

    val person = getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    
    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona
    rules.checkIfPermitted(person.getOffice());
    
    val gson = gsonBuilder.create();
    val personDto = gson.fromJson(body, PersonUpdateDto.class); 
    val validationResult = validation.valid(personDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    personDto.update(person);
    
    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato 
    //nel DTO
    rules.checkIfPermitted(person.getOffice());

    if (!validation.valid(person).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    person.save();

    log.info("Updated person {} via REST", person);
    renderJSON(gson.toJson(PersonShowDto.build(person)));
  }

  /**
   * Effettua la cancellazione di una persona individuata con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  @BasicAuth
  public static void delete(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode, String number) {
    RestUtils.checkMethod(request, HttpMethod.DELETE);
    val person = getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    rules.checkIfPermitted(person.getOffice());
    
    if (!person.getContracts().isEmpty()) {
      JsonResponse.conflict(
          String.format("Ci sono %d contratti associati a questa persona. "
              + "Cancellare prima i contratti associati.", person.getContracts().size()));
    }

    person.delete();
    person.getUser().delete();
    log.info("Deleted person {} via REST", person);
    JsonResponse.ok();
  }
  
  /**
   * Cerca la persona in funzione dei parametri passati.
   * La ricerca viene fatta in funzione dei parametri passati
   * che possono essere null, nell'ordine id, email, eppn,
   * perseoPersonId, fiscalCode.
   *
   * @return la persona se trovata, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   * 
   */
  @Util
  public static Person getPersonFromRequest(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode,
      String number) {
    if (id == null && email == null && eppn == null 
        && personPerseoId == null && fiscalCode == null && number == null) {
      JsonResponse.badRequest("I parametri per individuare la persona non sono presenti");
    }

    Optional<Person> person = 
        personDao.byIdOrEppnOrEmailOrPerseoIdOrFiscalCodeOrNumber(
            id, eppn, email, personPerseoId, fiscalCode, number);

    if (!person.isPresent()) {
      log.info("Non trovata la persona in base ai parametri passati: "
          + "email = {}, eppn = {}, personPersoId = {}, fiscalCode = {}, number = {}", 
          email, eppn, personPerseoId, fiscalCode, number);
      JsonResponse.notFound("Non è stato possibile individuare la persona in ePAS con "
          + "i parametri passati.");
    }

    return person.get();
  }

}