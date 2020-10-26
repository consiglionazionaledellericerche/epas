package controllers.rest.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v3.OfficeMonthValidationStatusDto;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.rest.v2.Persons;
import dao.OfficeDao;
import dao.PersonDao;
import helpers.JsonResponse;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.CertificationManager;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class Certifications extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static CertificationManager certificationManager;
  
  /**
   * Metodo rest che ritorna la lista dello stato di invio al sistema
   * di gestione degli attestati mensili (attestati per il CNR).
   */
  public static void getMonthValidationStatusByOffice(String sedeId, Integer year, Integer month) {
    log.debug("getMonthValidationStatus -> sedeId={}, year={}, month={}", sedeId, year, month);
    if (year == null || month == null || sedeId == null) {
      JsonResponse.badRequest("I parametri sedeId, year e month sono tutti obbligatori");
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      JsonResponse.notFound("Office non trovato con il sedeId passato per parametro");
    }
    rules.checkIfPermitted(office.get());
    
    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    final List<Person> people = personDao.list(Optional.absent(),
        Sets.newHashSet(office.get()), false, monthBegin, monthEnd, true).list();
    val validationStatus = new OfficeMonthValidationStatusDto();
    people.stream().forEach(person -> {
      val certData = certificationManager.getPersonCertData(person, year, month);
      if (certData.validate) {
        validationStatus.getValidatedPersons().add(PersonShowTerseDto.build(person));
      } else {
        validationStatus.getNotValidatedPersons().add(PersonShowTerseDto.build(person));
      }
    });
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(validationStatus));  
  }

  /**
   * Metodo rest che ritorna le informazioni di validazione degli attestati mensili 
   * di una dipendente nell'anno/mese passati come parametro.
   */
  public static void getMonthValidationStatusByPerson(Long id, String email, String eppn, 
      Long personPerseoId, String fiscalCode, Integer year, Integer month) {
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);
    if (year == null || month == null) {
      JsonResponse.badRequest("I parametri year e month sono entrambi obbligatori");
    }
    rules.checkIfPermitted(person.office);
    val certData = certificationManager.getPersonCertData(person, year, month);
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(certData.validate));
  }

}
