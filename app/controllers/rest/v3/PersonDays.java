/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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
package controllers.rest.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v3.PersonDayShowDto;
import cnr.sync.dto.v3.PersonDayShowTerseDto;
import cnr.sync.dto.v3.PersonMonthRecapDto;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.PersonDays.MealTicketDecision;
import controllers.Resecure;
import controllers.rest.v2.Persons;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import helpers.JodaConverters;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.PersonManager;
import models.Office;
import models.Person;
import models.PersonDay;
import models.enumerate.MealTicketBehaviour;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la visualizzazione via REST di dati relativi alla situazione giornaliera.
 */
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
  static GsonBuilder gsonBuilder;
  @Inject
  static PersonManager personManager;
  @Inject
  static ConsistencyManager consistencyManager;
  
  /**
   * Metodo rest che ritorna la situazione della persona (passata per id, email, eppn, 
   * personPerseoId o fiscalCode) in un giorno specifico (date).
   */
  public static void getDaySituation(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode, 
      String number, LocalDate date) {
    log.debug("Chiamata getDaySituation, email = {}, date = {}", email, date);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    if (date == null) {
      JsonResponse.badRequest("Il parametro date è obbligatorio");
    }

    org.joda.time.LocalDate localDate = new org.joda.time.LocalDate(date.getYear(), 
        date.getMonthValue(), date.getDayOfMonth());
    rules.checkIfPermitted(person.getOffice(localDate).get());

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
   *
   * @param sedeId l'identificativo della sede di cui ricercare la situazione delle persone
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void getMonthSituationByOffice(Long id, String code, String codeId,
      String sedeId, Integer year, Integer month) {
    log.debug("getMonthSituationByOffice -> id={}, sedeId={}, year={}, month={}", 
        id, sedeId, year, month);
    if (year == null || month == null) {
      JsonResponse.badRequest("I parametri year e month sono tutti obbligatori");
    }
    val office = 
        Offices.getOfficeFromRequest(id, code, Strings.isNullOrEmpty(codeId) ? sedeId : codeId);
    rules.checkIfPermitted(office);
    
    org.joda.time.LocalDate date = new org.joda.time.LocalDate(year, month, 1);

    List<PersonDay> personDays = 
        personDayDao.getPersonDaysByOfficeInPeriod(
            office, date, date.dayOfMonth().withMaximumValue());

    val personDayMap = 
        personDays.stream().collect(Collectors.groupingBy(PersonDay::getPerson));

    List<PersonMonthRecapDto> monthRecaps = Lists.newArrayList();
      personDayMap.forEach((p, pds) -> {
        monthRecaps.add(PersonMonthRecapDto.builder()
            .person(PersonShowTerseDto.build(p))
            .year(year).month(month)
            .personDays(
                pds.stream().map(pd -> PersonDayShowTerseDto.build(pd))
                  .collect(Collectors.toList()))
            .build());
      });

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(monthRecaps));
  }

  /**
   * Metodo che ritorna la lista delle situazioni giornaliere di tutti 
   * i dipendenti della sede passata come parametro alla data passata come parametro.
   *
   * @param sedeId l'identificativo della sede di cui cercare le persone
   * @param date la data per cui cercare i dati
   */
  public static void getDaySituationByOffice(Long id, String code, String codeId,
      String sedeId, LocalDate date) {
    log.debug("getDaySituationByOffice -> sedeId={}, data={}", sedeId, date);
    if (sedeId == null || date == null) {
      JsonResponse.badRequest("I parametri sedeId e date sono obbligatori.");
    }
    val office = 
        Offices.getOfficeFromRequest(id, code, Strings.isNullOrEmpty(codeId) ? sedeId : codeId);
    Set<Office> offices = Sets.newHashSet(office);
    
    List<Person> personList = personDao
        .list(Optional.<String>absent(), offices, false, 
            JodaConverters.javaToJodaLocalDate(date), JodaConverters.javaToJodaLocalDate(date), 
            true).list();

    List<PersonDayShowDto> personDayDtos = Lists.newArrayList();
    
    for (Person person : personList) {
      PersonDay pd = 
          personDayDao.getPersonDay(person, JodaConverters.javaToJodaLocalDate(date)).orNull();
      if (pd == null) {
        log.info("Non sono presenti informazioni per {} nel giorno {}", person.getFullname(), date);
      } else {
        personDayDtos.add(PersonDayShowDto.build(pd));  
      }
    }

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(personDayDtos));
  }

  /**
   * Metodo rest che ritorna un json contenente la lista dei person day di un dipendente
   * nell'anno/mese passati come parametro.
   */
  public static void getMonthSituationByPerson(Long id, String email, String eppn, 
      Long personPerseoId, String fiscalCode, String number, Integer year, Integer month) {
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    if (year == null || month == null) {
      JsonResponse.badRequest("I parametri year e month sono tutti obbligatori");
    }

    org.joda.time.LocalDate localDate = new org.joda.time.LocalDate(year, month, 1);
    rules.checkIfPermitted(person.getOffice(localDate).get());

    val personDays = personDayDao.getPersonDayInMonth(person, new YearMonth(year, month));
    val monthRecap = 
        PersonMonthRecapDto.builder().year(year).month(month)
        .basedWorkingDays(
            personManager.basedWorkingDays(personDays, person.getContracts(), null))
        .person(PersonShowTerseDto.build(person))
        .personDays(personDays.stream().map(pd -> PersonDayShowTerseDto.build(pd))
            .collect(Collectors.toList()))
        .build();
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(monthRecap));
  }

  /**
   * Metodo rest che ritorna un json contenente la lista dei person day di un dipendente
   * nell'anno/mese passati come parametro e che abbiano almeno una timbratura per
   * lavoro fuori sede o per motivi di servizio con impostato luogo o motivazione.
   */
  public static void offSiteWorkByPersonAndMonth(Long id, String email, String eppn,
      Long personPerseoId, String fiscalCode, String number, Integer year, Integer month) {
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    if (year == null || month == null) {
      JsonResponse.badRequest("I parametri year e month sono tutti obbligatori");
    }

    org.joda.time.LocalDate localDate = new org.joda.time.LocalDate(year, month, 1);
    rules.checkIfPermitted(person.getOffice(localDate).get());

    val gson = gsonBuilder.create();
    val yearMonth = new YearMonth(year, month);
    val personDays = personDayDao.getOffSitePersonDaysByPersonInPeriod(
        person, yearMonth.toLocalDate(1), 
        yearMonth.toLocalDate(1).dayOfMonth().withMaximumValue());

    renderJSON(gson.toJson(personDays.stream().map(
        pd -> PersonDayShowTerseDto.build(pd)).collect(Collectors.toList())));
  }
  
  /**
   * Metodo rest che ritorna un json contenente la lista dei person day di una sede
   * nell'anno/mese passati come parametro e che abbiano almeno una timbratura per
   * lavoro fuori sede o per motivi di servizio con impostato luogo o motivazione.
   */
  public static void offSiteWorkByOfficeAndMonth(Long id, String code, String codeId,
      String sedeId, Integer year, Integer month) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office =
        Offices.getOfficeFromRequest(id, code, Strings.isNullOrEmpty(codeId) ? sedeId : codeId);
    val gson = gsonBuilder.create();
    val yearMonth = new YearMonth(year, month);
    val personDays = personDayDao.getOffSitePersonDaysByOfficeInPeriod(
        office, yearMonth.toLocalDate(1), 
        yearMonth.toLocalDate(1).dayOfMonth().withMaximumValue());

    renderJSON(gson.toJson(personDays.stream().map(
        pd -> PersonDayShowDto.build(pd)).collect(Collectors.toList())));
  }

  /**
   * Metodo rest che ritorna un json contenente la lista dei person day di una sede
   * nell'anno/mese passati come parametro e che abbiano almeno una timbratura per
   * motivi di servizio.
   */
  public static void serviceExitByOfficeAndMonth(Long id, String code, String codeId,
      String sedeId, Integer year, Integer month) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office =
        Offices.getOfficeFromRequest(id, code, Strings.isNullOrEmpty(codeId) ? sedeId : codeId);
    val gson = gsonBuilder.create();
    val yearMonth = new YearMonth(year, month);
    val personDays = personDayDao.getServiceExitPersonDaysByOfficeInPeriod(
        office, yearMonth.toLocalDate(1), 
        yearMonth.toLocalDate(1).dayOfMonth().withMaximumValue());

    renderJSON(gson.toJson(personDays.stream().map(
        pd -> PersonDayShowDto.build(pd)).collect(Collectors.toList())));
  }

  /**
   * Metodo rest che ritorna la situazione della persona (passata per id, email, eppn, 
   * personPerseoId o fiscalCode) in un giorno specifico (date).
   */
  public static void setMealTicketBehavior(
      Long id, String email, String eppn, Long personPerseoId, String fiscalCode, 
      String number, LocalDate date, MealTicketDecision mealTicketDecision, String note) {
    log.debug("Chiamata getDaySituation, email = {}, date = {}", email, date);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    if (date == null || mealTicketDecision == null) {
      JsonResponse.badRequest("Il parametro date e mealTicketDecision sono obbligatori");
    }
    rules.checkIfPermitted(person.getCurrentOffice().get());

    PersonDay pd = 
        personDayDao.getPersonDay(person, JodaConverters.javaToJodaLocalDate(date)).orNull();
    if (pd == null) {
      JsonResponse.notFound(
          String.format("Non sono presenti informazioni per %s nel giorno %s",
              person.getFullname(), date));
    }

    if (mealTicketDecision.equals(MealTicketDecision.COMPUTED)) {
      pd.setTicketForcedByAdmin(false);
    } else {
      pd.setTicketForcedByAdmin(true);
      if (mealTicketDecision.equals(MealTicketDecision.FORCED_FALSE)) {
        pd.setTicketAvailable(MealTicketBehaviour.notAllowMealTicket);
      }
      if (mealTicketDecision.equals(MealTicketDecision.FORCED_TRUE)) {
        pd.setTicketAvailable(MealTicketBehaviour.allowMealTicket);
      }
    }
    pd.setNote(Strings.emptyToNull(note));
    pd.save();
    consistencyManager.updatePersonSituation(pd.getPerson().id, pd.getDate());

    log.info("Impostato comportamento buono pasto {}) per giorno {} di {}", 
        mealTicketDecision, pd.getDate(), person);

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(PersonDayShowTerseDto.build(pd)));
  }

}