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
import dao.PersonDao;
import helpers.JsonResponse;
import java.io.IOException;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.OfficeManager;
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
  static OfficeManager officeManager;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  @BasicAuth
  public static void show(String email, String eppn, Long personPerseoId, String fiscalCode) {

    val person = getPersonFromRequest(email, eppn, personPerseoId, fiscalCode);

    rules.checkIfPermitted(person.office);
    
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(PersonShowDto.build(person)));
  }
  
  @BasicAuth
  public static void update(String email, String eppn, Long personPerseoId, String fiscalCode,
      String body) throws JsonParseException, JsonMappingException, IOException {
    Verify.verify(request.method.equalsIgnoreCase("PUT"));
    
    log.debug("update person -> request.body = {}", body);
    
    val person = getPersonFromRequest(email, eppn, personPerseoId, fiscalCode);

    val gson = gsonBuilder.create();
    val personDto = gson.fromJson(body, PersonUpdateDto.class); 
    val validationResult = validation.valid(personDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    personDto.update(person);
    person.save();
    
    log.info("Aggiornata persona {} via REST", person);
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
    if (!personManager.properPersonCreate(person) ||
        !validation.valid(person).ok) {
      //Restituire un messaggio esaustivo dei problemi 
      //riscontrati
      JsonResponse.badRequest(validation.errorsMap().toString());
    };
    
    person.save();
    
    log.info("Created person {} via REST", person);
    renderJSON(gson.toJson(PersonShowDto.build(person)));
  }
  
  @Util
  public static Person getPersonFromRequest(
      String email, String eppn, Long personPerseoId, String fiscalCode) {
    if (email == null && eppn == null && personPerseoId == null && fiscalCode == null) {
      JsonResponse.notFound();
    }

    Optional<Person> person = 
        personDao.byEppnOrEmailOrPerseoIdOrFiscalCode(eppn, email, personPerseoId, fiscalCode);

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
