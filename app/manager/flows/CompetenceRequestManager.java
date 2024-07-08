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
import com.google.common.collect.Sets;
import controllers.Security;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.CompetenceRequestDao;
import dao.GeneralSettingDao;
import dao.GroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.CompetenceManager;
import manager.ConsistencyManager;
import manager.GroupOvertimeManager;
import manager.NotificationManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Competence;
import models.CompetenceCode;
import models.GeneralSetting;
import models.GroupOvertime;
import models.Office;
import models.Person;
import models.PersonReperibilityDay;
import models.Role;
import models.TotalOvertime;
import models.User;
import models.UsersRolesOffices;
import models.dto.PersonOvertimeInMonth;
import models.flows.Affiliation;
import models.flows.CompetenceRequest;
import models.flows.CompetenceRequestEvent;
import models.flows.Group;
import models.flows.enumerate.CompetenceRequestEventType;
import models.flows.enumerate.CompetenceRequestType;
import org.apache.commons.compress.utils.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.libs.F.Promise;

/**
 * Operazioni sulle richieste di compenteze.
 *
 * @author Dario Tagliaferri
 * @author Cristian Lucchesi
 */
@Slf4j
public class CompetenceRequestManager {

  private ConfigurationManager configurationManager;
  private UsersRolesOfficesDao uroDao;
  private RoleDao roleDao;
  private NotificationManager notificationManager;
  private CompetenceRequestDao competenceRequestDao;
  private GroupDao groupDao;
  private PersonDao personDao;
  private PersonReperibilityDayDao repDao;
  private CompetenceCodeDao competenceCodeDao;
  private ConsistencyManager consistencyManager;
  private GroupOvertimeManager groupOvertimeManager;
  private CompetenceDao competenceDao;
  private CompetenceManager competenceManager;
  private GeneralSettingDao settingDao;


  /**
   * DTO per la configurazione delle CompenteRequest.
   */
  @Data
  @RequiredArgsConstructor
  @ToString
  public class CompetenceRequestConfiguration {

    final Person person;
    final CompetenceRequestType type;
    boolean employeeApprovalRequired;
    boolean managerApprovalRequired;
    boolean officeHeadApprovalRequired;
    boolean advanceApprovalRequired;
  }

  /**
   * Injector.
   *
   * @param configurationManager configurationManager per la sede
   * @param uroDao               dao per gli usersRolesOffices
   * @param roleDao              dao per i ruoli
   * @param notificationManager  manager per le notifiche
   * @param competenceRequestDao dao per le richieste di competenza
   * @param groupDao             dao per i gruppi
   * @param personDao            dao per la persona
   * @param repDao               dao per la reperibiltà
   */
  @Inject
  public CompetenceRequestManager(ConfigurationManager configurationManager,
      UsersRolesOfficesDao uroDao, RoleDao roleDao, NotificationManager notificationManager,
      CompetenceRequestDao competenceRequestDao, GroupDao groupDao, PersonDao personDao,
      PersonReperibilityDayDao repDao, CompetenceCodeDao competenceCodeDao, 
      ConsistencyManager consistencyManager, GroupOvertimeManager groupOvertimeManager,
      CompetenceDao competenceDao, CompetenceManager competenceManager, GeneralSettingDao settingDao) {
    this.configurationManager = configurationManager;
    this.uroDao = uroDao;
    this.roleDao = roleDao;
    this.notificationManager = notificationManager;
    this.competenceRequestDao = competenceRequestDao;
    this.groupDao = groupDao;
    this.personDao = personDao;
    this.repDao = repDao;
    this.competenceCodeDao = competenceCodeDao;
    this.consistencyManager = consistencyManager;
    this.groupOvertimeManager = groupOvertimeManager;
    this.competenceDao = competenceDao;
    this.competenceManager = competenceManager;
    this.settingDao = settingDao;
  }

  private static String code = "S1";

