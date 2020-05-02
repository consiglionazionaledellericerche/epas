package controllers.rest.v2;

import com.google.common.base.Optional;
import java.util.Map;
import javax.inject.Inject;
import controllers.Resecure;
import dao.PersonDao;
import helpers.JsonResponse;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.service.ICertificationService;
import manager.attestati.service.PersonCertData;
import manager.attestati.service.PersonMonthlySituationData;
import models.Certification;
import models.Person;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class Certifications extends Controller{

  @Inject
  static PersonMonthlySituationData monthData;
  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;

  public static void getMonthSituation(String email, String eppn, 
      Long personPersoId, int year, int month) {

    Optional<Person> person = personDao.byEppnOrEmailOrPerseoId(eppn, email, personPersoId);

    if (!person.isPresent()) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente in ePAS la "
          + "mail che serve per la ricerca.");
    }

    rules.checkIfPermitted(person);

    Map<String, Certification> map = monthData.getCertification(person.get(), year, month);
    
    renderJSON(map);
  }
}

