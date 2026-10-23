/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package controllers.rest.v4;

import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v3.MealTicketResidualDto;
import cnr.sync.dto.v3.OfficeShowTerseDto;
import cnr.sync.dto.v3.PersonDayShowTerseDto;
import cnr.sync.dto.v3.VacationSituationDto;
import cnr.sync.dto.v4.AbsenceRequestCreateDto;
import cnr.sync.dto.v4.AbsenceRequestShowDto;
import cnr.sync.dto.v4.AbsenceRequestTypeDto;
import cnr.sync.dto.v4.MeMonthRecapDto;
import cnr.sync.dto.v4.MeShowDto;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.Security;
import dao.AbsenceRequestDao;
import dao.PersonDayDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import helpers.JodaConverters;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.NotificationManager;
import manager.NotificationManager.Crud;
import manager.PersonManager;
import manager.flows.AbsenceRequestManager;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.absences.AbsenceService;
import models.Person;
import models.User;
import models.absences.definitions.DefaultGroup;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API REST self-service del dipendente autenticato (ad esempio per l'app mobile).
 *
 * <p>La persona è sempre quella associata all'utente autenticato e non viene mai letta dai
 * parametri della richiesta: un dipendente può vedere e modificare solo i propri dati.</p>
 *
 * <p>Le azioni sono annotate con BasicAuth: senza autenticazione rispondono 401 invece di
 * reindirizzare alla pagina di login.</p>
 */
@Slf4j
@With(Resecure.class)
public class Me extends Controller {

  /**
   * I tipi di richiesta di assenza gestiti dalle API REST.
   */
  static final List<AbsenceRequestType> SUPPORTED_REQUEST_TYPES = ImmutableList.of(
      AbsenceRequestType.VACATION_REQUEST, AbsenceRequestType.COMPENSATORY_REST,
      AbsenceRequestType.PERSONAL_PERMISSION);

  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static SecurityRules rules;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static PersonManager personManager;
  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  static AbsenceService absenceService;
  @Inject
  static AbsenceComponentDao absenceComponentDao;
  @Inject
  static AbsenceRequestDao absenceRequestDao;
  @Inject
  static AbsenceRequestManager absenceRequestManager;
  @Inject
  static NotificationManager notificationManager;

  /**
   * Il profilo del dipendente: persona, sede, contratto attuale e tipi di richiesta di
   * assenza disponibili.
   */
  @BasicAuth
  public static void show() {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = currentPerson();
    val currentContract = wrapperFactory.create(person).getCurrentContract();
    val requestTypes = SUPPORTED_REQUEST_TYPES.stream().map(type -> {
      val problems = absenceRequestManager.checkconfiguration(type, person);
      return AbsenceRequestTypeDto.builder().type(type).allDay(type.allDay)
          .available(problems.isEmpty()).problems(problems).build();
    }).collect(Collectors.toList());

    val dto = MeShowDto.builder()
        .person(PersonShowTerseDto.build(person))
        .office(OfficeShowTerseDto.build(person.getOffice()))
        .qualification(person.getQualification() != null
            ? person.getQualification().getQualification() : null)
        .topQualification(person.isTopQualification())
        .currentContractBeginDate(currentContract.isPresent()
            ? JodaConverters.jodaToJavaLocalDate(currentContract.get().getBeginDate()) : null)
        .currentContractEndDate(currentContract.isPresent()
            ? JodaConverters.jodaToJavaLocalDate(currentContract.get().calculatedEnd()) : null)
        .absenceRequestTypes(requestTypes)
        .build();
    renderJSON(gsonBuilder.create().toJson(dto));
  }

  /**
   * La situazione mensile: giorni con timbrature e assenze, residuo orario e buoni pasto.
   *
   * @param year l'anno, se assente quello corrente
   * @param month il mese, se assente quello corrente
   */
  @BasicAuth
  public static void month(Integer year, Integer month) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = currentPerson();
    val today = LocalDate.now();
    int y = year != null ? year : today.getYear();
    int m = month != null ? month : today.getMonthValue();
    if (m < 1 || m > 12) {
      JsonResponse.badRequest("Il parametro month deve essere compreso tra 1 e 12");
    }

    val personDays = personDayDao.getPersonDayInMonth(person, new YearMonth(y, m));
    val recap = stampingsRecapFactory.create(person, y, m, true);
    val dto = MeMonthRecapDto.builder()
        .year(y)
        .month(m)
        .basedWorkingDays(
            personManager.basedWorkingDays(personDays, person.getContracts(), null))
        .residualMinutes(recap.contractMonths.stream()
            .mapToInt(cm -> cm.getValue().getRemainingMinutesLastYear()
                + cm.getValue().getRemainingMinutesCurrentYear()).sum())
        .mealTicketResidual(recap.contractMonths.stream()
            .mapToInt(cm -> cm.getValue().getRemainingMealTickets()).sum())
        .personDays(personDays.stream().map(PersonDayShowTerseDto::build)
            .collect(Collectors.toList()))
        .build();
    renderJSON(gsonBuilder.create().toJson(dto));
  }

  /**
   * Il dettaglio di un giorno, con timbrature e assenze.
   *
   * @param date il giorno, se assente oggi
   */
  @BasicAuth
  public static void day(LocalDate date) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = currentPerson();
    val day = date != null ? date : LocalDate.now();
    val personDay =
        personDayDao.getPersonDay(person, JodaConverters.javaToJodaLocalDate(day)).orNull();
    if (personDay == null) {
      JsonResponse.notFound(String.format("Non sono presenti informazioni nel giorno %s", day));
    }
    renderJSON(gsonBuilder.create().toJson(PersonDayShowTerseDto.build(personDay)));
  }

  /**
   * La situazione delle ferie e dei permessi per ciascun contratto attivo nell'anno.
   *
   * @param year l'anno, se assente quello corrente
   */
  @BasicAuth
  public static void vacations(Integer year) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = currentPerson();
    int y = year != null ? year : LocalDate.now().getYear();
    val vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    val situations = wrapperFactory.create(person).orderedYearContracts(y).stream()
        .map(contract -> VacationSituationDto.build(absenceService.buildVacationSituation(
            contract, y, vacationGroup, Optional.absent(), false)))
        .collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(situations));
  }

  /**
   * I buoni pasto residui ad oggi.
   */
  @BasicAuth
  public static void mealTickets() {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = currentPerson();
    val today = LocalDate.now();
    val recap = stampingsRecapFactory.create(
        person, today.getYear(), today.getMonthValue(), true);
    renderJSON(gsonBuilder.create().toJson(MealTicketResidualDto.build(recap)));
  }

  /**
   * Le richieste di assenza del dipendente con inizio successivo alla data indicata.
   *
   * @param type il tipo di richiesta, se assente tutti i tipi gestiti
   * @param active true per le sole richieste con flusso in corso, false per le sole
   *     terminate, se assente tutte
   * @param from la data da cui cercare, se assente un mese prima dell'inizio dell'anno
   */
  @BasicAuth
  public static void absenceRequests(AbsenceRequestType type, Boolean active, LocalDate from) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = currentPerson();
    val fromDate = from != null
        ? startOfDay(from) : LocalDateTime.now().dayOfYear().withMinimumValue().minusMonths(1);
    val types = type != null ? ImmutableList.of(type) : SUPPORTED_REQUEST_TYPES;
    val states = active != null ? ImmutableList.of(active) : ImmutableList.of(true, false);

    val requests = types.stream()
        .flatMap(t -> states.stream().flatMap(state -> absenceRequestDao
            .findByPersonAndDate(person, fromDate, Optional.absent(), t, state).stream()))
        .sorted(Comparator.comparing(AbsenceRequest::getStartAt).reversed())
        .map(AbsenceRequestShowDto::build)
        .collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(requests));
  }

  /**
   * Il dettaglio di una richiesta di assenza del dipendente.
   *
   * @param id l'id della richiesta
   */
  @BasicAuth
  public static void absenceRequest(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val absenceRequest = ownAbsenceRequest(id);
    rules.checkIfPermitted(absenceRequest);
    renderJSON(gsonBuilder.create().toJson(AbsenceRequestShowDto.build(absenceRequest)));
  }

  /**
   * Inserisce una nuova richiesta di assenza ed avvia il flusso di approvazione.
   *
   * <p>Il body è un AbsenceRequestCreateDto in JSON. Se la richiesta non è inseribile
   * risponde 400 con l'elenco dei problemi riscontrati nel campo errors.</p>
   *
   * @param body il JSON della richiesta
   */
  @BasicAuth
  public static void createAbsenceRequest(String body) {
    RestUtils.checkMethod(request, HttpMethod.POST);
    val person = currentPerson();
    if (body == null) {
      JsonResponse.badRequest("Il body della richiesta è obbligatorio");
    }
    val gson = gsonBuilder.create();
    AbsenceRequestCreateDto dto = null;
    try {
      dto = gson.fromJson(body, AbsenceRequestCreateDto.class);
    } catch (JsonParseException ex) {
      JsonResponse.badRequest("JSON non valido: " + ex.getMessage());
    }
    if (dto == null || !validation.valid(dto).ok) {
      JsonResponse.badRequest("I campi type e startAt sono obbligatori");
    }
    if (!SUPPORTED_REQUEST_TYPES.contains(dto.getType())) {
      JsonResponse.badRequest(
          String.format("Il tipo di richiesta %s non è gestito", dto.getType()));
    }

    val absenceRequest = new AbsenceRequest();
    absenceRequest.setType(dto.getType());
    absenceRequest.setPerson(person);
    absenceRequest.setStartAt(startOfDay(dto.getStartAt()));
    if (dto.getEndTo() != null) {
      absenceRequest.setEndTo(startOfDay(dto.getEndTo()));
    }
    absenceRequest.setNote(dto.getNote());

    val problems = absenceRequestManager.checkNewRequest(
        absenceRequest, dto.getHours(), dto.getMinutes());
    if (!problems.isEmpty()) {
      response.status = 400;
      renderJSON(gson.toJson(ImmutableMap.of("errors", problems)));
    }

    // come nella form web: permesso giornaliero 661G, orario 661M
    val absenceType = dto.getType().equals(AbsenceRequestType.PERSONAL_PERMISSION)
        ? absenceComponentDao.absenceTypeByCode(
            dto.getHours() == null && dto.getMinutes() == null ? "661G" : "661M").orNull()
        : null;
    val outcome = absenceRequestManager.saveNewRequest(absenceRequest, dto.getHours(),
        dto.getMinutes(), absenceType, null, currentUser());
    log.info("Inserita via REST {} con esito {}", absenceRequest, outcome);

    response.status = 201;
    renderJSON(gson.toJson(AbsenceRequestShowDto.build(absenceRequest)));
  }

  /**
   * Ritira una richiesta di assenza non ancora approvata.
   *
   * @param id l'id della richiesta
   */
  @BasicAuth
  public static void deleteAbsenceRequest(Long id) {
    RestUtils.checkMethod(request, HttpMethod.DELETE);
    val absenceRequest = ownAbsenceRequest(id);
    if (!AbsenceRequestShowDto.isDeletable(absenceRequest)) {
      JsonResponse.conflict("La richiesta è già stata approvata o il flusso è terminato");
    }
    rules.checkIfPermitted(absenceRequest);
    val problem = absenceRequestManager.executeEvent(absenceRequest, currentPerson(),
        AbsenceRequestEventType.DELETE, Optional.absent());
    if (problem.isPresent()) {
      JsonResponse.conflict(problem.get());
    }
    // invio la notifica a chi doveva validare la richiesta
    notificationManager.notificationAbsenceRequestPolicy(absenceRequest.getPerson().getUser(),
        absenceRequest, Crud.DELETE);
    log.info("Ritirata via REST {}", absenceRequest);
    JsonResponse.ok();
  }

  private static LocalDateTime startOfDay(LocalDate date) {
    return JodaConverters.javaToJodaLocalDate(date).toLocalDateTime(LocalTime.MIDNIGHT);
  }

  private static User currentUser() {
    return Security.getUser().get();
  }

  /**
   * La persona associata all'utente autenticato, 403 se non presente.
   */
  private static Person currentPerson() {
    val person = currentUser().getPerson();
    if (person == null) {
      response.status = 403;
      renderJSON(gsonBuilder.create().toJson(ImmutableMap.of("error",
          "L'utente corrente non ha associata una persona")));
    }
    return person;
  }

  /**
   * La richiesta di assenza con l'id indicato, 404 se non esiste o non è del dipendente: non
   * si rivela l'esistenza delle richieste altrui.
   */
  private static AbsenceRequest ownAbsenceRequest(Long id) {
    val person = currentPerson();
    AbsenceRequest absenceRequest = id != null ? AbsenceRequest.findById(id) : null;
    if (absenceRequest == null || !absenceRequest.getPerson().equals(person)) {
      JsonResponse.notFound();
    }
    return absenceRequest;
  }
}