  /**
   * Verifica che gruppi ed eventuali responsabile di sede siano presenti per poter richiedere il
   * tipo di competenza.
   *
   * @param requestType il tipo di competenza da controllare
   * @param person      la persona per cui controllare il tipo di assenza
   * @return la lista degli eventuali problemi riscontrati.
   */
  public List<String> checkconfiguration(CompetenceRequestType requestType, Person person) {
    Verify.verifyNotNull(requestType);
    Verify.verifyNotNull(person);

    val problems = Lists.<String>newArrayList();
    val config = getConfiguration(requestType, person);

    if (person.getUser().hasRoles(Role.GROUP_MANAGER, Role.SEAT_SUPERVISOR)) {
      return Lists.newArrayList();
    }

    if (config.isManagerApprovalRequired()
        && groupDao.myGroups(person).isEmpty()) {
      problems.add(
          String.format("Approvazione del responsabile di gruppo richiesta. "
              + "La persona %s non ha impostato nessun responsabile di gruppo "
              + "e non appartiene ad alcun gruppo. "
              + "Contattare l'ufficio del personale.",
              person.getFullname()));
    }

    if (config.isEmployeeApprovalRequired()
        && personDao.byOffice(person.getOffice()).isEmpty()) {
      problems.add(String.format("Approvazione di un dipendente richiesta."
          + "L'ufficio %s non ha altri dipendenti."
          + "Contattare l'ufficio del personale.", person.getOffice().getName()));
    }
    return problems;
  }

  /**
   * Verifica quali sono le approvazioni richiesta per questo tipo di competenza per questa
   * persona.
   *
   * @param requestType il tipo di richiesta di competenza
   * @param person      la persona.
   * @return la configurazione con i tipi di approvazione necessari.
   */
  public CompetenceRequestConfiguration getConfiguration(
      CompetenceRequestType requestType, Person person) {
    val competenceRequestConfiguration = new CompetenceRequestConfiguration(person, requestType);

    if (requestType.alwaysSkipOfficeHeadApproval) {
      competenceRequestConfiguration.officeHeadApprovalRequired = false;
    } else {
      if (requestType.getOfficeHeadApprovalRequiredTechnicianLevel().isPresent()) {
        competenceRequestConfiguration.officeHeadApprovalRequired =
            (Boolean) configurationManager.configValue(
                person.getOffice(), requestType.officeHeadApprovalRequiredTechnicianLevel.get(),
                LocalDate.now());
      }
    }

    if (requestType.alwaysSkipManagerApproval) {
      competenceRequestConfiguration.managerApprovalRequired = false;
    } else {
      if (requestType.getManagerApprovalRequiredTechnicianLevel().isPresent()) {
        competenceRequestConfiguration.managerApprovalRequired =
            (Boolean) configurationManager.configValue(
                person.getOffice(), requestType.getManagerApprovalRequiredTechnicianLevel().get(),
                LocalDate.now());
      }
    }

    if (requestType.alwaysSkipEmployeeApproval) {
      competenceRequestConfiguration.employeeApprovalRequired = false;
    } else {
      if (requestType.employeeApprovalRequired.isPresent()) {
        competenceRequestConfiguration.employeeApprovalRequired =
            (Boolean) configurationManager.configValue(
                person.getOffice(), requestType.employeeApprovalRequired.get(),
                LocalDate.now());
      }
    }

    if (requestType.equals(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST)) {
      competenceRequestConfiguration.advanceApprovalRequired = false;
    } else {
      if (requestType.advanceApprovalRequired.isPresent()) {
        competenceRequestConfiguration.advanceApprovalRequired = 
            (Boolean) configurationManager.configValue(person.getOffice(), 
                requestType.advanceApprovalRequired.get(), LocalDate.now());
      }      
    }

    return competenceRequestConfiguration;
  }

