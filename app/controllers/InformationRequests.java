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
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import dao.InformationRequestDao;
import dao.PersonDao;
import dao.PersonDao.PersonLite;
import dao.TeleworkValidationDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.Web;
import helpers.validators.StringIsTime;
import it.cnr.iit.epas.DateUtility;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.NotificationManager;
import manager.TeleworkStampingManager;
import manager.configurations.EpasParam;
import manager.flows.InformationRequestManager;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Person;
import models.Role;
import models.TeleworkValidation;
import models.User;
import models.UsersRolesOffices;
import models.base.InformationRequest;
import models.dto.NewTeleworkDto;
import models.dto.TeleworkApprovalDto;
import models.enumerate.InformationType;
import models.flows.enumerate.InformationRequestEventType;
import models.informationrequests.IllnessRequest;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
import org.joda.time.YearMonth;
import play.data.validation.CheckWith;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

/**
 * Controller per la gestione delle richieste di flusso informativo dei dipendenti.
 *
 * @author dario
 */
@Slf4j
@With(Resecure.class)
public class InformationRequests extends Controller {
  
  @Inject
  static InformationRequestManager informationRequestManager;
  @Inject
  static InformationRequestDao informationRequestDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static PersonDao personDao;
  @Inject
  static NotificationManager notificationManager;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  static TeleworkStampingManager teleworkStampingManager;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  static TeleworkValidationDao validationDao;
  @Inject
  static TeleworkStampingManager manager;

  public static void teleworks() {
    list(InformationType.TELEWORK_INFORMATION);
  }
  
  public static void illness() {
    list(InformationType.ILLNESS_INFORMATION);
  }
  
  public static void serviceExit() {
    list(InformationType.SERVICE_INFORMATION);
  }
  
  public static void teleworksToApprove() {
    listToApprove(InformationType.TELEWORK_INFORMATION);
  }
  
  public static void illnessToApprove() {
    listToApprove(InformationType.ILLNESS_INFORMATION);
  }
  
  public static void serviceExitToApprove() {
    listToApprove(InformationType.SERVICE_INFORMATION);
  }

