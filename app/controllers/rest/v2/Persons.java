package controllers.rest.v2;

import cnr.sync.dto.v2.PersonCreateDto;
import cnr.sync.dto.v2.PersonShowDto;
import cnr.sync.dto.v2.PersonUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Optional;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.rest.v2.RestUtil.HttpMethod;
import dao.PersonDao;
import helpers.JsonResponse;
import java.io.IOException;
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
import security.SecurityRules;

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
  public static void list(Long id, String code, String codeId) {
    RestUtil.checkMethod(request, HttpMethod.GET);
    val office = Offices.getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);
    
    val list = 
        office.persons.stream().map(p -> PersonShowDto.build(p)).collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(list));
  }

  /**
   * Restituisce il JSON con i dati della persona individuata con i parametri
   * passati. 
   */
  public static void show(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode) {
    RestUtil.checkMethod(request, HttpMethod.GET);
    val person = getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);

    rules.checkIfPermitted(person.office);

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
    RestUtil.checkMethod(request, HttpMethod.POST);

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
    rules.checkIfPermitted(person.office);
    
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
      String body) throws JsonParseException, JsonMappingException, IOException {
    RestUtil.checkMethod(request, HttpMethod.PUT);

    log.debug("Update person -> request.body = {}", body);

    val person = getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);
    
    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona
    rules.checkIfPermitted(person.office);
    
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
    rules.checkIfPermitted(person.office);

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
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode) {
    RestUtil.checkMethod(request, HttpMethod.DELETE);
    val person = getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);
    rules.checkIfPermitted(person.office);
    
    if (!person.contracts.isEmpty()) {
      JsonResponse.conflict(
          String.format("Ci sono %d contratti associati a questa persona. "
              + "Cancellare prima i contratti associati.", person.contracts.size()));
    }

    person.delete();
    person.user.delete();
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
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode) {
    if (id == null && email == null && eppn == null 
        && personPerseoId == null && fiscalCode == null) {
      JsonResponse.badRequest();
    }

    Optional<Person> person = 
        personDao.byIdOrEppnOrEmailOrPerseoIdOrFiscalCode(
            id, eppn, email, personPerseoId, fiscalCode);

    if (!person.isPresent()) {
      log.info("Non trovata la persona in base ai parametri passati: "
          + "email = {}, eppn = {}, personPersoId = {}, fiscalCode = {}", 
          email, eppn, personPerseoId, fiscalCode);
      JsonResponse.notFound("Non è stato possibile individuare la persona in ePAS con "
          + "i parametri passati.");
    }

    return person.get();
  }
  

}