  /**
   * Imposta nella richiesta di competenza i tipi di approvazione necessari in funzione del tipo di
   * competenza e della configurazione specifica della sede del dipendente.
   *
   * @param competenceRequest la richiesta di assenza.
   */
  public void configure(CompetenceRequest competenceRequest) {
    Verify.verifyNotNull(competenceRequest.getType());
    Verify.verifyNotNull(competenceRequest.getPerson());

    val config = getConfiguration(competenceRequest.getType(), competenceRequest.getPerson());

    competenceRequest.setManagerApprovalRequired(config.managerApprovalRequired);

    competenceRequest.setEmployeeApprovalRequired(config.employeeApprovalRequired);
    competenceRequest.setOfficeHeadApprovalRequired(config.officeHeadApprovalRequired);
    competenceRequest.setFirstApprovalRequired(config.advanceApprovalRequired);
  }

  /**
   * Rimuove tutte le eventuali approvazioni ed impostata il flusso come da avviare.
   *
   * @param competenceRequest la richiesta di competenza
   */
  public void resetFlow(CompetenceRequest competenceRequest) {
    competenceRequest.setFlowStarted(false);
    competenceRequest.setManagerApproved(null);
    competenceRequest.setEmployeeApproved(null);
  }

  /**
   * Metodo che verifica se la richiesta può essere approvata o se non è necessario.
   *
   * @param competenceRequest la richiesta di competenza
   * @param approver          chi deve approvarla
   * @param eventType         che tipo di evento stiamo considerando
   * @return una stringa opzionale che contiene lo stato della richiesta.
   */
  public Optional<String> checkCompetenceRequestEvent(CompetenceRequest competenceRequest,
      Person approver, CompetenceRequestEventType eventType) {
    if (eventType == CompetenceRequestEventType.STARTING_APPROVAL_FLOW) {
      if (!competenceRequest.getPerson().equals(approver)) {
        return Optional.of("Il flusso può essere avviato solamente dal diretto interessato.");
      }
      if (competenceRequest.isFlowStarted()) {
        return Optional.of("Flusso già avviato, impossibile avviarlo di nuovo.");
      }
    }

    if (eventType == CompetenceRequestEventType.MANAGER_APPROVAL
        || eventType == CompetenceRequestEventType.MANAGER_REFUSAL) {
      if (!competenceRequest.isManagerApprovalRequired()) {
        return Optional.of("Questa richiesta non prevede approvazione/rifiuto "
            + "da parte del responsabile di gruppo.");
      }
      if (competenceRequest.isManagerApproved()) {
        return Optional.of("Questa richiesta è già stata approvata "
            + "da parte del responsabile di gruppo.");
      }
    }

    if (eventType == CompetenceRequestEventType.EMPLOYEE_APPROVAL
        || eventType == CompetenceRequestEventType.EMPLOYEE_REFUSAL) {
      if (!competenceRequest.isEmployeeApprovalRequired()) {
        return Optional.of("Questa richiesta di competenza non prevede approvazione/rifiuto "
            + "da parte di un dipendente");
      }
      if (competenceRequest.isEmployeeApproved()) {
        return Optional.of("Questa richiesta di competenza è già stata approvata "
            + "da parte di un dipendente");
      }
      if (!uroDao.getUsersRolesOffices(approver.getUser(), roleDao.getRoleByName(Role.EMPLOYEE),
          competenceRequest.getPerson().getOffice()).isPresent()) {
        return Optional.of(String.format("L'evento %s non può essere eseguito da %s perchè non ha"
            + " il ruolo di dipendente.", eventType, approver.getFullname()));
      }
    }
    return Optional.absent();
  }

