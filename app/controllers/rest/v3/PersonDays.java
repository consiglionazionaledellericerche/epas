package controllers.rest.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v3.PersonDayShowTerseDto;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.rest.v2.Persons;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import helpers.JodaConverters;
import helpers.JsonResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Office;
import models.Person;
import models.PersonDay;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class PersonDays extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Metodo rest che ritorna la situazione della persona (passata per id, email, eppn, 
   * personPerseoId o fiscalCode) in un giorno specifico (date).
   */
  public static void getDaySituation(
      Long id, String email, String eppn, 
      Long personPerseoId, String fiscalCode, LocalDate date) {
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);
    if (date == null) {
      JsonResponse.badRequest("Il parametro date è obbligatorio");
    }
    rules.checkIfPermitted(person.office);

    PersonDay pd = 
        personDayDao.getPersonDay(person, JodaConverters.javaToJodaLocalDate(date)).orNull();
    if (pd == null) {
      JsonResponse.notFound(
          String.format("Non sono presenti informazioni per %s nel giorno %s",
              person.getFullname(), date));
    }

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(PersonDayShowTerseDto.build(pd)));
  }
  
  /**
   * Metodo rest che ritorna un json contenente la lista dei person day di tutti i dipendenti
   * di una certa sede nell'anno/mese passati come parametro.
   * @param sedeId l'identificativo della sede di cui ricercare la situazione delle persone
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  @BasicAuth
  public static void getMonthSituationByOffice(String sedeId, Integer year, Integer month) {
    log.debug("getMonthSituationByOffice -> sedeId={}, year={}, month={}", sedeId, year, month);
    if (year == null || month == null || sedeId == null) {
      JsonResponse.badRequest("I parametri sedeId, year e month sono tutti obbligatori");
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      JsonResponse.notFound("Office non trovato con il sedeId passato per parametro");
    }
    rules.checkIfPermitted(office.get());    
    
    org.joda.time.LocalDate date = new org.joda.time.LocalDate(year, month, 1);

    List<PersonDay> personDays = 
        personDayDao.getPersonDaysByOfficeInPeriod(
            office.get(), date, date.dayOfMonth().withMaximumValue());
    
    Map<PersonShowTerseDto, List<PersonDayShowTerseDto>> map = Maps.newHashMap();
    List<PersonDayShowTerseDto> list = Lists.newArrayList();

    for (PersonDay pd : personDays) {
      PersonDayShowTerseDto pdDto = PersonDayShowTerseDto.build(pd);
      val personDto = PersonShowTerseDto.build(pd.person);
      if (map.containsKey(personDto)) {
        list = map.get(personDto);
      } else {
        list = Lists.newArrayList();
      }
      list.add(pdDto);
      map.put(personDto, list);
      
    }

    log.debug("Terminato invio di informazioni della sede {} per l'anno {} mese {}", 
        office.get().name, year, month);
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(map));
  }
  
  /**
   * Metodo che ritorna la mappa delle situazioni giornaliere di tutti 
   * i dipendenti della sede passata come parametro alla data passata come parametro.
   * @param sedeId l'identificativo della sede di cui cercare le persone
   * @param date la data per cui cercare i dati
   */
  @BasicAuth
  public static void getDaySituationByOffice(String sedeId, LocalDate date) {
    log.debug("getDaySituationByOffice -> sedeId={}, data={}", sedeId, date);
    if (sedeId == null || date == null) {
      JsonResponse.badRequest("I parametri sedeId e date sono obbligatori.");
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      JsonResponse.notFound("Office non trovato con il sedeId passato per parametro");
    }
    rules.checkIfPermitted(office.get());
    Set<Office> offices = Sets.newHashSet(office.get());
    
    List<Person> personList = personDao
        .list(Optional.<String>absent(), offices, false, 
            JodaConverters.javaToJodaLocalDate(date), JodaConverters.javaToJodaLocalDate(date), 
            true).list();
    Map<PersonShowTerseDto, PersonDayShowTerseDto> map = Maps.newHashMap();
    for (Person person : personList) {
      PersonDay pd = 
          personDayDao.getPersonDay(person, JodaConverters.javaToJodaLocalDate(date)).orNull();
      if (pd == null) {
        JsonResponse.notFound("Non sono presenti informazioni per "
                + person.name + " " + person.surname + " nel giorno " + date);
      }
      map.put(PersonShowTerseDto.build(person), PersonDayShowTerseDto.build(pd));
    }
    log.debug("Terminato invio di informazioni della sede {} per il giorno {}", 
        office.get().name, date);
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(map));
  }

}
