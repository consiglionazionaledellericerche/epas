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

package manager.flows;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import controllers.Security;
import dao.InformationRequestDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import java.time.LocalTime;
import java.util.List;
import javax.inject.Inject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.NotificationManager;
import manager.configurations.ConfigurationManager;
import models.Person;
import models.Role;
import models.TeleworkValidation;
import models.User;
import models.base.InformationRequest;
import models.enumerate.InformationType;
import models.flows.enumerate.InformationRequestEventType;
import models.informationrequests.IllnessRequest;
import models.informationrequests.InformationRequestEvent;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
import org.apache.commons.compress.utils.Lists;
import org.joda.time.LocalDate;


/**
 * Classe di costruzione information request.
 * @author dario
 * 
 */
@Slf4j
public class InformationRequestManager {

  private ConfigurationManager configurationManager;
  private UsersRolesOfficesDao uroDao;
  private RoleDao roleDao;
  private InformationRequestDao dao;
  private NotificationManager notificationManager;
  
  /**
   * DTO per la configurazione delle InformationRequest.
   */
  @Data
  @RequiredArgsConstructor
  @ToString
  public class InformationRequestConfiguration {
    final Person person;
    final InformationType type;
    boolean officeHeadApprovalRequired;
    boolean administrativeApprovalRequired;
  }
  
  /**
   * Costruttore injector. 
   * @param configurationManager il configuration manager
   * @param uroDao il dao per gli usersRolesOffices
   * @param roleDao il dao per i ruoli
   * @param dao il dao per le informationRequest
   * @param notificationManager il manager delle notifiche
   */
  @Inject
  public InformationRequestManager(ConfigurationManager configurationManager, 
      UsersRolesOfficesDao uroDao, RoleDao roleDao, InformationRequestDao dao,
      NotificationManager notificationManager) {
    this.configurationManager = configurationManager;
    this.uroDao = uroDao;
    this.roleDao = roleDao;
    this.dao = dao;
    this.notificationManager = notificationManager;
  }
  