  /**
   * Ritorna la lista delle richieste del tipo passato come parametro.
   * 
   * @param type il tipo di flusso informativo da richiedere
   */
  public static void list(InformationType type) {
    Verify.verifyNotNull(type);

    val currentUser = Security.getUser().get();
    if (currentUser.person == null) {
      flash.error("L'utente corrente non ha associata una persona, non può vedere le proprie "
          + "richieste di assenza");
      Application.index();
      return;
    }
    val person = currentUser.person;
    val fromDate = LocalDateTime.now().withDayOfYear(1).withMonth(1).minusMonths(1);
    log.debug("Prelevo le richieste di tipo {} per {} a partire da {}", type, person,
        fromDate);
    val config = informationRequestManager.getConfiguration(type, person);
    List<TeleworkRequest> teleworks = Lists.newArrayList();
    List<TeleworkRequest> teleworksClosed = Lists.newArrayList();
    List<ServiceRequest> services = Lists.newArrayList();
    List<ServiceRequest> servicesClosed = Lists.newArrayList();
    List<IllnessRequest> illness = Lists.newArrayList();
    List<IllnessRequest> illnessClosed = Lists.newArrayList();
    switch (type) {
      case TELEWORK_INFORMATION:
        teleworks = informationRequestDao.teleworksByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.TELEWORK_INFORMATION, true);
        teleworksClosed = informationRequestDao.teleworksByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.TELEWORK_INFORMATION, false);
        break;
      case ILLNESS_INFORMATION:
        illness = informationRequestDao.illnessByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.ILLNESS_INFORMATION, true);
        illnessClosed = informationRequestDao.illnessByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.ILLNESS_INFORMATION, false);
        break;
      case SERVICE_INFORMATION:
        services = informationRequestDao.servicesByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.SERVICE_INFORMATION, true);
        servicesClosed = informationRequestDao.servicesByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.SERVICE_INFORMATION, false);
        break;
      default:
        log.info("Passato argomento non conosciuto");
        break;
    }
    render(teleworks, services, illness, teleworksClosed, 
        illnessClosed, servicesClosed, config, type);
  }
  
  /**
   * Genera la pagina con tutte le richieste di flusso informativo del tipo passato
   * come parametro.
   * 
   * @param type la tipologia di flusso informativo.
   */
  public static void listToApprove(InformationType type) {
    Verify.verifyNotNull(type);

    val person = Security.getUser().get().person;
    val fromDate = LocalDateTime.now().withDayOfYear(1).withMonth(1).minusMonths(1);
    log.debug("Prelevo le richieste da approvare di assenze di tipo {} a partire da {}", type,
        fromDate);
    
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(person.user);
    List<InformationRequest> myResults = 
        informationRequestDao.toApproveResults(roleList, Optional.absent(),
        Optional.absent(), type, person);
    List<InformationRequest> approvedResults = 
        informationRequestDao.totallyApproved(roleList, fromDate,
        Optional.absent(), type, person);
    
    List<Long> idMyResults = myResults.stream().map(ir -> ir.id).collect(Collectors.toList());
    List<Long> idApprovedResults = approvedResults.stream().map(ir -> ir.id)
        .collect(Collectors.toList());
    List<IllnessRequest> myIllnessResult = Lists.newArrayList();
    List<IllnessRequest> illnessApprovedResult = Lists.newArrayList();
    List<ServiceRequest> myServiceResult = Lists.newArrayList();
    List<ServiceRequest> serviceApprovedResult = Lists.newArrayList();
    List<TeleworkRequest> myTeleworkResult = Lists.newArrayList();
    List<TeleworkRequest> teleworkApprovedResult = Lists.newArrayList();
    switch (type) {
      case ILLNESS_INFORMATION:
        myIllnessResult = informationRequestDao.illnessByIds(idMyResults);
        illnessApprovedResult = informationRequestDao.illnessByIds(idApprovedResults);
        break;
      case SERVICE_INFORMATION:
        myServiceResult = informationRequestDao.servicesByIds(idMyResults);
        serviceApprovedResult = informationRequestDao.servicesByIds(idApprovedResults);
        break;
      case TELEWORK_INFORMATION:
        myTeleworkResult = informationRequestDao.teleworksByIds(idMyResults);
        teleworkApprovedResult = informationRequestDao.teleworksByIds(idApprovedResults);
        break;
      default:
        break;
    }
    val config = informationRequestManager.getConfiguration(type, person);
    val onlyOwn = false;

    render(config, type, onlyOwn, approvedResults, myResults, 
        myIllnessResult, illnessApprovedResult, myServiceResult, serviceApprovedResult,
        myTeleworkResult, teleworkApprovedResult);
  }
  
  /**
   * Crea la pagina di inserimento di una nuova richiesta di flusso informativo.
   * 
   * @param personId l'identificativo della persona
   * @param type la tipologia di flusso informativo da generare
   */
  public static void blank(Optional<Long> personId, InformationType type) {
    Verify.verifyNotNull(type);
    
    Person person;
    if (personId.isPresent()) {
      rules.check("AbsenceRequests.blank4OtherPerson");
      person = personDao.getPersonById(personId.get());
    } else {
      if (Security.getUser().isPresent() && Security.getUser().get().person != null) {
        person = Security.getUser().get().person;
      } else {
        flash.error("L'utente corrente non ha associato una persona.");
        list(type);
        return;
      }
    }
    notFoundIfNull(person);
    
    val configurationProblems = informationRequestManager.checkconfiguration(type, person);
    if (!configurationProblems.isEmpty()) {
      flash.error(Joiner.on(" ").join(configurationProblems));
      list(type);
      return;
    }
    ServiceRequest serviceRequest = new ServiceRequest();
    IllnessRequest illnessRequest = new IllnessRequest();
    switch (type) {
      case SERVICE_INFORMATION:
        serviceRequest.person = person;
        serviceRequest.informationType = type;
        render("@editServiceRequest", serviceRequest, type, person);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest.person = person;
        illnessRequest.informationType = type;
        render("@editIllnessRequest", illnessRequest, type, person);
        break;
      default: 
        break;
    }
  }
  
  public static void editServiceRequest(ServiceRequest serviceRequest, InformationType type,
      @CheckWith(StringIsTime.class) String begin, @CheckWith(StringIsTime.class) String finish) {
    
    
  }
  
  public static void editIllnessRequest(IllnessRequest illnessRequest, boolean retroactiveAbsence) {
    
  }
  
  /**
   * Persiste la richiesta di uscita di servizio e avvia il flusso approvativo.
   * 
   * @param serviceRequest la richiesta di uscita di servizio
   * @param begin l'orario di inizio
   * @param finish l'orario di fine
   */
  public static void saveServiceRequest(ServiceRequest serviceRequest,
      @CheckWith(StringIsTime.class) String begin, @CheckWith(StringIsTime.class) String finish) {
    InformationType type = serviceRequest.informationType;
    boolean insertable = true;
    if (Validation.hasErrors()) {      
      response.status = 400;
      insertable = false;
      render("@editServiceRequest", serviceRequest, insertable, begin, finish, type);
    }
    serviceRequest.beginAt = informationRequestManager.deparseTime(begin);
    serviceRequest.finishTo = informationRequestManager.deparseTime(finish);
    if (serviceRequest.beginAt == null || serviceRequest.finishTo == null) {
      Validation.addError("serviceRequest.beginAt",
          "Entrambi i campi data devono essere valorizzati");
      Validation.addError("serviceRequest.finishTo",
          "Entrambi i campi data devono essere valorizzati");
      response.status = 400;
      insertable = false;
      render("@editServiceRequest", serviceRequest, insertable, begin, finish, type);
    }
    if (serviceRequest.beginAt.isAfter(serviceRequest.finishTo)) {
      Validation.addError("serviceRequest.beginAt", 
          "L'orario di inizio non può essere successivo all'orario di fine");
      response.status = 400;
      insertable = false;
      render("@editServiceRequest", serviceRequest, insertable, begin, finish, type);
    }
    informationRequestManager.configure(Optional.absent(), 
        Optional.of(serviceRequest), Optional.absent());
    serviceRequest.startAt = LocalDateTime.now();
    serviceRequest.save();
    
    boolean isNewRequest = !serviceRequest.isPersistent();
    if (isNewRequest || !serviceRequest.flowStarted) {
      informationRequestManager.executeEvent(Optional.fromNullable(serviceRequest), 
          Optional.absent(), Optional.absent(), serviceRequest.person,
          InformationRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
      if (serviceRequest.autoApproved()) {
        informationRequestManager.executeEvent(Optional.fromNullable(serviceRequest), 
            Optional.absent(), Optional.absent(), serviceRequest.person, 
            InformationRequestEventType.COMPLETE, Optional.absent());
      }
      if (serviceRequest.person.isSeatSupervisor()) {
        approval(serviceRequest.id);
      } else {
        // invio la notifica al primo che deve validare la mia richiesta
        notificationManager.notificationInformationRequestPolicy(serviceRequest.person.user,
            serviceRequest, true);
        // invio anche la mail
        notificationManager.sendEmailInformationRequestPolicy(serviceRequest.person.user,
            serviceRequest, true);
        log.debug("Inviata la richiesta di approvazione");
      }
    }
    flash.success("Operazione effettuata correttamente");
    InformationRequests.list(serviceRequest.informationType);
    
  }
  
  /**
   * Persiste la richiesta e avvia il flusso informativo.
   * 
   * @param illnessRequest la richiesta informativa di malattia
   */
  public static void saveIllnessRequest(IllnessRequest illnessRequest) {
    InformationType type = illnessRequest.informationType;
    if (illnessRequest.beginDate == null || illnessRequest.endDate == null) {
      Validation.addError("illnessRequest.beginDate",
          "Entrambi i campi data devono essere valorizzati");
      Validation.addError("illnessRequest.endDate",
          "Entrambi i campi data devono essere valorizzati");
      response.status = 400;
      
      render("@editIllnessRequest", illnessRequest, type);
    }
    if (illnessRequest.beginDate.isAfter(illnessRequest.endDate)) {
      Validation.addError("illnessRequest.beginDate", 
          "La data di inizio non può essere successiva alla data di fine");
      response.status = 400;
      render("@editIllnessRequest", illnessRequest, type);
    }
    informationRequestManager.configure(Optional.of(illnessRequest), 
        Optional.absent(), Optional.absent());
    illnessRequest.startAt = LocalDateTime.now();
    illnessRequest.save();
    boolean isNewRequest = !illnessRequest.isPersistent();
    if (isNewRequest || !illnessRequest.flowStarted) {
      informationRequestManager.executeEvent(Optional.absent(), 
          Optional.fromNullable(illnessRequest), Optional.absent(), illnessRequest.person,
          InformationRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
      if (illnessRequest.person.isSeatSupervisor()) {
        approval(illnessRequest.id);
      } else {
        // invio la notifica al primo che deve validare la mia richiesta
        notificationManager.notificationInformationRequestPolicy(illnessRequest.person.user,
            illnessRequest, true);
        // invio anche la mail
        notificationManager.sendEmailInformationRequestPolicy(illnessRequest.person.user,
            illnessRequest, true);
        log.debug("Inviata la richiesta di approvazione");
      }
    }
    flash.success("Operazione effettuata correttamente");
    InformationRequests.list(illnessRequest.informationType);
  }
  
  /**
   * Persiste la richiesta di telelavoro e avvia il flusso informativo.
   * 
   * @param personId l'identificativo della persona
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void saveTeleworkRequest(Long personId, int year, int month) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    TeleworkRequest teleworkRequest = new TeleworkRequest();
    teleworkRequest.year = year;
    teleworkRequest.month = month;
    teleworkRequest.person = person;
    teleworkRequest.startAt = LocalDateTime.now();
    teleworkRequest.informationType = InformationType.TELEWORK_INFORMATION;
    teleworkRequest.save();
    informationRequestManager.configure(Optional.absent(), 
        Optional.absent(), Optional.of(teleworkRequest));
    boolean isNewRequest = !teleworkRequest.isPersistent();
    if (isNewRequest || !teleworkRequest.flowStarted) {
      informationRequestManager.executeEvent(Optional.absent(), Optional.absent(),
          Optional.fromNullable(teleworkRequest), teleworkRequest.person,
          InformationRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
      if (teleworkRequest.person.isSeatSupervisor()) {
        approval(teleworkRequest.id);
      } else {
        // invio la notifica al primo che deve validare la mia richiesta
        notificationManager.notificationInformationRequestPolicy(teleworkRequest.person.user,
            teleworkRequest, true);
        // invio anche la mail
        notificationManager.sendEmailInformationRequestPolicy(teleworkRequest.person.user,
            teleworkRequest, true);
        log.debug("Inviata la richiesta di approvazione");
      }
    }
    flash.success("Operazione effettuata correttamente");
    InformationRequests.list(teleworkRequest.informationType);
    
  }
  
  
  /**
   * Metodo dispatcher che chiama il corretto metodo per approvare la richiesta.
   *
   * @param id l'id della richiesta da approvare
   */
  public static void approval(long id) {
    InformationRequest request = informationRequestDao.getById(id);
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    switch (request.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = informationRequestDao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = informationRequestDao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = informationRequestDao.getTeleworkById(id);
        break;
      default:
        break;
    }
    notFoundIfNull(request);
    User user = Security.getUser().get();

    boolean approved = informationRequestManager.approval(serviceRequest, 
        illnessRequest, teleworkRequest, user);

    if (approved) {
      notificationManager.sendEmailToUser(Optional.absent(), Optional.absent(), 
          Optional.fromNullable(request), true);
      log.debug("Inviata mail con approvazione");
      flash.success("Operazione conclusa correttamente");
    } else {
      flash.error("Problemi nel completare l'operazione contattare il supporto tecnico di ePAS.");
    }
    if (user.person.isSeatSupervisor()) {
      InformationRequests.listToApprove(request.informationType);
    } else {
      InformationRequests.list(request.informationType);
    }    

  }
  
  /**
   * Dispatcher che instrada al corretto metodo l'operazione da fare sulla richiesta a seconda dei
   * parametri.
   *
   * @param id l'id della richiesta di assenza
   */
  public static void disapproval(long id, boolean disapproval, String reason) {
    InformationRequest informationRequest = informationRequestDao.getById(id);
    ServiceRequest serviceRequest = null;
    IllnessRequest illnessRequest = null;
    TeleworkRequest teleworkRequest = null;
    notFoundIfNull(informationRequest);
    User user = Security.getUser().get();
    switch (informationRequest.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = informationRequestDao.getServiceById(id).get();
        if (!disapproval) {
          disapproval = true;
          render(serviceRequest, informationRequest, disapproval);
        }
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = informationRequestDao.getIllnessById(id).get();
        if (!disapproval) {
          disapproval = true;
          render(illnessRequest, informationRequest, disapproval);
        }
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = informationRequestDao.getTeleworkById(id).get();
        if (!disapproval) {
          disapproval = true;
          render(teleworkRequest, informationRequest, disapproval);
        }
        break;
      default:
        break;
    }      
    
    if (informationRequest.officeHeadApprovalRequired 
        && informationRequest.officeHeadApproved == null
        && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      // caso di approvazione da parte del responsabile di sede
      informationRequestManager.officeHeadDisapproval(id, reason);
      flash.error("Richiesta respinta");
      InformationType type = informationRequest.informationType;
      render("@show", informationRequest, type, serviceRequest, 
          illnessRequest, teleworkRequest, user);
    }
    render("@show", informationRequest, user);
  }
  
  /**
   * Mostra al template la richiesta.
   *
   * @param id l'id della richiesta da visualizzare
   * @param type la tipologia di richiesta
   */
  public static void show(long id, InformationType type) {
    InformationRequest informationRequest = informationRequestDao.getById(id);
    notFoundIfNull(informationRequest);
    rules.checkIfPermitted(informationRequest);
    User user = Security.getUser().get();
    ServiceRequest serviceRequest = null;
    IllnessRequest illnessRequest = null;
    TeleworkRequest teleworkRequest = null;
    switch (type) {
      case SERVICE_INFORMATION:
        serviceRequest = informationRequestDao.getServiceById(id).get();        
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = informationRequestDao.getIllnessById(id).get();        
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = informationRequestDao.getTeleworkById(id).get();   
        break;
      default:
        log.info("Passato argomento non conosciuto");
        break;
    }
    boolean disapproval = false;
    render(informationRequest, teleworkRequest, illnessRequest, serviceRequest, 
        type, user, disapproval);
  }
  
  /**
   * Form di cancellazione di un flusso informativo.
   *
   * @param id del flusso da cancellare.
   */
  public static void delete(long id) {
    InformationRequest informationRequest = informationRequestDao.getById(id);
    notFoundIfNull(informationRequest);
    rules.checkIfPermitted(informationRequest);
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    switch (informationRequest.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = Optional.fromNullable(informationRequestDao.getServiceById(id).get());
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = Optional.fromNullable(informationRequestDao.getIllnessById(id).get());
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = Optional.fromNullable(informationRequestDao.getTeleworkById(id).get());
        break;
      default: 
        break;
    }
    informationRequestManager.executeEvent(serviceRequest, illnessRequest, 
        teleworkRequest, Security.getUser().get().person,
        InformationRequestEventType.DELETE, Optional.absent());
    flash.success(Web.msgDeleted(InformationRequest.class));
    list(informationRequest.informationType);
  }
  
  /**
   * Ritorna il riepilogo del telelavoro dell'anno/mese della persona in oggetto.
   * @param personId l'identificativo della persona
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @throws NoSuchFieldException eccezione di mancanza di campo
   * @throws ExecutionException eccezione in esecuzione
   */
  public static void generateTeleworkReport(Long personId, int year, int month) 
      throws NoSuchFieldException, ExecutionException {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    IWrapperPerson wrperson = wrapperFactory.create(person);
   
    List<NewTeleworkDto> list = Lists.newArrayList();
    
    if (!wrperson.isActiveInMonth(new YearMonth(year, month))) {
      flash.error("Non esiste situazione mensile per il mese di %s %s",
          DateUtility.fromIntToStringMonth(month), year);

      YearMonth last = wrperson.getLastActiveMonth();
      Stampings.stampings(last.getYear(), last.getMonthOfYear());
    }
    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrperson.getValue(), year, month, true);
    
    log.debug("Chiedo la lista delle timbrature in telelavoro ad applicazione esterna.");
    
    list = manager.stampingsForReport(psDto);
    render(list, person);
  }
  
  /**
   * Ritorna la form di gestione approvazioni di telelavoro.
   * 
   * @param personId l'identificativo della persona di cui gestire le richieste/approvazioni
   *     di telelavoro
   */
  public static void handleTeleworkApproval(Long personId) {
    PersonLite p = null;
    Person person = personDao.getPersonById(personId);
    if (person.personConfigurations.stream().noneMatch(pc -> 
        pc.epasParam.equals(EpasParam.TELEWORK_STAMPINGS) && pc.fieldValue.equals("true"))) {
      List<PersonDao.PersonLite> persons = (List<PersonLite>) renderArgs.get("navPersons");
      if (persons.isEmpty()) {
        flash.error("Non ci sono persone abilitate al telelavoro!!");
        Stampings.personStamping(personId, Integer.parseInt(session.get("yearSelected")), 
            Integer.parseInt(session.get("monthSelected")));
      }
      p = persons.get(0);
      
    }
    if (p != null) {
      person = personDao.getPersonById(p.id); 
    }
    
    Preconditions.checkNotNull(person);
    rules.checkIfPermitted(person.getCurrentOffice().get());
    List<TeleworkApprovalDto> dtoList = Lists.newArrayList();
    List<TeleworkRequest> teleworkRequests = informationRequestDao.personTeleworkList(person);
    TeleworkApprovalDto dto = null;
    for (TeleworkRequest request : teleworkRequests) {
      Optional<TeleworkValidation> validation = validationDao
          .byPersonYearAndMonth(person, request.year, request.month);
      if (validation.isPresent()) {
        dto = TeleworkApprovalDto.builder()
            .teleworkValidation(validation.get())
            .teleworkRequest(request).build();
      } else {
        dto = TeleworkApprovalDto.builder()
            .teleworkValidation(null)
            .teleworkRequest(request).build();
      }
      dtoList.add(dto);
    }
    render(dtoList);
  }
  
  /**
   * Revoca la validazione ad un telelavoro.
   * 
   * @param validationId l'identificativo della validazione
   */
  public static void revokeValidation(Long validationId) {
    Optional<TeleworkValidation> validation = validationDao.getValidationById(validationId);
    if (validation.isPresent()) {
      validation.get().delete();
      flash.success("Validazione rimossa. Effettuare nuova richiesta di approvazione");      
    } else {
      flash.error("Validazione sconosciuta! Verificare l'identificativo.");
    }
    Stampings.personStamping(Security.getUser().get().person.id, 
        Integer.parseInt(session.get("yearSelected")), 
        Integer.parseInt(session.get("monthSelected")));    
  }
}
