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
import dao.InformationRequestDao;
import dao.PersonDao;
import helpers.Web;
import helpers.validators.StringIsTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.NotificationManager;
import manager.flows.InformationRequestManager;
import models.Person;
import models.Role;
import models.User;
import models.base.InformationRequest;
import models.enumerate.InformationType;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.InformationRequestEventType;
import models.informationrequests.IllnessRequest;
import models.informationrequests.InformationRequestEvent;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
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
    List<ServiceRequest> services = Lists.newArrayList();
    List<IllnessRequest> illness = Lists.newArrayList();
    switch (type) {
      case TELEWORK_INFORMATION:
        teleworks = informationRequestDao.teleworksByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.TELEWORK_INFORMATION, true);
        break;
      case ILLNESS_INFORMATION:
        illness = informationRequestDao.illnessByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.ILLNESS_INFORMATION, true);
        break;
      case SERVICE_INFORMATION:
        services = informationRequestDao.servicesByPersonAndDate(person, fromDate, 
            Optional.absent(), InformationType.SERVICE_INFORMATION, true);
        break;
      default:
        log.info("Passato argomento non conosciuto");
        break;
    }
    render(teleworks, services, illness, config, type);
  }
  
  public static void listToApprove(InformationType type) {
    
  }
  
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
    switch(type) {
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
    rules.checkIfPermitted(illnessRequest);
    boolean insertable = true;
    render(illnessRequest, retroactiveAbsence, insertable);
  }
  
  public static void saveServiceRequest(ServiceRequest serviceRequest,
      @CheckWith(StringIsTime.class) String begin, @CheckWith(StringIsTime.class) String finish) {
  //rules.checkIfPermitted(serviceRequest);
    boolean insertable = true;
    serviceRequest.beginAt = informationRequestManager.deparseTime(begin);
    serviceRequest.finishTo = informationRequestManager.deparseTime(finish);
    if (serviceRequest.beginAt == null || serviceRequest.finishTo == null) {
      Validation.addError("serviceRequest.beginAt",
          "Entrambi i campi data devono essere valorizzati");
      Validation.addError("serviceRequest.finishTo",
          "Entrambi i campi data devono essere valorizzati");
      response.status = 400;
      insertable = false;
      render("@editServiceRequest", serviceRequest, insertable, begin, finish);
    }
    
    serviceRequest.startAt = LocalDateTime.now();
    serviceRequest.save();
    
    boolean isNewRequest = !serviceRequest.isPersistent();
    if (isNewRequest || !serviceRequest.flowStarted) {
      informationRequestManager.executeEvent(Optional.fromNullable(serviceRequest), 
          Optional.absent(), Optional.absent(),serviceRequest.person,
          InformationRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
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
  
  public static void saveIllnessRequest(IllnessRequest illnessRequest) {
    if (illnessRequest.beginDate == null || illnessRequest.endDate == null) {
      Validation.addError("serviceRequest.beginDate",
          "Entrambi i campi data devono essere valorizzati");
      Validation.addError("serviceRequest.endDate",
          "Entrambi i campi data devono essere valorizzati");
      response.status = 400;
      
      render("@editServiceRequest", illnessRequest);
    }
    illnessRequest.startAt = LocalDateTime.now();
    illnessRequest.save();
    boolean isNewRequest = !illnessRequest.isPersistent();
    if (isNewRequest || !illnessRequest.flowStarted) {
      informationRequestManager.executeEvent(Optional.absent(), 
          Optional.fromNullable(illnessRequest), Optional.absent(),illnessRequest.person,
          InformationRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
      if (illnessRequest.person.isSeatSupervisor()) {
        approval(illnessRequest.id);
      } else {
        // invio la notifica al primo che deve validare la mia richiesta
        //notificationManager.notificationAbsenceRequestPolicy(absenceRequest.person.user,
        //    absenceRequest, true);
        // invio anche la mail
        //notificationManager.sendEmailAbsenceRequestPolicy(absenceRequest.person.user,
        //    absenceRequest, true);
        log.debug("Inviata la richiesta di approvazione");
      }

    }
    flash.success("Operazione effettuata correttamente");

    InformationRequests.list(illnessRequest.informationType);
  }
  
  
  /**
   * Metodo dispatcher che chiama il corretto metodo per approvare la richiesta.
   *
   * @param id l'id della richiesta da approvare
   */
  public static void approval(long id) {
    InformationRequest request = informationRequestDao.getById(id);
    ServiceRequest serviceRequest = null;
    IllnessRequest illnessRequest = null;
    TeleworkRequest teleworkRequest = null;
    switch (request.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = informationRequestDao.getServiceById(id).get();
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = informationRequestDao.getIllnessById(id).get();
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = informationRequestDao.getTeleworkById(id).get();
    }
    notFoundIfNull(request);
    User user = Security.getUser().get();

    boolean approved = informationRequestManager.approval(Optional.of(serviceRequest), 
        Optional.of(illnessRequest), Optional.of(teleworkRequest), user);

    if (approved) {
      notificationManager.sendEmailToUser(Optional.absent(), Optional.absent(), 
          Optional.fromNullable(request));
      log.debug("Inviata mail con approvazione");
      flash.success("Operazione conclusa correttamente");
    } else {
      flash.error("Problemi nel completare l'operazione contattare il supporto tecnico di ePAS.");
    }
    InformationRequests.listToApprove(request.informationType);

  }
  
  /**
   * Dispatcher che instrada al corretto metodo l'operazione da fare sulla richiesta a seconda dei
   * parametri.
   *
   * @param id l'id della richiesta di assenza
   */
  public static void disapproval(long id, boolean disapproval, String reason) {
    InformationRequest request = informationRequestDao.getById(id);
    ServiceRequest serviceRequest = null;
    IllnessRequest illnessRequest = null;
    TeleworkRequest teleworkRequest = null;
    notFoundIfNull(request);
    User user = Security.getUser().get();
    switch (request.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = informationRequestDao.getServiceById(id).get();
        if (!disapproval) {
          disapproval = true;
          render(serviceRequest, disapproval);
        }
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = informationRequestDao.getIllnessById(id).get();
        if (!disapproval) {
          disapproval = true;
          render(illnessRequest, disapproval);
        }
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = informationRequestDao.getTeleworkById(id).get();
        if (!disapproval) {
          disapproval = true;
          render(teleworkRequest, disapproval);
        }
    }      
    
    if (request.officeHeadApprovalRequired && request.officeHeadApproved == null
        && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      // caso di approvazione da parte del responsabile di sede
      informationRequestManager.officeHeadDisapproval(id, reason);
      flash.error("Richiesta respinta");
      render("@show", request, user);
    }
    render("@show", request, user);
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
    ServiceRequest serviceRequest = null;
    IllnessRequest illnessRequest = null;
    TeleworkRequest teleworkRequest = null;
    switch (informationRequest.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = informationRequestDao.getServiceById(id).get();
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = informationRequestDao.getIllnessById(id).get();
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = informationRequestDao.getTeleworkById(id).get();
    }
    informationRequestManager.executeEvent(Optional.of(serviceRequest), Optional.of(illnessRequest), 
        Optional.of(teleworkRequest), Security.getUser().get().person,
        InformationRequestEventType.DELETE, Optional.absent());
    flash.success(Web.msgDeleted(AbsenceRequest.class));
    list(informationRequest.informationType);
  }
}
