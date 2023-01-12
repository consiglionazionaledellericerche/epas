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
import models.informationrequests.ParentalLeaveRequest;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
import org.apache.commons.compress.utils.Lists;
import org.joda.time.LocalDate;


/**
 * Classe di costruzione information request.
 *
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
    boolean managerApprovalRequired;
  }
  
  /**
   * Costruttore injector. 
   *
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
            (Boolean) configurationManager.configValue(person.getOffice(),
                requestType.officeHeadApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification()
          && requestType.officeHeadApprovalRequiredTechnicianLevel.isPresent()) {
        informationRequestConfiguration.officeHeadApprovalRequired =
            (Boolean) configurationManager.configValue(person.getOffice(),
                requestType.officeHeadApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }
    }
    if (requestType.alwaysSkipAdministrativeApproval) {
      informationRequestConfiguration.administrativeApprovalRequired = false;
    } else {
      if (person.isTopQualification()
          && requestType.administrativeApprovalRequiredTopLevel.isPresent()) {
        informationRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(person.getOffice(), 
                requestType.administrativeApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification()
          && requestType.administrativeApprovalRequiredTechnicianLevel.isPresent()) {
        informationRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(person.getOffice(), 
                requestType.administrativeApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }
    }
    if (requestType.alwaysSkipManagerApproval) {
      informationRequestConfiguration.managerApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.managerApprovalRequiredTopLevel.isPresent()) {
        informationRequestConfiguration.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(person.getOffice(), 
                requestType.managerApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification() 
          && requestType.managerApprovalRequiredTechnicianLevel.isPresent()) {
        informationRequestConfiguration.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(person.getOffice(), 
                requestType.managerApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }
    }
    
    return informationRequestConfiguration;
  }
  
  /**
   * Imposta nella richiesta di assenza i tipi di approvazione necessari in funzione del tipo di
   * assenza e della configurazione specifica della sede del dipendente.
   *
   * @param illnessRequest l'opzionale richiesta di malattia
   * @param serviceRequest l'opzionale richiesta di uscita di servizio
   * @param teleworkRequest l'opzionale richiesta di telelavoro
   */
  public void configure(Optional<IllnessRequest> illnessRequest, 
      Optional<ServiceRequest> serviceRequest, Optional<TeleworkRequest> teleworkRequest,
      Optional<ParentalLeaveRequest> parentalLeaveRequest) {
    val request = serviceRequest.isPresent() ? serviceRequest.get() : 
        (teleworkRequest.isPresent() ? teleworkRequest.get() : 
          (illnessRequest.isPresent() ? illnessRequest.get() : parentalLeaveRequest.get()));
    Verify.verifyNotNull(request.getInformationType());
    Verify.verifyNotNull(request.getPerson());

    val config = getConfiguration(request.getInformationType(), request.getPerson());

    request.setOfficeHeadApprovalRequired(config.officeHeadApprovalRequired);
    request.setManagerApprovalRequired(config.managerApprovalRequired);
    if (illnessRequest.isPresent() || parentalLeaveRequest.isPresent()) {
      request.setAdministrativeApprovalRequired(config.administrativeApprovalRequired);
    }    
  }
  
  /**
   * Verifica che gruppi ed eventuali responsabile di sede siano presenti per poter richiedere il
   * tipo di assenza.
   *
   * @param requestType il tipo di assenza da controllare
   * @param person la persona per cui controllare il tipo di assenza
   * @return la lista degli eventuali problemi riscontrati.
   */
  public List<String> checkconfiguration(InformationType requestType, Person person) {
    Verify.verifyNotNull(requestType);
    Verify.verifyNotNull(person);

    val problems = Lists.<String>newArrayList();
    val config = getConfiguration(requestType, person);

    if (person.getUser().hasRoles(Role.GROUP_MANAGER, Role.SEAT_SUPERVISOR)) {
      return Lists.newArrayList();
    }
    
    if (config.isOfficeHeadApprovalRequired() && uroDao
        .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.SEAT_SUPERVISOR), person.getOffice())
        .isEmpty()) {
      problems.add(String.format("Approvazione del responsabile di sede richiesta. "
          + "L'ufficio %s non ha impostato nessun responsabile di sede. "
          + "Contattare l'ufficio del personale.", person.getOffice().getName()));
    }
    if (config.isAdministrativeApprovalRequired() && uroDao
        .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.PERSONNEL_ADMIN), person.getOffice())
        .isEmpty()) {
      problems.add(String.format("Approvazione dell'amministratore del personale richiesta. "
          + "L'ufficio %s non ha impostato nessun amministratore del personale. "
          + "Contattare l'ufficio del personale.", person.getOffice().getName()));
    }
    return problems;
  }
  
  /**
   * Metodo di utilità per parsare una stringa e renderla un LocalTime.
   *
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
   *
   * @param serviceRequest la richiesta di uscita di servizio (opzionale)
   * @param illnessRequest la richiesta di informazione di malattia (opzionale)
   * @param teleworkRequest la richiesta di telelavoro (opzionale)
   * @param parentalLeaveRequest la richiesta di congedo parentale per il padre
   * @param person la persona che esegue la richiesta
   * @param eventType il tipo di evento da eseguire
   * @param reason la motivazione
   * @return il risultato dell'evento da eseguire nel flusso.
   */
  public Optional<String> executeEvent(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest,
      Optional<ParentalLeaveRequest> parentalLeaveRequest,
      Person person, InformationRequestEventType eventType, Optional<String> reason) {

    val request = serviceRequest.isPresent() ? serviceRequest.get() : 
        (teleworkRequest.isPresent() ? teleworkRequest.get() : 
          (illnessRequest.isPresent() ? illnessRequest.get() : parentalLeaveRequest.get()));

    val problem = checkInformationRequestEvent(serviceRequest, illnessRequest, teleworkRequest, 
        parentalLeaveRequest, person, eventType);
    if (problem.isPresent()) {
      log.warn("Impossibile inserire la richiesta di informazione {}. Problema: {}", request,
          problem.get());
      return problem;
    }

    switch (eventType) {
      case STARTING_APPROVAL_FLOW:
        request.setFlowStarted(true);
        break;

      case OFFICE_HEAD_ACKNOWLEDGMENT:
        request.setOfficeHeadApproved(java.time.LocalDateTime.now());
        request.setEndTo(java.time.LocalDateTime.now());
        request.setFlowEnded(true);
        if (request.getInformationType().equals(InformationType.TELEWORK_INFORMATION)) {
          TeleworkValidation validation = new TeleworkValidation();
          validation.setPerson(teleworkRequest.get().getPerson());
          validation.setYear(teleworkRequest.get().getYear());
          validation.setMonth(teleworkRequest.get().getMonth());
          validation.setApproved(true);
          validation.setApprovationDate(java.time.LocalDate.now());
          validation.save();
        }
        break;

      case OFFICE_HEAD_REFUSAL:
        // si riparte dall'inizio del flusso.
        resetFlow(serviceRequest, illnessRequest, teleworkRequest, parentalLeaveRequest);
        request.setFlowEnded(true);
        notificationManager.notificationInformationRequestRefused(serviceRequest, 
            illnessRequest, teleworkRequest, parentalLeaveRequest, person);
        break;
        
      case ADMINISTRATIVE_ACKNOWLEDGMENT:
        //TODO: completare con controllo su IllnessRequest
        request.setAdministrativeApproved(java.time.LocalDateTime.now());
        request.setEndTo(java.time.LocalDateTime.now());
        request.setFlowEnded(true);
        break;
        
      case ADMINISTRATIVE_REFUSAL:
        resetFlow(serviceRequest, illnessRequest, teleworkRequest, parentalLeaveRequest);
        break;
        
      case MANAGER_ACKNOWLEDGMENT:
        //TODO: completare con controllo su IllnessRequest
        request.setManagerApproved(java.time.LocalDateTime.now());
        request.setEndTo(java.time.LocalDateTime.now());
        //request.flowEnded = true;
        break;
        
      case MANAGER_REFUSAL:
        //TODO: completare
        break;
        
      case COMPLETE:
        request.setOfficeHeadApproved(java.time.LocalDateTime.now());
        request.setEndTo(java.time.LocalDateTime.now());
        request.setFlowEnded(true);
        break;
        
      case DELETE:
        // Impostato flowEnded a true per evitare di completare il flusso inserendo l'assenza
        request.setFlowEnded(true);
        break;
      case EPAS_REFUSAL:
        resetFlow(serviceRequest, illnessRequest, teleworkRequest, parentalLeaveRequest);
        //TODO: aggiungere notifica
        //notificationManager.notificationAbsenceRequestRefused(request, person);
        break;

      default:
        throw new IllegalStateException(
            String.format("Evento di richiesta assenza %s non previsto", eventType));
    }

    val event = InformationRequestEvent.builder().informationRequest(request)
        .owner(person.getUser()).eventType(eventType).build();
    event.save();

    log.info("Costruito evento per richiesta di assenza {}", event);
    request.save();
    checkAndCompleteFlow(serviceRequest, illnessRequest, teleworkRequest, parentalLeaveRequest);
    return Optional.absent();    
  }
  

  /**
   * Verifica se il tipo di evento è eseguibile dall'utente indicato.
   *
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di malattia
   * @param teleworkRequest l'eventuale richiesta di telelavoro
   * @param approver la persona che effettua l'approvazione.
   * @param eventType il tipo di evento.
   * @return l'eventuale problema riscontrati durante l'approvazione.
   */
  public Optional<String> checkInformationRequestEvent(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest,
      Optional<ParentalLeaveRequest> parentalLeaveRequest,
      Person approver, InformationRequestEventType eventType) {
    
    val request = serviceRequest.isPresent() ? serviceRequest.get() : 
        (teleworkRequest.isPresent() ? teleworkRequest.get() : 
          (illnessRequest.isPresent() ? illnessRequest.get() : parentalLeaveRequest.get()));
    
    if (eventType == InformationRequestEventType.STARTING_APPROVAL_FLOW) {
      if (!request.getPerson().equals(approver)) {
        return Optional.of("Il flusso può essere avviato solamente dal diretto interessato.");
      }
      if (request.isFlowStarted()) {
        return Optional.of("Flusso già avviato, impossibile avviarlo di nuovo.");
      }
    }

    if (eventType == InformationRequestEventType.OFFICE_HEAD_ACKNOWLEDGMENT
        || eventType == InformationRequestEventType.OFFICE_HEAD_REFUSAL) {
      if (request.isOfficeHeadApproved()) {
        return Optional.of("Questa richiesta di assenza è già stata approvata "
            + "da parte del responsabile di sede.");
      }
      if (!uroDao.getUsersRolesOffices(approver.getUser(), 
          roleDao.getRoleByName(Role.SEAT_SUPERVISOR),
          request.getPerson().getOffice()).isPresent()) {
        return Optional.of(String.format("L'evento %s non può essere eseguito da %s perché non ha"
            + " il ruolo di responsabile di sede.", eventType, approver.getFullname()));
      }
    }
    
    if (eventType == InformationRequestEventType.ADMINISTRATIVE_ACKNOWLEDGMENT
        || eventType == InformationRequestEventType.ADMINISTRATIVE_REFUSAL) {
      if (!request.isAdministrativeApprovalRequired()) {
        return Optional.of("Questa richiesta di assenza non prevede approvazione/rifiuto "
            + "da parte dell'amministrazione del personale.");
      }
      if (request.isAdministrativeApproved()) {
        return Optional.of("Questa richiesta di assenza è già stata approvata "
            + "da parte dell'amministrazione del personale.");
      }
      if (!uroDao
          .getUsersRolesOffices(approver.getUser(),
              roleDao.getRoleByName(Role.PERSONNEL_ADMIN), request.getPerson().getOffice())
          .isPresent()) {
        return Optional.of(String.format("L'evento %s non può essere eseguito da %s perché non ha"
            + " il ruolo di amministratore del personale.", eventType, approver.getFullname()));
      }
    }

    return Optional.absent();
  }

  /**
   * Rimuove tutte le eventuali approvazioni ed impostata il flusso come da avviare.
   *
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di malattia
   * @param teleworkRequest l'eventuale richiesta di telelavoro
   */
  public void resetFlow(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest,
      Optional<ParentalLeaveRequest> parentalLeaveRequest) {
    if (serviceRequest.isPresent()) {
      serviceRequest.get().setFlowStarted(false);
      serviceRequest.get().setOfficeHeadApproved(null);
    }
    if (illnessRequest.isPresent()) {
      illnessRequest.get().setFlowStarted(false);
      illnessRequest.get().setOfficeHeadApproved(null);
      illnessRequest.get().setAdministrativeApproved(null);
    }
    if (teleworkRequest.isPresent()) {
      teleworkRequest.get().setFlowStarted(false);
      teleworkRequest.get().setOfficeHeadApproved(null);
    }
    if (parentalLeaveRequest.isPresent()) {
      parentalLeaveRequest.get().setFlowStarted(false);
      parentalLeaveRequest.get().setAdministrativeApproved(null);
    }
  }
  
  /**
   * Controlla se una richiesta informativa può essere terminata con successo.
   *
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di malattia
   * @param teleworkRequest l'eventuale richiesta di telelavoro
   */
  public void checkAndCompleteFlow(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest,
      Optional<ParentalLeaveRequest> parentalLeaveRequest) {
    if (serviceRequest.isPresent()) {
      if (serviceRequest.get().isFullyApproved() && !serviceRequest.get().isFlowEnded()) {
        completeFlow(serviceRequest, Optional.absent(), Optional.absent(), Optional.absent());      
      } 
    }
    if (illnessRequest.isPresent()) {
      if (illnessRequest.get().isFullyApproved() && !illnessRequest.get().isFlowEnded()) {
        completeFlow(Optional.absent(), illnessRequest, Optional.absent(), Optional.absent());      
      } 
    }
    if (teleworkRequest.isPresent()) {
      if (teleworkRequest.get().isFullyApproved() && !teleworkRequest.get().isFlowEnded()) {
        completeFlow(Optional.absent(), Optional.absent(), 
            teleworkRequest, Optional.absent());      
      } 
    }  
    if (parentalLeaveRequest.isPresent()) {
      if (parentalLeaveRequest.get().isFullyApproved() 
          && !parentalLeaveRequest.get().isFlowEnded()) {
        completeFlow(Optional.absent(), Optional.absent(), Optional.absent(), parentalLeaveRequest);
      }
    }
  }
 
  /**
   * Certifica il completamento del flusso.
   *
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di informazione malattia
   * @param teleworkRequest l'eventuale richiesta di approvazione telelavoro
   */
  private void completeFlow(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest,
      Optional<ParentalLeaveRequest> parentalLeaveRequest) {
    if (serviceRequest.isPresent()) {
      serviceRequest.get().setFlowEnded(true);
      serviceRequest.get().save();    
    }
    if (illnessRequest.isPresent()) {
      illnessRequest.get().setFlowEnded(true);
      illnessRequest.get().save(); 
    }
    if (teleworkRequest.isPresent()) {
      teleworkRequest.get().setFlowEnded(true);
      teleworkRequest.get().save(); 
    }  
    if (parentalLeaveRequest.isPresent()) {
      parentalLeaveRequest.get().setFlowEnded(true);
      parentalLeaveRequest.get().save();
    }
  }
  
  /**
   * Segue l'approvazione del flusso controllando i vari casi possibili. 
   *
   * @param serviceRequest l'eventuale richiesta di uscita di servizio
   * @param illnessRequest l'eventuale richiesta di informazione malattia
   * @param teleworkRequest l'eventuale richiesta di approvazione telelavoro
   * @param user l'utente che sta approvando il flusso
   * @return true se il flusso è stato approvato correttamente, false altrimenti.
   */
  public boolean approval(Optional<ServiceRequest> serviceRequest, 
      Optional<IllnessRequest> illnessRequest, Optional<TeleworkRequest> teleworkRequest,
      Optional<ParentalLeaveRequest> parentalLeaveRequest, User user) {
    if (serviceRequest.isPresent()) {
      if (!serviceRequest.get().isFullyApproved() && user.hasRoles(Role.SEAT_SUPERVISOR)) {
        // caso di approvazione da parte del responsabile di sede
        officeHeadApproval(serviceRequest.get().id, user);
        return true;
      }
      if (!serviceRequest.get().isFullyApproved() && user.hasRoles(Role.GROUP_MANAGER)) {
        managerApproval(serviceRequest.get().id, user);
        return true;
      }
      
    }
    if (illnessRequest.isPresent()) {
      if (illnessRequest.get().isOfficeHeadApprovalRequired() 
          && illnessRequest.get().getOfficeHeadApproved() == null
          && user.hasRoles(Role.SEAT_SUPERVISOR)) {
        // caso di approvazione da parte del responsabile di sede
        officeHeadApproval(illnessRequest.get().id, user);
        return true;
      }
      if (illnessRequest.get().isAdministrativeApprovalRequired()
          && illnessRequest.get().getAdministrativeApproved() == null 
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
    if (parentalLeaveRequest.isPresent()) {
      if (parentalLeaveRequest.get().isAdministrativeApprovalRequired()
          && parentalLeaveRequest.get().getAdministrativeApproved() == null
          && user.hasRoles(Role.PERSONNEL_ADMIN)) {
        personnelAdministratorApproval(parentalLeaveRequest.get().id, user);
        return true;
      }
    }
    
    return false;
  }
  

  /**
   * Approvazione richiesta informativa da parte del responsabile di sede. 
   *
   * @param id id della richiesta di assenza.
   * @param user l'utente che deve approvare
   */
  public void officeHeadApproval(long id, User user) {
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    Optional<ParentalLeaveRequest> parentalLeaveRequest = Optional.absent();
    InformationRequest request = dao.getById(id);
    switch (request.getInformationType()) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id);
        break;
      case PARENTAL_LEAVE_INFORMATION:
        parentalLeaveRequest = dao.getParentalLeaveById(id);
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().getPerson();
    executeEvent(serviceRequest, illnessRequest, teleworkRequest, parentalLeaveRequest,
        currentPerson, InformationRequestEventType.OFFICE_HEAD_ACKNOWLEDGMENT,
        Optional.absent());
    log.info("{} approvata dal responsabile di sede {}.", request,
        currentPerson.getFullname());
    
    notificationManager.notificationInformationRequestPolicy(user, request, true);
  }
  
 
  /**
   * Disapprovazione richiesta di flusso informativo da parte del responsabile di sede.
   *
   * @param id id della richiesta di assenza.
   * @param reason la motivazione del rifiuto
   */
  public void officeHeadDisapproval(long id, String reason) {
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    Optional<ParentalLeaveRequest> parentalLeaveRequest = Optional.absent();
    InformationRequest request = dao.getById(id);
    switch (request.getInformationType()) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id);
        break;
      case PARENTAL_LEAVE_INFORMATION:
        parentalLeaveRequest = dao.getParentalLeaveById(id);
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().getPerson();
    executeEvent(serviceRequest, illnessRequest, 
        teleworkRequest, parentalLeaveRequest, currentPerson, 
        InformationRequestEventType.OFFICE_HEAD_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.", request,
        currentPerson.getFullname());
  }
  
  /**
   * Approvazione della richiesta di flusso informativo da parte dell'amministratore del personale.
   *
   * @param id l'id della richiesta di assenza.
   * @param user l'utente che approva.
   */
  public void personnelAdministratorApproval(long id, User user) {
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    Optional<ParentalLeaveRequest> parentalLeaveRequest = Optional.absent();
    InformationRequest request = dao.getById(id);
    switch (request.getInformationType()) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id);
        break;
      case PARENTAL_LEAVE_INFORMATION:
        parentalLeaveRequest = dao.getParentalLeaveById(id);
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().getPerson();
    executeEvent(serviceRequest, illnessRequest, teleworkRequest, parentalLeaveRequest,
        currentPerson, InformationRequestEventType.ADMINISTRATIVE_ACKNOWLEDGMENT,
        Optional.absent());
    log.info("{} approvata dall'amministratore del personale {}.", request,
        currentPerson.getFullname());
    
    notificationManager.notificationInformationRequestPolicy(user, request, true);
  }
  
  /**
   * Approvazione della richiesta di flusso informativo da parte dell'amministratore del personale.
   *
   * @param id l'id della richiesta di flusso informativo.
   * @param reason la motivazione della respinta.
   */
  public void personnelAdministratorDisapproval(long id, String reason) {
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    Optional<ParentalLeaveRequest> parentalLeaveRequest = Optional.absent();
    InformationRequest request = dao.getById(id);
    switch (request.getInformationType()) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id);
        break;
      case PARENTAL_LEAVE_INFORMATION:
        parentalLeaveRequest = dao.getParentalLeaveById(id);
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().getPerson();
    executeEvent(serviceRequest, illnessRequest, 
        teleworkRequest, parentalLeaveRequest, currentPerson,
        InformationRequestEventType.ADMINISTRATIVE_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.", request,
        currentPerson.getFullname());
  }
  
  /**
   * Approvazione del responsabile di gruppo.
   *
   * @param id l'identificativo della richiesta
   * @param user l'utente che approva
   */
  public void managerApproval(long id, User user) {
    Optional<ServiceRequest> serviceRequest = Optional.absent();
    Optional<IllnessRequest> illnessRequest = Optional.absent();
    Optional<TeleworkRequest> teleworkRequest = Optional.absent();
    Optional<ParentalLeaveRequest> parentalLeaveRequest = Optional.absent();
    InformationRequest request = dao.getById(id);
    switch (request.getInformationType()) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id);
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id);
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id);
        break;
      case PARENTAL_LEAVE_INFORMATION:
        parentalLeaveRequest = dao.getParentalLeaveById(id);
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().getPerson();
    executeEvent(serviceRequest, illnessRequest, teleworkRequest, parentalLeaveRequest,
        currentPerson, InformationRequestEventType.MANAGER_ACKNOWLEDGMENT,
        Optional.absent());
    log.info("{} approvata dal responsabile di gruppo {}.", request,
        currentPerson.getFullname());
    
    notificationManager.notificationInformationRequestPolicy(user, request, true);
  }
  
  /**
   * Respinta del responsabile di gruppo.
   *
   * @param id l'identificativo della richiesta
   * @param reason la motivazione del respingimento
   */
  public void managerDisapproval(long id, String reason) {
    ServiceRequest serviceRequest = null;
    IllnessRequest illnessRequest = null;
    TeleworkRequest teleworkRequest = null;
    ParentalLeaveRequest parentalLeaveRequest = null;
    InformationRequest request = dao.getById(id);
    switch (request.getInformationType()) {
      case SERVICE_INFORMATION:
        serviceRequest = dao.getServiceById(id).get();
        break;
      case ILLNESS_INFORMATION:
        illnessRequest = dao.getIllnessById(id).get();
        break;
      case TELEWORK_INFORMATION:
        teleworkRequest = dao.getTeleworkById(id).get();
        break;
      case PARENTAL_LEAVE_INFORMATION:
        parentalLeaveRequest = dao.getParentalLeaveById(id).get();
        break;
      default:
        break;
    }
    val currentPerson = Security.getUser().get().getPerson();
    executeEvent(Optional.of(serviceRequest), Optional.of(illnessRequest), 
        Optional.of(teleworkRequest), Optional.of(parentalLeaveRequest), currentPerson, 
        InformationRequestEventType.MANAGER_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.", request,
        currentPerson.getFullname());
  }

}