  /**
   * Verifica quali sono le approvazioni richiesta per questo tipo di assenza per questa persona.
   *
   * @param requestType il tipo di richiesta di assenza
   * @param person la persona.
   *
   * @return la configurazione con i tipi di approvazione necessari.
   */
  public InformationRequestConfiguration getConfiguration(InformationType requestType,
      Person person) {
    val informationRequestConfiguration = new InformationRequestConfiguration(person, requestType);

    if (requestType.alwaysSkipOfficeHeadApproval) {
      informationRequestConfiguration.officeHeadApprovalRequired = false;
    } else {
      if (person.isTopQualification()
          && requestType.officeHeadApprovalRequiredTopLevel.isPresent()) {
        informationRequestConfiguration.officeHeadApprovalRequired =
            (Boolean) configurationManager.configValue(person.getCurrentOffice().get(),
                requestType.officeHeadApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification()
          && requestType.officeHeadApprovalRequiredTechnicianLevel.isPresent()) {
        informationRequestConfiguration.officeHeadApprovalRequired =
            (Boolean) configurationManager.configValue(person.getCurrentOffice().get(),
                requestType.officeHeadApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }
    }
    if (requestType.alwaysSkipAdministrativeApproval) {
      informationRequestConfiguration.administrativeApprovalRequired = false;
    } else {
      if (person.isTopQualification()
          && requestType.administrativeApprovalRequiredTopLevel.isPresent()) {
        informationRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(person.getCurrentOffice().get(), 
                requestType.administrativeApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification()
          && requestType.administrativeApprovalRequiredTechnicianLevel.isPresent()) {
        informationRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(person.getCurrentOffice().get(), 
                requestType.administrativeApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }
    }
    
    return informationRequestConfiguration;
  }
  
  /**
   * Imposta nella richiesta di assenza i tipi di approvazione necessari in funzione del tipo di
   * assenza e della configurazione specifica della sede del dipendente.   * 
   * @param illnessRequest l'opzionale richiesta di malattia
   * @param serviceRequest l'opzionale richiesta di uscita di servizio
   * @param teleworkRequest l'opzionale richiesta di telelavoro
   */
  public void configure(Optional<IllnessRequest> illnessRequest, 
      Optional<ServiceRequest> serviceRequest, Optional<TeleworkRequest> teleworkRequest) {
    val request = serviceRequest.isPresent() ? serviceRequest.get() : 
        (teleworkRequest.isPresent() ? teleworkRequest.get() : illnessRequest.get());
    Verify.verifyNotNull(request.informationType);
    Verify.verifyNotNull(request.person);

    val config = getConfiguration(request.informationType, request.person);

    request.officeHeadApprovalRequired = config.officeHeadApprovalRequired;
    if (illnessRequest.isPresent()) {
      request.administrativeApprovalRequired = config.administrativeApprovalRequired;
    }    
  }
  
  /**
   * Verifica che gruppi ed eventuali responsabile di sede siano presenti per poter richiedere il
   * tipo di assenza.
   * @param requestType il tipo di assenza da controllare
   * @param person la persona per cui controllare il tipo di assenza
   * @return la lista degli eventuali problemi riscontrati.
   */
  public List<String> checkconfiguration(InformationType requestType, Person person) {
    Verify.verifyNotNull(requestType);
    Verify.verifyNotNull(person);

    val problems = Lists.<String>newArrayList();
    val config = getConfiguration(requestType, person);

    if (person.user.hasRoles(Role.GROUP_MANAGER, Role.SEAT_SUPERVISOR)) {
      return Lists.newArrayList();
    }
    
    if (config.isOfficeHeadApprovalRequired() && uroDao
        .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.SEAT_SUPERVISOR), 
            person.getCurrentOffice().get()).isEmpty()) {
      problems.add(String.format("Approvazione del responsabile di sede richiesta. "
          + "L'ufficio %s non ha impostato nessun responsabile di sede. "
          + "Contattare l'ufficio del personale.", person.getCurrentOffice().get().getName()));
    }
    if (config.isAdministrativeApprovalRequired() && uroDao
        .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.PERSONNEL_ADMIN), 
            person.getCurrentOffice().get()).isEmpty()) {
      problems.add(String.format("Approvazione dell'amministratore del personale richiesta. "
          + "L'ufficio %s non ha impostato nessun amministratore del personale. "
          + "Contattare l'ufficio del personale.", person.getCurrentOffice().get().getName()));
    }
    return problems;
  }
  
  /**
   * Metodo di utilità per parsare una stringa e renderla un LocalTime.   * 
   * @param time la stringa contenente l'ora
   * @return il LocalTime corrispondente alla stringa passata come parametro.
   */
  public LocalTime deparseTime(String time) {
    if (time == null || time.isEmpty()) {
      return null;
    }
    time = time.replaceAll(":", "");
    Integer hour = Integer.parseInt(time.substring(0, 2));
    Integer minute = Integer.parseInt(time.substring(2, 4));
    return LocalTime.of(hour, minute);
  }