  /**
   * Approvazione di una richiesta di assenza.
   *
   * @param competenceRequest la richiesta di assenza.
   * @param person            la persona che effettua l'approvazione.
   * @param eventType         il tipo di evento.
   * @param note              eventuali note da aggiungere all'evento generato.
   * @return l'eventuale problema riscontrati durante l'approvazione.
   */
  public Optional<String> executeEvent(
      CompetenceRequest competenceRequest, Person person,
      CompetenceRequestEventType eventType, Optional<String> note) {

    val problem = checkCompetenceRequestEvent(competenceRequest, person, eventType);
    if (problem.isPresent()) {
      log.warn("Impossibile inserire la richiesta di {}. Problema: {}",
          competenceRequest.getType(), problem.get());
      return problem;
    }

    switch (eventType) {
      case STARTING_APPROVAL_FLOW:
        competenceRequest.setFlowStarted(true);
        //invio la notifica al primo che deve validare la mia richiesta 
        notificationManager
        .notificationCompetenceRequestPolicy(competenceRequest.getPerson().getUser(),
            competenceRequest, true);
        // invio anche la mail
        notificationManager
        .sendEmailCompetenceRequestPolicy(competenceRequest.getPerson().getUser(), 
            competenceRequest, true);
        break;

      case FIRST_APPROVAL:
        competenceRequest.setFirstApproved(LocalDateTime.now());
        break;

      case MANAGER_APPROVAL:
        competenceRequest.setManagerApproved(LocalDateTime.now());
        break;

      case MANAGER_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        log.info("Flusso {} rifiutato dal responsabile", competenceRequest);
        competenceRequest.setFlowEnded(true);
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;

      case OFFICE_HEAD_APPROVAL:
        competenceRequest.setOfficeHeadApproved(LocalDateTime.now());
        break;

      case OFFICE_HEAD_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        log.info("Flusso {} rifiutato dal responsabile di sede", competenceRequest);
        competenceRequest.setFlowEnded(true);
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;

      case EMPLOYEE_APPROVAL:
        competenceRequest.setEmployeeApproved(LocalDateTime.now());
        break;

      case EMPLOYEE_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        competenceRequest.setFlowEnded(true);
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;

      case COMPLETE:
        competenceRequest.setManagerApproved(LocalDateTime.now());
        break;

      case DELETE:
        competenceRequest.setFlowEnded(true);
        notificationManager.notificationCompetenceRequestRevoked(competenceRequest, person);
        break;
      case EPAS_REFUSAL:
        resetFlow(competenceRequest);
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;

      default:
        throw new IllegalStateException(
            String.format("Evento di richiesta assenza %s non previsto", eventType));
    }

    val event = CompetenceRequestEvent.builder()
        .competenceRequest(competenceRequest).owner(person.getUser()).eventType(eventType)
        .description(note.orNull())
        .build();
    event.save();

    log.debug("Costruito evento per {}", event.competenceRequest.getType());
    competenceRequest.save();
    checkAndCompleteFlow(competenceRequest);
    return Optional.absent();
  }

  /**
   * Controlla se una richiesta di competenza può essere terminata con successo, in caso positivo
   * effettua l'inserimento della competenza o evento.
   *
   * @param competenceRequest la richiesta da verificare e da utilizzare per i dati dell'inserimento
   *                          assenza.
   * @return un report con l'inserimento dell'assenze se è stato possibile farlo.
   */
  public boolean checkAndCompleteFlow(CompetenceRequest competenceRequest) {
    if (competenceRequest.isFullyApproved() && !competenceRequest.isFlowEnded()) {
      return completeFlow(competenceRequest);
    }
    return false;
  }

