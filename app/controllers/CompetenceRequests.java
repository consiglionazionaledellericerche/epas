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

package controllers;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import common.security.SecurityRules;
import dao.CompetenceRequestDao;
import dao.GroupDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import helpers.Web;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.NotificationManager;
import manager.configurations.ConfigurationManager;
import manager.flows.CompetenceRequestManager;
import models.Person;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.enumerate.CompetenceRequestEventType;
import models.flows.enumerate.CompetenceRequestType;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle richieste di straordinario dei dipendenti.
 *
 * @author Dario Tagliaferri
 */
@Slf4j
@With(Resecure.class)
public class CompetenceRequests extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static CompetenceRequestManager competenceRequestManager;
  @Inject
  static SecurityRules rules;
  @Inject
  static NotificationManager notificationManager;
  @Inject
  static GroupDao groupDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static CompetenceRequestDao competenceRequestDao;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  private static PersonReperibilityDayDao repDao;


  public static void changeReperibility() {
    list(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST);
  }

  public static void changeReperibilityToApprove() {
    listToApprove(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST);
  }

  /**
   * Lista delle richieste di straordinario dell'utente corrente.
   *
   * @param competenceType il tipo di competenza
   */
  public static void list(CompetenceRequestType competenceType) {
    Verify.verifyNotNull(competenceType);

    val currentUser = Security.getUser().get();
    if (currentUser.person == null) {
      flash.error("L'utente corrente non ha associata una persona, non può vedere le proprie "
          + "richieste di assenza");
      Application.index();
      return;
    }
    val person = currentUser.person;
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue();
    log.debug("Prelevo le richieste di tipo {} per {} a partire da {}",
        competenceType, person, fromDate);

    val config = competenceRequestManager.getConfiguration(competenceType, person);
    List<CompetenceRequest> myResults = competenceRequestDao
        .findByPersonAndDate(person, fromDate, Optional.absent(), competenceType, true);
    List<CompetenceRequest> closed = competenceRequestDao
        .findByPersonAndDate(person, fromDate, Optional.absent(), competenceType, false);
    val onlyOwn = true;
    boolean persist = false;

    render(config, myResults, competenceType, onlyOwn, persist, closed);
  }

  /**
   * Lista delle richieste di assenza da approvare da parte dell'utente corrente.
   *
   * @param type tipo opzionale di tipo di richiesta di assenza.
   */
  public static void listToApprove(CompetenceRequestType type) {
    Verify.verifyNotNull(type);

    val person = Security.getUser().get().person;
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue();
    log.debug("Prelevo le richieste da approvare  di tipo {} a partire da {}",
        type, fromDate);
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(person.user);
    List<CompetenceRequest> results = competenceRequestDao
        .allResults(roleList, fromDate, Optional.absent(), type, person);
    List<CompetenceRequest> myResults =
        competenceRequestDao.toApproveResults(roleList, fromDate, Optional.absent(),
            type, person);
    List<CompetenceRequest> approvedResults =
        competenceRequestDao
            .totallyApproved(roleList, fromDate, Optional.absent(), type, person);
    val config = competenceRequestManager.getConfiguration(type, person);
    val onlyOwn = false;
    val available = person.user.hasRoles(Role.REPERIBILITY_MANAGER) ? false : true;
    render(config, results, type, onlyOwn, available, approvedResults, myResults);
  }

  /**
   * Ritorna la form di richiesta di nuova competenza.
   *
   * @param personId       l'id della persona che sta richiedendo la competenza
   * @param year           l'anno
   * @param month          il mese
   * @param competenceType il tipo di richiesta competenza
   */
  public static void blank(Optional<Long> personId, int year, int month,
      CompetenceRequestType competenceType) {
    Verify.verifyNotNull(competenceType);
    Person person;
    if (personId.isPresent()) {
      rules.check("CompetenceRequests.blank4OtherPerson");
      person = personDao.getPersonById(personId.get());
    } else {
      if (Security.getUser().isPresent() && Security.getUser().get().person != null) {
        person = Security.getUser().get().person;
      } else {
        flash.error("L'utente corrente non ha associato una persona.");
        list(competenceType);
        return;
      }
    }
    notFoundIfNull(person);

    val configurationProblems = competenceRequestManager.checkconfiguration(competenceType, person);
    if (!configurationProblems.isEmpty()) {
      flash.error(Joiner.on(" ").join(configurationProblems));
      list(competenceType);
      return;
    }

    val competenceRequest = new CompetenceRequest();
    competenceRequest.type = competenceType;
    competenceRequest.person = person;
    PersonReperibilityType type = null;
    List<Person> teamMates = Lists.newArrayList();
    List<PersonReperibilityType> types = Lists.newArrayList();
    boolean insertable = false;
    if (competenceType.equals(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST)) {
      types = repDao.getReperibilityTypeByOffice(person.office, Optional.of(false))
          .stream().filter(prt -> prt.personReperibilities.stream()
              .anyMatch(pr -> pr.person.equals(person)))
          .collect(Collectors.toList());
      //ritorno solo il primo elemento della lista con la lista dei dipendenti afferenti al servizio

      type = types.get(0);
      teamMates = type.personReperibilities.stream()
          .filter(pr -> pr.isActive(new YearMonth(year, month)))
          .map(pr -> pr.person)
          .filter(p -> p.id != person.id).collect(Collectors.toList());

    }
    competenceRequest.startAt = competenceRequest.endTo = LocalDateTime.now().plusDays(1);
    render("@edit", competenceRequest, insertable, competenceType,
        year, month, type, teamMates, types);
  }

  /**
   * Ritorna la form di richiesta cambio di reperibilità aggiornata coi dati richiesti.
   *
   * @param competenceRequest la richiesta di competenza
   * @param year              l'anno di riferimento
   * @param month             il mese di riferimento
   * @param type              il servizio di reperibilità
   * @param teamMate          la persona selezionata per il cambio di reperibilità
   * @param beginDayToAsk     la data da chiedere
   * @param beginDayToGive    la data da cedere
   */
  public static void edit(CompetenceRequest competenceRequest, int year, int month,
      PersonReperibilityType type, Person teamMate, PersonReperibilityDay beginDayToAsk,
      PersonReperibilityDay beginDayToGive, PersonReperibilityDay endDayToGive,
      PersonReperibilityDay endDayToAsk) {

    rules.checkIfPermitted(type);
    competenceRequest.person = Security.getUser().get().person;
    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate to = begin.dayOfMonth().withMaximumValue();
    List<PersonReperibilityDay> reperibilityDates = repDao
        .getPersonReperibilityDaysByPeriodAndType(begin, to, type, teamMate);
    List<PersonReperibilityDay> myReperibilityDates = repDao
        .getPersonReperibilityDaysByPeriodAndType(begin, to, type, competenceRequest.person);

    List<PersonReperibilityType> types = repDao
        .getReperibilityTypeByOffice(competenceRequest.person.office, Optional.of(false))
        .stream().filter(prt -> prt.personReperibilities.stream()
            .anyMatch(pr -> pr.person.equals(competenceRequest.person)))
        .collect(Collectors.toList());
    List<Person> teamMates = type.personReperibilities.stream().map(pr -> pr.person)
        .filter(p -> p.id != competenceRequest.person.id).collect(Collectors.toList());
    boolean insertable = true;

    render(competenceRequest, insertable, reperibilityDates, type, teamMate,
        month, year, teamMates, types, beginDayToAsk, myReperibilityDates, beginDayToGive,
        endDayToGive, endDayToAsk);

  }

  /**
   * Metodo che permette il salvataggio di una richiesta di competenza.
   *
   * @param competenceRequest la richiesta di competenza da salvare
   * @param year              l'anno di riferimento
   * @param month             il mese di riferimento
   * @param teamMate          la persona destinataria della richiesta
   * @param beginDayToAsk     il giorno iniziale da chiedere
   * @param beginDayToGive    il giorno iniziale da dare
   * @param endDayToGive      il giorno finale da dare
   * @param endDayToAsk       il giorno finale da chiedere
   * @param type              il servizio su cui chiedere il cambio
   */
  public static void save(@Required CompetenceRequest competenceRequest, int year,
      int month, Person teamMate, Optional<PersonReperibilityDay> beginDayToAsk,
      PersonReperibilityDay beginDayToGive, PersonReperibilityDay endDayToGive,
      Optional<PersonReperibilityDay> endDayToAsk, PersonReperibilityType type) {

    //rules.checkIfPermitted(type);

    Verify.verifyNotNull(beginDayToGive.date);
    Verify.verifyNotNull(endDayToGive.date);

    competenceRequest.year = year;
    competenceRequest.month = month;
    competenceRequest.startAt = LocalDateTime.now();
    competenceRequest.teamMate = teamMate;
    competenceRequest.person = Security.getUser().get().person;

    notFoundIfNull(competenceRequest.person);

    CompetenceRequest existing = competenceRequestManager.checkCompetenceRequest(competenceRequest);
    if (existing != null) {
      Validation.addError("teamMate",
          "Esiste già una richiesta di questo tipo");
    }

    if (beginDayToGive.date.isAfter(endDayToGive.date)) {
      Validation.addError("beginDayToGive", "Le date devono essere congruenti");
    }

    val dayToAskBegin = beginDayToAsk.isPresent() ? beginDayToAsk.get() : null;
    val dayToAskEnd = endDayToAsk.isPresent() ? endDayToAsk.get() : null;

    if (dayToAskBegin != null && dayToAskEnd != null) {
      if (dayToAskBegin.date.isAfter(dayToAskEnd.date)) {
        Validation.addError("beginDayToAsk", "Le date devono essere congruenti");
      }
      if (Days.daysBetween(dayToAskBegin.date, dayToAskEnd.date).getDays()
          != Days.daysBetween(beginDayToGive.date, endDayToGive.date).getDays()) {
        Validation.addError("beginDayToAsk",
            "La quantità di giorni da chiedere e da dare deve coincidere");
        Validation.addError("beginDayToGive",
            "La quantità di giorni da chiedere e da dare deve coincidere");
      }
    }
    if (!competenceRequest.person.checkLastCertificationDate(
        new YearMonth(competenceRequest.year,
            competenceRequest.month))) {
      Validation.addError("beginDayToAsk",
          "Non è possibile fare una richiesta per una data di un mese già "
              + "processato in Attestati");
    }
    if (Validation.hasErrors()) {
      LocalDate begin = new LocalDate(year, month, 1);
      LocalDate to = begin.dayOfMonth().withMaximumValue();
      List<PersonReperibilityDay> reperibilityDates = repDao
          .getPersonReperibilityDaysByPeriodAndType(begin, to, type, teamMate);
      List<PersonReperibilityDay> myReperibilityDates = repDao
          .getPersonReperibilityDaysByPeriodAndType(begin, to, type, competenceRequest.person);

      List<PersonReperibilityType> types = repDao
          .getReperibilityTypeByOffice(competenceRequest.person.office, Optional.of(false))
          .stream().filter(prt -> prt.personReperibilities.stream()
              .anyMatch(pr -> pr.person.equals(competenceRequest.person)))
          .collect(Collectors.toList());
      List<Person> teamMates = type.personReperibilities.stream().map(pr -> pr.person)
          .filter(p -> p.id != competenceRequest.person.id).collect(Collectors.toList());
      boolean insertable = true;
      response.status = 400;
      render("@edit", competenceRequest, beginDayToAsk, beginDayToGive,
          endDayToAsk, endDayToGive, type, year, month, teamMate, insertable,
          teamMates, types, reperibilityDates, myReperibilityDates);
    }

    competenceRequest.beginDateToAsk = beginDayToAsk.isPresent() ? dayToAskBegin.date : null;
    competenceRequest.endDateToAsk = endDayToAsk.isPresent() ? dayToAskEnd.date : null;
    competenceRequest.beginDateToGive = beginDayToGive.date;
    competenceRequest.endDateToGive = endDayToGive.date;

    competenceRequestManager.configure(competenceRequest);

    if (competenceRequest.endTo == null) {
      competenceRequest.endTo = competenceRequest.startAt;
    }

    boolean isNewRequest = !competenceRequest.isPersistent();
    competenceRequest.save();

    //Avvia il flusso se necessario.
    if (isNewRequest || !competenceRequest.flowStarted) {
      competenceRequestManager.executeEvent(
          competenceRequest, competenceRequest.person,
          CompetenceRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
    }
    flash.success("Operazione effettuata correttamente");

    CompetenceRequests.list(competenceRequest.type);
  }

  /**
   * Metodo per l'eliminazione della richiesta di competenza.
   *
   * @param id l'identificativo della richiesta d competenza da eliminare
   */
  public static void delete(long id) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    notFoundIfNull(competenceRequest);
    rules.checkIfPermitted(competenceRequest);
    competenceRequestManager.executeEvent(
        competenceRequest, Security.getUser().get().person,
        CompetenceRequestEventType.DELETE, Optional.absent());
    flash.success(Web.msgDeleted(AbsenceRequest.class));
    list(competenceRequest.type);
  }

  /**
   * Renderizza la form in cui viene mostrata la richiesta.
   *
   * @param id   l'identificativo della richiesta
   * @param type il tipo di richiesta
   */
  public static void show(long id, CompetenceRequestType type) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    notFoundIfNull(competenceRequest);
    rules.checkIfPermitted(competenceRequest);
    User user = Security.getUser().get();
    boolean disapproval = false;
    render(competenceRequest, type, user, disapproval);
  }

  /**
   * Permette l'approvazione della richiesta.
   *
   * @param id l'identificativo della richiesta
   */
  public static void approval(long id) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    notFoundIfNull(competenceRequest);
    User user = Security.getUser().get();
    rules.checkIfPermitted(competenceRequest);

    log.debug("Approving competence request {}", competenceRequest);
    boolean approved = competenceRequestManager.approval(competenceRequest, user);

    if (approved) {
      notificationManager
          .sendEmailToUser(Optional.absent(), Optional.fromNullable(competenceRequest),
              Optional.absent(), true);

      flash.success("Operazione conclusa correttamente");
    } else {
      flash.error("Problemi nel completare l'operazione contattare il supporto tecnico di ePAS.");
    }

    CompetenceRequests.listToApprove(competenceRequest.type);
  }

  /**
   * Metodo che permette il rifiuto della richiesta.
   *
   * @param id          identificativo della richiesta di competenza
   * @param disapproval true se si rifiuta, false altrimenti
   * @param reason      la motivazione al rifiuto
   */
  public static void disapproval(long id, boolean disapproval, String reason) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    User user = Security.getUser().get();
    if (!disapproval) {
      disapproval = true;
      render(competenceRequest, disapproval);
    }

    if (competenceRequest.reperibilityManagerApprovalRequired
        && competenceRequest.reperibilityManagerApproved == null
        && rules.check("CompetenceRequests.reperibilityManagerDisapproval", competenceRequest)) {
      log.debug("Disapproval da parte del REPERIBILITY_MANAGER, richiesta {}",
          competenceRequest);
      //TODO: caso di disapprovazione da parte del supervisore del servizio.
      competenceRequestManager.reperibilityManagerDisapproval(id, reason);
    }
    if (competenceRequest.employeeApprovalRequired && competenceRequest.employeeApproved == null
        && user.hasRoles(Role.EMPLOYEE)) {
      //TODO: caso di disapprovazione da parte del dipendente reperibile
      competenceRequestManager.employeeDisapproval(id, reason);
    }
    notificationManager
        .sendEmailToUser(Optional.absent(), Optional.fromNullable(competenceRequest),
            Optional.absent(), false);
    flash.error("Richiesta respinta");
    render("@show", competenceRequest, user);
  }
}