  /**
   * Metodo che esegue gli eventi del flusso.
   * @param serviceRequest la richiesta di uscita di servizio (opzionale)
   * @param illnessRequest la richiesta di informazione di malattia (opzionale)
   * @param teleworkRequest la richiesta di telelavoro (opzionale)
   * @param person la persona che esegue la richiesta
   * @param eventType il tipo di evento da eseguire
   * @param reason la motivazione
   * @return
   */
  public Optional<String> executeEvent(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest, 
      Person person, InformationRequestEventType eventType, Optional<String> reason) {

    val request = serviceRequest.isPresent() ? serviceRequest.get() : 
        (teleworkRequest.isPresent() ? teleworkRequest.get() : illnessRequest.get());

    val problem = checkInformationRequestEvent(serviceRequest, illnessRequest, teleworkRequest, 
        person, eventType);
    if (problem.isPresent()) {
      log.warn("Impossibile inserire la richiesta di informazione {}. Problema: {}", request,
          problem.get());
      return problem;
    }

    switch (eventType) {
      case STARTING_APPROVAL_FLOW:
        request.flowStarted = true;
        break;

      case OFFICE_HEAD_ACKNOWLEDGMENT:
        request.officeHeadApproved = java.time.LocalDateTime.now();
        request.endTo = java.time.LocalDateTime.now();
        request.flowEnded = true;
        if (request.informationType.equals(InformationType.TELEWORK_INFORMATION)) {
          TeleworkValidation validation = new TeleworkValidation();
          validation.person = teleworkRequest.get().person;
          validation.year = teleworkRequest.get().year;
          validation.month = teleworkRequest.get().month;
          validation.approved = true;
          validation.approvationDate = java.time.LocalDate.now();
          validation.save();
        }
        break;

      case OFFICE_HEAD_REFUSAL:
        // si riparte dall'inizio del flusso.
        resetFlow(serviceRequest, illnessRequest, teleworkRequest);
        request.flowEnded = true;
        notificationManager.notificationInformationRequestRefused(serviceRequest, 
            illnessRequest, teleworkRequest, person);
        break;
        
      case ADMINISTRATIVE_ACKNOWLEDGMENT:
        //TODO: completare con controllo su IllnessRequest
        request.administrativeApproved = java.time.LocalDateTime.now();
        request.endTo = java.time.LocalDateTime.now();
        request.flowEnded = true;
        break;
        
      case ADMINISTRATIVE_REFUSAL:
        //TODO: completare
        break;
        
      case COMPLETE:
        request.officeHeadApproved = java.time.LocalDateTime.now();
        request.endTo = java.time.LocalDateTime.now();
        request.flowEnded = true;
        break;
        
      case DELETE:
        // Impostato flowEnded a true per evitare di completare il flusso inserendo l'assenza
        request.flowEnded = true;
        break;
      case EPAS_REFUSAL:
        resetFlow(serviceRequest, illnessRequest, teleworkRequest);
        //TODO: aggiungere notifica
        //notificationManager.notificationAbsenceRequestRefused(request, person);
        break;

      default:
        throw new IllegalStateException(
            String.format("Evento di richiesta assenza %s non previsto", eventType));
    }

    val event = InformationRequestEvent.builder().informationRequest(request).owner(person.user)
        .eventType(eventType).build();
    event.save();

    log.info("Costruito evento per richiesta di assenza {}", event);
    request.save();
    checkAndCompleteFlow(serviceRequest, illnessRequest, teleworkRequest);
    return Optional.absent();    
  }
  

  /**
   * Verifica se il tipo di evento è eseguibile dall'utente indicato.
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di malattia
   * @param teleworkRequest l'eventuale richiesta di telelavoro
   * @param approver la persona che effettua l'approvazione.
   * @param eventType il tipo di evento.
   * @return l'eventuale problema riscontrati durante l'approvazione.
   */
  public Optional<String> checkInformationRequestEvent(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest, 
      Person approver, InformationRequestEventType eventType) {
    
    val request = serviceRequest.isPresent() ? serviceRequest.get() : 
        (teleworkRequest.isPresent() ? teleworkRequest.get() : illnessRequest.get());
    
    if (eventType == InformationRequestEventType.STARTING_APPROVAL_FLOW) {
      if (!request.person.equals(approver)) {
        return Optional.of("Il flusso può essere avviato solamente dal diretto interessato.");
      }
      if (request.flowStarted) {
        return Optional.of("Flusso già avviato, impossibile avviarlo di nuovo.");
      }
    }

    if (eventType == InformationRequestEventType.OFFICE_HEAD_ACKNOWLEDGMENT
        || eventType == InformationRequestEventType.OFFICE_HEAD_REFUSAL) {
      if (request.isOfficeHeadApproved()) {
        return Optional.of("Questa richiesta di assenza è già stata approvata "
            + "da parte del responsabile di sede.");
      }
      if (!uroDao.getUsersRolesOffices(approver.user, roleDao.getRoleByName(Role.SEAT_SUPERVISOR),
          request.person.getCurrentOffice().get()).isPresent()) {
        return Optional.of(String.format("L'evento %s non può essere eseguito da %s perché non ha"
            + " il ruolo di responsabile di sede.", eventType, approver.getFullname()));
      }
    }
    
    if (eventType == InformationRequestEventType.ADMINISTRATIVE_ACKNOWLEDGMENT
        || eventType == InformationRequestEventType.ADMINISTRATIVE_REFUSAL) {
      if (!request.administrativeApprovalRequired) {
        return Optional.of("Questa richiesta di assenza non prevede approvazione/rifiuto "
            + "da parte dell'amministrazione del personale.");
      }
      if (request.isAdministrativeApproved()) {
        return Optional.of("Questa richiesta di assenza è già stata approvata "
            + "da parte dell'amministrazione del personale.");
      }
      if (!uroDao
          .getUsersRolesOffices(approver.user,
              roleDao.getRoleByName(Role.PERSONNEL_ADMIN), request.person.getCurrentOffice().get())
          .isPresent()) {
        return Optional.of(String.format("L'evento %s non può essere eseguito da %s perché non ha"
            + " il ruolo di amministratore del personale.", eventType, approver.getFullname()));
      }
    }

    return Optional.absent();
  }

