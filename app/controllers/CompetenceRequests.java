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

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import common.security.SecurityRules;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.CompetenceRequestDao;
import dao.GroupDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import helpers.Web;
import it.cnr.iit.epas.DateUtility;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.CertificationManager;
import manager.CompetenceManager;
import manager.NotificationManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.flows.CompetenceRequestManager;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Competence;
import models.CompetenceCode;
import models.GroupOvertime;
import models.Person;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.Role;
import models.TotalOvertime;
import models.User;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.Group;
import models.flows.enumerate.CompetenceRequestEventType;
import models.flows.enumerate.CompetenceRequestType;
import play.Play;
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
  @Inject
  private static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  private static CertificationManager certificationManager;
  @Inject
  static CompetenceDao competenceDao;
  @Inject
  static CompetenceCodeDao competenceCodeDao;
  @Inject
  static CompetenceManager competenceManager;
  
  static final String ATTESTATI_ACTIVE = "attestati.active";


  public static void changeReperibility() {
    list(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST);
  }

  public static void changeReperibilityToApprove() {
    listToApprove(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST);
  }

  public static void overtimes() {
    list(CompetenceRequestType.OVERTIME_REQUEST);
  }

  public static void overtimesToApprove() {
    listToApprove(CompetenceRequestType.OVERTIME_REQUEST);
  }
  
  public static void overtimesToApproveInAdvance() {
    listToApproveWithRequestInAdvance(CompetenceRequestType.OVERTIME_REQUEST);
  }

  /**
   * Lista delle richieste di straordinario dell'utente corrente.
   *
   * @param competenceType il tipo di competenza
   */
  public static void list(CompetenceRequestType competenceType) {
    Verify.verifyNotNull(competenceType);

    val currentUser = Security.getUser().get();
    if (currentUser.getPerson() == null) {
      flash.error("L'utente corrente non ha associata una persona, non può vedere le proprie "
          + "richieste di competenza");
      Application.index();
      return;
    }
    val person = currentUser.getPerson();
    List<TotalOvertime> totalList = 
        competenceDao.getTotalOvertime(LocalDate.now().getYear(), person.getOffice());
    int totale = competenceManager.getTotalOvertime(totalList);
    List<Group> groups = person.getGroups();
    List<GroupOvertime> list = groups.stream().flatMap(g -> g.getGroupOvertimes().stream()
        .filter(go -> go.getYear().equals(LocalDate.now().getYear())))
        .collect(Collectors.toList());
    if (person.totalOvertimeHourInYear(LocalDate.now().getYear()) == 0 
        && totale == 0 && list.isEmpty()) {
      flash.error("Non sono definiti monte ore per la sede, nè per la persona. "
          + "Definire i monte ore per poter procedere con la richiesta di straordinri.");
      Stampings.stampings(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
      
    }
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
   * Lista delle richieste di competenza da approvare da parte dell'utente corrente.
   *
   * @param type tipo opzionale di tipo di richiesta di assenza.
   */
  public static void listToApprove(CompetenceRequestType competenceType) {
    Verify.verifyNotNull(competenceType);

    val person = Security.getUser().get().getPerson();
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue();
    log.debug("Prelevo le richieste da approvare  di tipo {} a partire da {}",
        competenceType, fromDate);

    val config = competenceRequestManager.getConfiguration(competenceType, person);
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(person.getUser());
    List<CompetenceRequest> results = competenceRequestDao
        .allResults(roleList, fromDate, Optional.absent(), competenceType, person);
    List<CompetenceRequest> myResults =
        competenceRequestDao.toApproveResults(roleList, fromDate, Optional.absent(),
            competenceType, person);
    List<CompetenceRequest> approvedResults = competenceRequestDao
        .totallyApproved(roleList, fromDate, Optional.absent(), competenceType, person);
    if (config.isAdvanceApprovalRequired()) {
      approvedResults = myResults;
      myResults = myResults.stream().filter(cr -> cr.actualEvent().eventType
          .equals(CompetenceRequestEventType.STARTING_APPROVAL_FLOW)).collect(Collectors.toList());
      approvedResults = approvedResults.stream().filter(cr -> cr.actualEvent().eventType
          .equals(CompetenceRequestEventType.FIRST_APPROVAL)).collect(Collectors.toList());
    }
    val onlyOwn = false;
    boolean overtimesQuantityEnabled = (Boolean)configurationManager
        .configValue(person.getOffice(), 
            EpasParam.ENABLE_EMPLOYEE_REQUEST_OVERTIME_QUANTITY, LocalDate.now());
    boolean isDefinitevely = false;
    val available = person.getUser().hasRoles(Role.REPERIBILITY_MANAGER) ? false : true;
    render(config, results, competenceType, onlyOwn, available, approvedResults, 
        myResults, overtimesQuantityEnabled, isDefinitevely);
  }
  
  /**
   * 
   * @param competenceType
   */
  public static void listToApproveWithRequestInAdvance(CompetenceRequestType competenceType) {
    Verify.verifyNotNull(competenceType);
    val person = Security.getUser().get().getPerson();
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue();
    log.debug("Prelevo le richieste da approvare  di tipo {} a partire da {}",
        competenceType, fromDate);

    val config = competenceRequestManager.getConfiguration(competenceType, person);
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(person.getUser());
    List<CompetenceRequest> results = competenceRequestDao
        .allResults(roleList, fromDate, Optional.absent(), competenceType, person);
    List<CompetenceRequest> myResults =
        competenceRequestDao.toApproveResults(roleList, fromDate, Optional.absent(),
            competenceType, person);
    List<CompetenceRequest> approvedResults = competenceRequestDao
        .totallyApproved(roleList, fromDate, Optional.absent(), competenceType, person);
    if (config.isAdvanceApprovalRequired()) {
      myResults = myResults.stream().filter(cr -> cr.actualEvent().eventType
          .equals(CompetenceRequestEventType.FIRST_APPROVAL)).collect(Collectors.toList());      
    }
    val onlyOwn = false;
    boolean overtimesQuantityEnabled = (Boolean)configurationManager
        .configValue(person.getOffice(), 
            EpasParam.ENABLE_EMPLOYEE_REQUEST_OVERTIME_QUANTITY, LocalDate.now());
    boolean isDefinitevely = true;
    val available = person.getUser().hasRoles(Role.REPERIBILITY_MANAGER) ? false : true;
    render(config, results, competenceType, onlyOwn, available, approvedResults, 
        myResults, overtimesQuantityEnabled, isDefinitevely);
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
      if (Security.getUser().isPresent() && Security.getUser().get().getPerson() != null) {
        person = Security.getUser().get().getPerson();
      } else {
        flash.error("L'utente corrente non ha associato una persona.");
        list(competenceType);
        return;
      }
    }
    notFoundIfNull(person);

    val configurationProblems = competenceRequestManager
        .checkconfiguration(competenceType, person);
    if (!configurationProblems.isEmpty()) {
      flash.error(Joiner.on(" ").join(configurationProblems));
      list(competenceType);
      return;
    }

    val competenceRequest = new CompetenceRequest();
    competenceRequest.setType(competenceType);
    competenceRequest.setPerson(person);
    PersonReperibilityType type = null;
    List<Person> teamMates = Lists.newArrayList();
    List<PersonReperibilityType> types = Lists.newArrayList();
    PersonStampingRecap psDto = null;
    boolean enabledOvertimePerPerson = false;
    boolean isOvertime = false;
    int overtimeResidual = 0;
    switch (competenceType) {
      case CHANGE_REPERIBILITY_REQUEST:
        types = repDao.getReperibilityTypeByOffice(person.getOffice(), Optional.of(false))
        .stream().filter(prt -> prt.getPersonReperibilities().stream()
            .anyMatch(pr -> pr.getPerson().equals(person)))
        .collect(Collectors.toList());
        //ritorno solo il primo elemento della lista con la lista dei dipendenti afferenti 
        //al servizio

        type = types.get(0);
        teamMates = type.getPersonReperibilities().stream()
            .filter(pr -> pr.isActive(new YearMonth(year, month)))
            .map(pr -> pr.getPerson())
            .filter(p -> p.id != person.id).collect(Collectors.toList());
        break;
      case OVERTIME_REQUEST:
        // controllo con la chiamata rest ad attestati per verificare se l'attestato di 
        // quell'anno/mese è già stato validato (valido solo per istanza CNR)
        if ("true".equals(Play.configuration.getProperty(ATTESTATI_ACTIVE, "false"))) {
          /*
           * Nel caso di prove in locale, occorre cambiare l'indirizzo di Attestati nei parametri 
           * di configurazione del Play altrimenti viene lanciata un'eccezione a questa chiamata
           */
          try {
          val certData = certificationManager.getPersonCertData(person, year, month);
            if (certData.validate) {
              flash.error("Attestato già validato per l'anno/mese richiesto. "
                  + "Non si può procedere con una richiesta di straordinario.");
              list(competenceType);
              return;
            }
          } catch (Exception e) {
            log.warn("Impossibile effettuare il controllo di validazione dell'attestato " + 
                " del {}/{} per la richiesta di straordinari di {}", 
                month, year, person.getFullname());
          }
        }        
        isOvertime = true;
        
        if ((Boolean) configurationManager
            .configValue(person.getOffice(), EpasParam.ENABLE_OVERTIME_PER_PERSON)) {
          enabledOvertimePerPerson = true;
          CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
          List<CompetenceCode> codeList = Lists.newArrayList();
          codeList.add(code);
          overtimeResidual = person.totalOvertimeHourInYear(year) - 
              competenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.absent(), 
                  Optional.fromNullable(person), codeList).or(0);
        } 
        psDto = stampingsRecapFactory.create(person, year, month, true);  
        break;
      default:
        break;
    }

    boolean insertable = false;
    competenceRequest.setStartAt(LocalDateTime.now().plusDays(1));
    competenceRequest.setEndTo(LocalDateTime.now().plusDays(1));
    render("@edit", competenceRequest, insertable, competenceType,
        year, month, type, teamMates, types, psDto, isOvertime, 
        enabledOvertimePerPerson, overtimeResidual);
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
    competenceRequest.setPerson(Security.getUser().get().getPerson());
    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate to = begin.dayOfMonth().withMaximumValue();
    List<PersonReperibilityDay> reperibilityDates = repDao
        .getPersonReperibilityDaysByPeriodAndType(begin, to, type, teamMate);
    List<PersonReperibilityDay> myReperibilityDates = repDao
        .getPersonReperibilityDaysByPeriodAndType(begin, to, type, competenceRequest.getPerson());

    List<PersonReperibilityType> types = repDao
        .getReperibilityTypeByOffice(competenceRequest.getPerson().getOffice(), Optional.of(false))
        .stream().filter(prt -> prt.getPersonReperibilities().stream()
            .anyMatch(pr -> pr.getPerson().equals(competenceRequest.getPerson())))
        .collect(Collectors.toList());
    List<Person> teamMates = type.getPersonReperibilities().stream().map(pr -> pr.getPerson())
        .filter(p -> p.id != competenceRequest.getPerson().id).collect(Collectors.toList());
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
      int month, Person teamMate, PersonReperibilityDay beginDayToAsk,
      PersonReperibilityDay beginDayToGive, PersonReperibilityDay endDayToGive,
      PersonReperibilityDay endDayToAsk, PersonReperibilityType type) {

    notFoundIfNull(competenceRequest.getPerson());

    //rules.checkIfPermitted(type);
    if (competenceRequest.getType().equals(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST)) {
      Verify.verifyNotNull(beginDayToGive.getDate());
      Verify.verifyNotNull(endDayToGive.getDate());
      if (beginDayToGive.getDate().isAfter(endDayToGive.getDate())) {
        Validation.addError("beginDayToGive", "Le date devono essere congruenti");
      }

      val beginDateToAsk = beginDayToAsk != null ? beginDayToAsk.getDate() : null;
      val endDateToAsk = endDayToAsk != null ? endDayToAsk.getDate() : null;
      if (beginDateToAsk != null && endDateToAsk != null) {
        if (beginDateToAsk.isAfter(endDateToAsk)) {
          Validation.addError("beginDayToAsk", "Le date devono essere congruenti");
        }
        if (Days.daysBetween(beginDateToAsk, endDateToAsk).getDays()
            != Days.daysBetween(beginDayToGive.getDate(), endDayToGive.getDate()).getDays()) {
          Validation.addError("beginDayToAsk",
              "La quantità di giorni da chiedere e da dare deve coincidere");
          Validation.addError("beginDayToGive",
              "La quantità di giorni da chiedere e da dare deve coincidere");
        }
      }
      competenceRequest.setBeginDateToAsk(beginDateToAsk);
      competenceRequest.setEndDateToAsk(endDateToAsk);
      competenceRequest.setBeginDateToGive(beginDayToGive.getDate());
      competenceRequest.setEndDateToGive(endDayToGive.getDate());
      competenceRequest.setTeamMate(teamMate);
    } else {
      
      //log.debug("Richiesta di straordinario per un mese concluso {}/[} per {}", 
      //    month, year, competenceRequest.getPerson().getFullname());
      if ((Boolean) configurationManager.configValue(competenceRequest.getPerson().getOffice(), 
          EpasParam.OVERTIME_ADVANCE_REQUEST_AND_CONFIRMATION, LocalDate.now())) {
        if (month < LocalDate.now().getMonthOfYear()) {
          Validation.addError("competenceRequest.note", 
              "E' prevista richiesta preventiva. "
              + "Non si può richiedere straordinario per un mese concluso!");
        }
      }
    }

    competenceRequest.setYear(year);
    competenceRequest.setMonth(month);
    competenceRequest.setStartAt(LocalDateTime.now());
    
    competenceRequest.setPerson(Security.getUser().get().getPerson());

    CompetenceRequest existing = competenceRequestManager
        .checkCompetenceRequest(competenceRequest);
    if (existing != null) {
      Validation.addError("competenceRequest.note",
          "Esiste già una richiesta di questo tipo");
    }
    CompetenceRequestType competenceType = competenceRequest.getType();
    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    Optional<Competence> competence = competenceDao
        .getCompetence(competenceRequest.getPerson(), year, month, code);
    if (competence.isPresent()) {
      String mese = DateUtility.fromIntToStringMonth(month);
      flash.error("Esiste già un'attribuzione di %s ore di straordinario per %s %s. "
          + "Verificare con la propria segreteria del personale prima di "
          + "sottomettere una nuova richiesta", competence.get().getValueApproved(), mese, year);
      CompetenceRequests.list(competenceType);
    }
      
    if (Validation.hasErrors()) {

      if (competenceRequest.getType().equals(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST)) {
        LocalDate begin = new LocalDate(year, month, 1);
        LocalDate to = begin.dayOfMonth().withMaximumValue();
        List<PersonReperibilityDay> reperibilityDates = repDao
            .getPersonReperibilityDaysByPeriodAndType(begin, to, type, teamMate);
        List<PersonReperibilityDay> myReperibilityDates = repDao
            .getPersonReperibilityDaysByPeriodAndType(begin, to, type, 
                competenceRequest.getPerson());

        List<PersonReperibilityType> types = repDao
            .getReperibilityTypeByOffice(competenceRequest.getPerson()
                .getOffice(), Optional.of(false))
            .stream().filter(prt -> prt.getPersonReperibilities().stream()
                .anyMatch(pr -> pr.getPerson().equals(competenceRequest.getPerson())))
            .collect(Collectors.toList());
        List<Person> teamMates = type.getPersonReperibilities().stream().map(pr -> pr.getPerson())
            .filter(p -> p.id != competenceRequest.getPerson().id).collect(Collectors.toList());
        boolean insertable = true;
        response.status = 400;
        render("@edit", competenceRequest, beginDayToAsk, beginDayToGive,
            endDayToAsk, endDayToGive, type, year, month, teamMate, insertable,
            teamMates, types, reperibilityDates, myReperibilityDates);
      } else {
        boolean isOvertime = true;
        
        PersonStampingRecap psDto = stampingsRecapFactory
            .create(competenceRequest.getPerson(), year, month, true);
        render("@edit", competenceRequest, competenceType,
            year, month, type, psDto, isOvertime);
      }
      
    }

    competenceRequestManager.configure(competenceRequest);

    if (competenceRequest.getEndTo() == null) {
      competenceRequest.setEndTo(competenceRequest.getStartAt());
    }

    boolean isNewRequest = !competenceRequest.isPersistent();
    competenceRequest.save();

    //Avvia il flusso se necessario.
    if (isNewRequest || !competenceRequest.isFlowStarted()) {
      competenceRequestManager.executeEvent(
          competenceRequest, competenceRequest.getPerson(),
          CompetenceRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
    }
    flash.success("Operazione effettuata correttamente");
    
    CompetenceRequests.list(competenceType);
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
        competenceRequest, Security.getUser().get().getPerson(),
        CompetenceRequestEventType.DELETE, Optional.absent());
    flash.success(Web.msgDeleted(AbsenceRequest.class));
    list(competenceRequest.getType());
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
    boolean showOvertimeAvailableHours = false;
    int hoursAvailable = 0;
    User user = Security.getUser().get();
    if (user.hasRoles(Role.GROUP_MANAGER) || user.hasRoles(Role.SEAT_SUPERVISOR)) {
      hoursAvailable = competenceRequestManager.hoursAvailable(user, competenceRequest);
      showOvertimeAvailableHours = true;
    }
    boolean disapproval = false;
    int month = competenceRequest.getMonth();
    PersonStampingRecap psDto = null;
    if (competenceRequest.getType().equals(CompetenceRequestType.OVERTIME_REQUEST)) {
      psDto = stampingsRecapFactory
          .create(competenceRequest.getPerson(), competenceRequest.getYear(), month, true);
    }    
    render(competenceRequest, type, user, disapproval, month, psDto, 
        showOvertimeAvailableHours, hoursAvailable);
  }

  /**
   * Permette l'approvazione della richiesta.
   *
   * @param id l'identificativo della richiesta
   */
  public static void approval(long id, boolean approval, Integer value) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    notFoundIfNull(competenceRequest);
    User user = Security.getUser().get();
    rules.checkIfPermitted(competenceRequest);
    if (competenceRequest.getType().equals(CompetenceRequestType.OVERTIME_REQUEST)) {
      boolean showOvertimeAvailableHours = true;
      competenceRequest.save();
      if (!approval) {
        
        approval = true;
        int month = competenceRequest.getMonth();
        PersonStampingRecap psDto = stampingsRecapFactory
            .create(competenceRequest.getPerson(), competenceRequest.getYear(), 
                competenceRequest.getMonth(), true);

        int hoursAvailable = competenceRequestManager.hoursAvailable(user, competenceRequest);
        render(competenceRequest, approval, psDto, month, hoursAvailable, 
            showOvertimeAvailableHours);
      }
    }    
    
    log.debug("Approving competence request {}", competenceRequest);
    if (competenceRequest.getType().equals(CompetenceRequestType.OVERTIME_REQUEST)) {
      int hoursAvailable = competenceRequestManager.hoursAvailable(user, competenceRequest);
      int month = competenceRequest.getMonth();
      approval = true;
      boolean showOvertimeAvailableHours = true;
      PersonStampingRecap psDto = stampingsRecapFactory
          .create(competenceRequest.getPerson(), competenceRequest.getYear(), 
              competenceRequest.getMonth(), true);
      if (value == null) {
        Validation.addError("value", "Inserire una quantità!!");  
        response.status = 400;
        if (Validation.hasErrors()) {
          render("@approval", competenceRequest, approval, hoursAvailable, month, psDto, 
              showOvertimeAvailableHours);
        }
      }
      
      if (hoursAvailable - value < 0) {
        Validation.addError("value", "La quantità non può essere assegnata perchè non ci sono "
            + "sufficienti ore disponibili!");        
        response.status = 400;
        if (Validation.hasErrors()) {
          render("@approval", competenceRequest, approval, hoursAvailable, month, psDto, 
              showOvertimeAvailableHours);
        }
      }

      competenceRequest.setValue(value);
      log.debug("Cambiato valore alla competenza da {} a {}",competenceRequest.getValue(), value);
      competenceRequest.save();
    }  
    
    boolean approved = competenceRequestManager.approval(competenceRequest, user);

    if (approved) {
      notificationManager
      .sendEmailToUser(Optional.absent(), Optional.fromNullable(competenceRequest),
          Optional.absent(), true);

      flash.success("Operazione conclusa correttamente");
    } else {
      flash.error("Problemi nel completare l'operazione contattare il supporto tecnico di ePAS.");
    }
    
    listToApprove(competenceRequest.getType());
   
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
      int month = competenceRequest.getMonth();
      PersonStampingRecap psDto = stampingsRecapFactory
          .create(competenceRequest.getPerson(), competenceRequest.getYear(), 
              competenceRequest.getMonth(), true);
      render(competenceRequest, disapproval, month, psDto);
    }

    if (competenceRequest.isManagerApprovalRequired()
        && competenceRequest.getManagerApproved() == null
        && rules.check("CompetenceRequests.reperibilityManagerDisapproval", competenceRequest)) {
      log.debug("Disapproval da parte del REPERIBILITY_MANAGER, richiesta {}",
          competenceRequest);
      //TODO: caso di disapprovazione da parte del supervisore del servizio.
      competenceRequestManager.reperibilityManagerDisapproval(id, reason);
    }
    if (competenceRequest.isEmployeeApprovalRequired() 
        && competenceRequest.getEmployeeApproved() == null
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