  /**
   * Effettua l'inserimento dell'assenza.
   *
   * @param absenceRequest la richiesta di assenza da cui prelevare i dati per l'inserimento.
   * @return il report con i codici di assenza inseriti.
   */
  private boolean completeFlow(CompetenceRequest competenceRequest) {

    competenceRequest.setFlowEnded(true);
    competenceRequest.save();
    log.info("Flusso relativo a {} terminato. ", competenceRequest);
    if (competenceRequest.getType().equals(CompetenceRequestType.OVERTIME_REQUEST)) {
      CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
      Competence competence = new Competence();
      competence.setMonth(competenceRequest.getMonth());
      competence.setYear(competenceRequest.getYear());
      competence.setValueApproved(competenceRequest.getValue());
      competence.setPerson(competenceRequest.getPerson());
      competence.setCompetenceCode(code);
      competence.save();
      consistencyManager.updatePersonSituation(competenceRequest.getPerson().id, 
          new LocalDate(competenceRequest.getYear(), competenceRequest.getMonth(), 1));
      return true;
    } else {
      LocalDate temp = competenceRequest.getBeginDateToGive();
      PersonReperibilityDay repDayAsker = null;
      PersonReperibilityDay repDayGiver = null;

      while (!temp.isAfter(competenceRequest.getEndDateToGive())) {
        //elimino le mie reperibilità
        Optional<PersonReperibilityDay> prd =
            repDao.getPersonReperibilityDay(competenceRequest.getPerson(), temp);
        if (prd.isPresent()) {
          repDayGiver = prd.get();
        } else {
          throw new IllegalArgumentException();
        }

        repDayGiver.delete();
        temp = temp.plusDays(1);
      }

      if (competenceRequest.getBeginDateToAsk() != null 
          && competenceRequest.getEndDateToAsk() != null) {
        temp = competenceRequest.getBeginDateToAsk();

        while (!temp.isAfter(competenceRequest.getEndDateToAsk())) {
          //elimino le reperibilità dell'altro reperibile
          Optional<PersonReperibilityDay> prd =
              repDao.getPersonReperibilityDay(competenceRequest.getTeamMate(), temp);
          if (prd.isPresent()) {
            repDayAsker = prd.get();
          } else {
            throw new IllegalArgumentException();
          }

          repDayAsker.delete();
          temp = temp.plusDays(1);
        }
      }

      JPA.em().flush();

      final List<Promise<Void>> results = new ArrayList<>();

      results.add(new Job<Void>() {

        @Override
        public void doJob() {
          List<Person> repList = Lists.newArrayList();
          repList.add(competenceRequest.getPerson());
          repList.add(competenceRequest.getTeamMate());
          LocalDate temp = competenceRequest.getBeginDateToGive();
          while (!temp.isAfter(competenceRequest.getEndDateToGive())) {
            PersonReperibilityDay day = new PersonReperibilityDay();
            day.setDate(temp);
            day.setReperibilityType(repDao.byListOfPerson(repList).get());

            if (repDao.byPersonDateAndType(competenceRequest.getTeamMate(), temp,
                day.getReperibilityType()).isPresent()) {
              day.setPersonReperibility(repDao
                  .byPersonDateAndType(competenceRequest.getTeamMate(), temp,
                      day.getReperibilityType()).get());
            } else {
              throw new IllegalArgumentException("Non è stato possibile inserire la "
                  + "giornata di reperibilità");
            }
            day.save();
            temp = temp.plusDays(1);
          }

          if (competenceRequest.getBeginDateToAsk() != null 
              && competenceRequest.getEndDateToAsk() != null) {
            temp = competenceRequest.getBeginDateToAsk();
            while (!temp.isAfter(competenceRequest.getEndDateToAsk())) {
              PersonReperibilityDay day = new PersonReperibilityDay();
              day.setDate(temp);
              day.setReperibilityType(repDao.byListOfPerson(repList).get());

              if (repDao.byPersonDateAndType(competenceRequest.getPerson(), temp,
                  day.getReperibilityType()).isPresent()) {
                day.setPersonReperibility(repDao
                    .byPersonDateAndType(competenceRequest.getPerson(), temp,
                        day.getReperibilityType()).get());
              } else {
                throw new IllegalArgumentException("Non è stato possibile inserire la "
                    + "giornata di reperibilità");
              }

              day.save();
              temp = temp.plusDays(1);
            }
          }

        }
      }.afterRequest());
      Promise.waitAll(results);
    }

    return true;
  }