  /**
   * Rimuove tutte le eventuali approvazioni ed impostata il flusso come da avviare. 
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di malattia
   * @param teleworkRequest l'eventuale richiesta di telelavoro
   */
  public void resetFlow(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest) {
    if (serviceRequest.isPresent()) {
      serviceRequest.get().flowStarted = false;
      serviceRequest.get().officeHeadApproved = null;
    }
    if (illnessRequest.isPresent()) {
      illnessRequest.get().flowStarted = false;
      illnessRequest.get().officeHeadApproved = null;
      illnessRequest.get().administrativeApproved = null;
    }
    if (teleworkRequest.isPresent()) {
      teleworkRequest.get().flowStarted = false;
      teleworkRequest.get().officeHeadApproved = null;
    }
  }
  
  /**
   * Controlla se una richiesta informativa può essere terminata con successo.
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di malattia
   * @param teleworkRequest l'eventuale richiesta di telelavoro
   */
  public void checkAndCompleteFlow(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest) {
    if (serviceRequest.isPresent()) {
      if (serviceRequest.get().isFullyApproved() && !serviceRequest.get().flowEnded) {
        completeFlow(serviceRequest, Optional.absent(), Optional.absent());      
      } 
    }
    if (illnessRequest.isPresent()) {
      if (illnessRequest.get().isFullyApproved() && !illnessRequest.get().flowEnded) {
        completeFlow(Optional.absent(), illnessRequest, Optional.absent());      
      } 
    }
    if (teleworkRequest.isPresent()) {
      if (teleworkRequest.get().isFullyApproved() && !teleworkRequest.get().flowEnded) {
        completeFlow(Optional.absent(), Optional.absent(), teleworkRequest);      
      } 
    }       
  }
 
  /**
   * Certifica il completamento del flusso.
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di informazione malattia
   * @param teleworkRequest l'eventuale richiesta di approvazione telelavoro
   */
  private void completeFlow(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest) {
    if (serviceRequest.isPresent()) {
      serviceRequest.get().flowEnded = true;
      serviceRequest.get().save();    
    }
    if (illnessRequest.isPresent()) {
      illnessRequest.get().flowEnded = true;
      illnessRequest.get().save(); 
    }
    if (teleworkRequest.isPresent()) {
      teleworkRequest.get().flowEnded = true;
      teleworkRequest.get().save(); 
    }  
  }
  
