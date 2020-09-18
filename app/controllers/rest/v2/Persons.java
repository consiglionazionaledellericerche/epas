package controllers.rest.v2;

import cnr.sync.dto.v2.PersonCreateDto;
import cnr.sync.dto.v2.PersonShowDto;
import cnr.sync.dto.v2.PersonUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.OfficeDao;
import dao.PersonDao;
import helpers.JsonResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.OfficeManager;
import manager.PersonManager;
import manager.UserManager;
import models.Office;
import models.Person;
import org.testng.util.Strings;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;
import security.SecurityRules;

@With(Resecure.class)
@Slf4j
public class Persons extends Controller {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static UserManager userManager;
  @Inject
  static PersonManager personManager;
  @Inject
  static OfficeManager officeManager;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  @BasicAuth
  public static void list(Long id, String code, String codeId) {
    val office = getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);
    
    val list = 
        office.persons.stream().map(p -> PersonShowDto.build(p)).collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(list));
  }

  @BasicAuth
  public static void show(Long id, String email, String eppn, Long personPerseoId, String fiscalCode) {

    val person = getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);

    rules.checkIfPermitted(person.office);

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(PersonShowDto.build(person)));
  }

  @BasicAuth
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    Verify.verify(request.method.equalsIgnoreCase("POST"));

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
    };
    
    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato
    //nel DTO
    rules.checkIfPermitted(person.office);
    
    personManager.properPersonCreate(person);
    person.save();

    log.info("Created person {} via REST", person);
    renderJSON(gson.toJson(PersonShowDto.build(person)));
  }

  @BasicAuth
  public static void update(Long id, String email, String eppn, Long personPerseoId, String fiscalCode,
      String body) throws JsonParseException, JsonMappingException, IOException {
    Verify.verify(request.method.equalsIgnoreCase("PUT"));

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
    };
    person.save();

    log.info("Updated person {} via REST", person);
    renderJSON(gson.toJson(PersonShowDto.build(person)));
  }

  @BasicAuth
  public static void delete(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode) {
    Verify.verify(request.method.equalsIgnoreCase("DELETE"));
    val person = getPersonFromRequest(id, null, null, null, null);
    rules.checkIfPermitted(person.office);
    
    if (!person.contracts.isEmpty()) {
      JsonResponse.conflict(
          String.format("Ci sono %d contratti associati a questa persona. "
              + "Cancellare prima i contratti associati.", person.contracts.size()));
    }

    person.delete();
    log.info("Deleted person {} via REST", person);
    JsonResponse.ok();
  }
  
  @Util
  public static Person getPersonFromRequest(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode) {
    if (id == null && email == null && eppn == null && 
        personPerseoId == null && fiscalCode == null) {
      JsonResponse.badRequest();
    }

    Optional<Person> person = 
        personDao.byIdOrEppnOrEmailOrPerseoIdOrFiscalCode(id, eppn, email, personPerseoId, fiscalCode);

    if (!person.isPresent()) {
      log.info("Non trovata la persona in base ai parametri passati: "
          + "email = {}, eppn = {}, personPersoId = {}, fiscalCode = {}", 
          email, eppn, personPerseoId, fiscalCode);
      JsonResponse.notFound("Non è stato possibile individuare la persona in ePAS con "
          + "i parametri passati.");
    }

    return person.get();
  }
  
  @Util
  public static Office getOfficeFromRequest(
      Long id, String code, String codeId) {
    if (id == null && code == null && codeId == null) {
      JsonResponse.badRequest();
    }
    Optional<Office> office = Optional.absent();
    if (id != null) {
      office = Optional.fromNullable(officeDao.getOfficeById(id));
    }
    if (!Strings.isNullOrEmpty(code)) {
      office = officeDao.byCode(code);
    }
    if (!Strings.isNullOrEmpty(codeId)) {
      office = officeDao.byCodeId(codeId);
    }

    if (!office.isPresent()) {
      log.info("Non trovato l'ufficio in base ai parametri passati: "
          + "id = {}, code = {}, codeId = {}", 
          id, code, codeId);
      JsonResponse.notFound("Non è stato possibile individuare l'ufficio in ePAS con "
          + "i parametri passati.");
    }

    return office.get();
  }
}