  /**
   * Metodo di utilità per la verifica dell'esistenza di una richiesta di competenza.
   *
   * @param competenceRequest la richiesta di competenza da controllare
   * @return la richiesta di competenza se esiste già con i parametri passati.
   */
  public CompetenceRequest checkCompetenceRequest(CompetenceRequest competenceRequest) {
    if (competenceRequest.getType().equals(CompetenceRequestType.OVERTIME_REQUEST)) {
      Optional<CompetenceRequest> comp = competenceRequestDao.existingOvertimeRequest(competenceRequest);
      if (comp.isPresent()) {
        return comp.get();
      }
      return null;
    } else {
      List<CompetenceRequest> existingList =
          competenceRequestDao.existingCompetenceRequests(competenceRequest);
      for (CompetenceRequest request : existingList) {
        if (request.getMonth().intValue() == competenceRequest.getMonth().intValue()
            && request.getYear().intValue() == competenceRequest.getYear().intValue() 
            && request.getType().equals(competenceRequest.getType())) {
          return request;
        }
      }
      return null;
    }    
  }

  /**
   * Metodo di approvazione delle richieste.
   *
   * @param competenceRequest la richiesta di competenza
   * @param user              l'utente
   * @return true se l'approvazione dell'utente è andata a buon fine, false altrimenti
   */
  public boolean approval(CompetenceRequest competenceRequest, User user) {
    boolean approved = false;

    if (competenceRequest.isEmployeeApprovalRequired() 
        && competenceRequest.getEmployeeApproved() == null
        && user.hasRoles(Role.EMPLOYEE)) {
      employeeApproval(competenceRequest.id, user);
      if (competenceRequest.getPerson().getReperibility().stream()
          .anyMatch(pr -> pr.getPersonReperibilityType().getSupervisor().equals(user.getPerson()))
          && competenceRequest.isManagerApprovalRequired()) {

        managerApproval(competenceRequest.id, user);
      }
      approved = true;
    }
    if (competenceRequest.getType().equals(CompetenceRequestType.OVERTIME_REQUEST)) {
      if (competenceRequest.isManagerApprovalRequired()
          && competenceRequest.getManagerApproved() == null ) {
        managerApproval(competenceRequest.id, user);
        approved = true;
      }
    } else {
      if (competenceRequest.isManagerApprovalRequired() 
          && competenceRequest.getManagerApproved() == null
          && competenceRequest.getPerson().getReperibility().stream()
          .anyMatch(pr -> pr.getPersonReperibilityType().getSupervisor().equals(user.getPerson()))) {
        managerApproval(competenceRequest.id, user);
        approved = true;
      }
    }

    if (competenceRequest.isOfficeHeadApprovalRequired() 
        && competenceRequest.getOfficeHeadApproved() == null) {
      officeHeadApproval(competenceRequest.id, user);
      approved = true;
    }    
    return approved;
  }


  public void officeHeadApproval(long id, User user) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().getPerson();

    if ((Boolean) configurationManager.configValue(currentPerson.getOffice(), 
        EpasParam.OVERTIME_ADVANCE_REQUEST_AND_CONFIRMATION, LocalDate.now()) 
        && competenceRequest.getFirstApproved() == null) {      
      executeEvent(competenceRequest, currentPerson, 
          CompetenceRequestEventType.FIRST_APPROVAL, Optional.absent());
      log.info("{} preventiva approvata dal responsabile di sede {}.", 
          competenceRequest, currentPerson.getFullname());
    } else {
      executeEvent(competenceRequest, currentPerson, 
          CompetenceRequestEventType.OFFICE_HEAD_APPROVAL, Optional.absent());
      log.info("{} approvata dal responsabile di sede {}.", 
          competenceRequest, currentPerson.getFullname());
    }