  /**
   * Segue l'approvazione del flusso controllando i vari casi possibili. 
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di informazione malattia
   * @param teleworkRequest l'eventuale richiesta di approvazione telelavoro
   * @param user l'utente che sta approvando il flusso
   * @return true se il flusso è stato approvato correttamente, false altrimenti.
   */
  public boolean approval(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest, 
      User user) {
    if (serviceRequest.isPresent()) {
      if (!serviceRequest.get().isFullyApproved() && user.hasRoles(Role.SEAT_SUPERVISOR)) {
        // caso di approvazione da parte del responsabile di sede
        officeHeadApproval(serviceRequest.get().id, user);
        return true;
      }
      
    }
    if (illnessRequest.isPresent()) {
      if (illnessRequest.get().officeHeadApprovalRequired 
          && illnessRequest.get().officeHeadApproved == null
          && user.hasRoles(Role.SEAT_SUPERVISOR)) {
        // caso di approvazione da parte del responsabile di sede
        officeHeadApproval(illnessRequest.get().id, user);
        return true;
      }
      if (illnessRequest.get().administrativeApprovalRequired
          && illnessRequest.get().administrativeApproved == null 
          && user.hasRoles(Role.PERSONNEL_ADMIN)) {
        // caso di approvazione da parte dell'amministratore del personale
        personnelAdministratorApproval(illnessRequest.get().id, user);
        return true;
      }
    }
    if (teleworkRequest.isPresent()) {
      if (!teleworkRequest.get().isFullyApproved() && user.hasRoles(Role.SEAT_SUPERVISOR)) {
        // caso di approvazione da parte del responsabile di sede
        officeHeadApproval(teleworkRequest.get().id, user);
        return true;
      }
    }  
    
    return false;
  }
  

  /**
   * Approvazione richiesta informativa da parte del responsabile di sede. 
   * @param id id della richiesta di assenza.
   * @param user l'utente che deve approvare
   */
  public void officeHeadApproval(long id, User user) {
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    InformationRequest request = dao.getById(id);
    switch (request.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id);
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().person;
    executeEvent(serviceRequest, illnessRequest, teleworkRequest,
        currentPerson, InformationRequestEventType.OFFICE_HEAD_ACKNOWLEDGMENT,
        Optional.absent());
    log.info("{} approvata dal responsabile di sede {}.", request,
        currentPerson.getFullname());
    
    notificationManager.notificationInformationRequestPolicy(user, request, true);
  }
  
 
  /**
   * Disapprovazione richiesta di flusso informativo da parte del responsabile di sede. 
   * @param id id della richiesta di assenza.
   * @param reason la motivazione del rifiuto
   */
  public void officeHeadDisapproval(long id, String reason) {
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    InformationRequest request = dao.getById(id);
    switch (request.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id);
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().person;
    executeEvent(serviceRequest, illnessRequest, 
        teleworkRequest, currentPerson, 
        InformationRequestEventType.OFFICE_HEAD_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.", request,
        currentPerson.getFullname());
  }
  
  /**
   * Approvazione della richiesta di flusso informativo da parte dell'amministratore del personale.
   * @param id l'id della richiesta di assenza.
   * @param user l'utente che approva.
   */
  public void personnelAdministratorApproval(long id, User user) {
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    InformationRequest request = dao.getById(id);
    switch (request.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id);
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().person;
    executeEvent(serviceRequest, illnessRequest, teleworkRequest,
        currentPerson, InformationRequestEventType.ADMINISTRATIVE_ACKNOWLEDGMENT,
        Optional.absent());
    log.info("{} approvata dall'amministratore del personale {}.", request,
        currentPerson.getFullname());
    
    notificationManager.notificationInformationRequestPolicy(user, request, true);
  }
  
  /**
   * Approvazione della richiesta di flusso informativo da parte dell'amministratore del personale.
   * @param id l'id della richiesta di flusso informativo.
   * @param reason la motivazione della respinta.
   */
  public void personnelAdministratorDisapproval(long id, String reason) {
    ServiceRequest serviceRequest = null;
    IllnessRequest illnessRequest = null;
    TeleworkRequest teleworkRequest = null;
    InformationRequest request = dao.getById(id);
    switch (request.informationType) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id).get();
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id).get();
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id).get();
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().person;
    executeEvent(Optional.of(serviceRequest), Optional.of(illnessRequest), 
        Optional.of(teleworkRequest), currentPerson, 
        InformationRequestEventType.ADMINISTRATIVE_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.", request,
        currentPerson.getFullname());
  }

}
