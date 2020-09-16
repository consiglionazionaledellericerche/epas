package controllers.rest.v2;

import cnr.sync.dto.v2.PersonShowDto;
import com.google.common.base.Optional;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.PersonDao;
import helpers.JsonResponse;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@With(Resecure.class)
@Slf4j
public class Persons extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  @BasicAuth
  public static void show(String email, String eppn, Long personPerseoId, String fiscalCode) {

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

    //rules.checkIfPermitted(person.get().office);
    
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(PersonShowDto.build(person.get())));

  }
  
  @BasicAuth
  public static void save(String email, String eppn, Long personPerseoId, String fiscalCode) {

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
  }
}