    notificationManager.notificationCompetenceRequestPolicy(user, competenceRequest, true);
  }

  /**
   * Approvazione richiesta competenza da parte del responsabile di gruppo.
   *
   * @param id id della richiesta di competenza.
   */
  public void managerApproval(long id, User user) {

    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().getPerson();
    if ((Boolean) configurationManager.configValue(currentPerson.getOffice(), 
        EpasParam.OVERTIME_ADVANCE_REQUEST_AND_CONFIRMATION, LocalDate.now()) 
        && competenceRequest.getFirstApproved() == null) {
      executeEvent(competenceRequest, currentPerson, 
          CompetenceRequestEventType.FIRST_APPROVAL, Optional.absent());
      log.info("{} preventiva approvata dal responsabile di gruppo {}.", 
          competenceRequest, currentPerson.getFullname());
    } else {
      executeEvent(
          competenceRequest, currentPerson,
          CompetenceRequestEventType.MANAGER_APPROVAL, Optional.absent());
      log.info("{} approvata dal responsabile di gruppo {}.",
          competenceRequest, currentPerson.getFullname());
    }    

    notificationManager.notificationCompetenceRequestPolicy(user, competenceRequest, true);
  }

  /**
   * Approvazione richiesta competenza da parte del responsabile di sede.
   *
   * @param id id della richiesta di competenza.
   */
  public void employeeApproval(long id, User user) {

    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().getPerson();
    if (competenceRequest.isEmployeeApprovalRequired() 
        && competenceRequest.getEmployeeApproved() == null) {
      log.info("{} approvazione da parte del dipendente {}.",
          competenceRequest, currentPerson.getFullname());
    } else {
      log.info("{} approvazione da parte dal responsabile {}.",
          competenceRequest, currentPerson.getFullname());
    }
    executeEvent(competenceRequest, currentPerson,
        CompetenceRequestEventType.EMPLOYEE_APPROVAL, Optional.absent());
    notificationManager.notificationCompetenceRequestPolicy(user, competenceRequest, true);

  }

  /**
   * Metodo che permette la disapprovazione della richiesta.
   *
   * @param id l'identificativo della richiesta di competenza
   */
  public void reperibilityManagerDisapproval(long id, String reason) {

    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().getPerson();
    executeEvent(
        competenceRequest, currentPerson,
        CompetenceRequestEventType.MANAGER_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di gruppo {}.",
        competenceRequest, currentPerson.getFullname());

  }

  /**
   * Approvazione richiesta assenza da parte del responsabile di sede.
   *
   * @param id id della richiesta di assenza.
   */
  public void employeeDisapproval(long id, String reason) {

    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().getPerson();
    executeEvent(
        competenceRequest, currentPerson,
        CompetenceRequestEventType.EMPLOYEE_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.",
        competenceRequest, currentPerson.getFullname());

  }

  /**
   * Metodo che ritorna la quantità di ore disponibili per lo straordinario.
   * 
   * @param approver l'utente che deve approvare la richiesta
   * @param competenceRequest la richiesta di competenza (straordinario) da verificare
   * @return la quantità di ore di straordinario disponibili per approvazione.
   */
  public int hoursAvailable(User approver, CompetenceRequest competenceRequest) {

    int totalOvertimes = 0;
    int overtimeHoursAlreadyAssigned = 0;
    val config = getConfiguration(competenceRequest.getType(), competenceRequest.getPerson());
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(approver);
    // Controllo le richieste da approvare quante ore di straordinario richiedono
    List<CompetenceRequest> results = competenceRequestDao
        .toApproveResults(roleList, 
            LocalDateTime.now().minusMonths(1), 
            Optional.absent(), CompetenceRequestType.OVERTIME_REQUEST, approver.getPerson());
    if (config.advanceApprovalRequired) {
      results = results.stream()
          .filter(cr -> cr.actualEvent().eventType
              .equals(CompetenceRequestEventType.FIRST_APPROVAL)).collect(Collectors.toList());
    } else {
      results = Lists.newArrayList();
    }
    int overtimePendingRequests = !results.isEmpty() ? 
        results.stream().mapToInt(cr -> cr.getValueRequested()).sum() : 0;
    if (config.managerApprovalRequired) {

      /* 
       * Controllo la quantità già assegnata nel corso dell'anno agli appartenenti al gruppo 
       * del richiedente 
       */
      Group group = groupDao
          .checkManagerPerson(approver.getPerson(), competenceRequest.getPerson()).get();
      Map<Integer, List<PersonOvertimeInMonth>> map = groupOvertimeManager
          .groupOvertimeSituationInYear(group.getPeople(), competenceRequest.getYear());
      overtimeHoursAlreadyAssigned = groupOvertimeManager.groupOvertimeAssignedInYear(map);

      /*
       * Controllo quante ore ha il mio gruppo a disposizione
       */
      totalOvertimes = groupOvertimeManager.totalGroupOvertimes(group);


    }
    if (config.officeHeadApprovalRequired) {
      /*
       * Controllo la quantità già assegnata nel corso dell'anno ai dipendenti della sede
       */
      CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode(code);
      Map<Integer, List<PersonOvertimeInMonth>> map = groupOvertimeManager
          .groupOvertimeSituationInYear(personDao.listForCompetence(competenceCode, 
              Optional.absent(), Sets.newHashSet(approver.getPerson().getOffice()), true, 
              LocalDate.now().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), 
              LocalDate.now(), Optional.absent()).list(), competenceRequest.getYear());
      overtimeHoursAlreadyAssigned = groupOvertimeManager.groupOvertimeAssignedInYear(map);

      /*
       * Controllo quante ore ha la sede a disposizione
       */
      List<TotalOvertime> totalList = competenceDao
          .getTotalOvertime(LocalDate.now().getYear(), approver.getPerson().getOffice());
      totalOvertimes = competenceManager.getTotalOvertime(totalList);

    }
    return totalOvertimes - overtimePendingRequests - overtimeHoursAlreadyAssigned;
  }

  /**
   * Ritorna quante ore di straordinario rimangono alla persona nell'anno.
   * 
   * @param person la persona di cui richiedere le ore di straordinario residue
   * @param year l'anno di riferimento
   * @return la quantità di ore di straordinario residue per la persona nell'anno.
   */
  public int myOvertimeResidual(Person person, int year) {
    //Calcolo le ore di straordinario residue con il monte ore per persona se abilitato in configurazione
    int overtimeResidual = 0;
    GeneralSetting settings = settingDao.generalSetting();
    if (settings.isEnableOvertimePerPerson()) {

      CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
      List<CompetenceCode> codeList = Lists.newArrayList();
      codeList.add(code);
      overtimeResidual = person.totalOvertimeHourInYear(year) - 
          competenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.absent(), 
              Optional.fromNullable(person), Optional.absent(), codeList).or(0);
    }
    return overtimeResidual;
  }
  
  /**
   * Ritorna la quantità di ore di straordinario disponibili per l'intera sede.
   * 
   * @param office la sede per cui controllare le ore disponibili di straordinario
   * @param year l'anno di riferimento
   * @return la quantità di ore di straordinario disponibili per la sede.
   */
  public int seatOvertimeResidual(Office office, int year) {
    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    List<CompetenceCode> codeList = Lists.newArrayList();
    codeList.add(code);
  //Il monte ore della sede
    int totalOvertimes = 0;
    List<TotalOvertime> totalList = competenceDao
      .getTotalOvertime(LocalDate.now().getYear(), office);
    totalOvertimes = competenceManager.getTotalOvertime(totalList);
    
    return totalOvertimes - competenceDao
        .valueOvertimeApprovedByMonthAndYear(year, Optional.absent(), 
        		Optional.absent(), Optional.fromNullable(office), codeList).or(0);
  }
  
  /**
   * 
   * @param person la persona appartenente al gruppo di cui si cercano gli straordinari residui
   * @param year l'anno di riferimento
   * @return la quantità di ore di straordinario residue del gruppo cui appartiene la persona nell'anno.
   */
  public int groupOvertimeResidual(Person person, int year) {
    Group group = groupDao
        .myGroups(person, java.time.LocalDate.now()).get(0);
    Map<Integer, List<PersonOvertimeInMonth>> map = groupOvertimeManager
        .groupOvertimeSituationInYear(group.getPeople(), year);
    int overtimeHoursAlreadyAssigned = groupOvertimeManager.groupOvertimeAssignedInYear(map); 
    int totalOvertimes = groupOvertimeManager.totalGroupOvertimes(group);
    return totalOvertimes - overtimeHoursAlreadyAssigned;
  }
}

