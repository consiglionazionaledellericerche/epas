package manager.flows;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import controllers.Security;
import dao.AbsenceRequestDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.CompetenceRequestDao;
import dao.GroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import dao.absences.AbsenceComponentDao;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.AbsenceManager;
import manager.CompetenceManager;
import manager.ConsistencyManager;
import manager.NotificationManager;
import manager.PersonDayManager;
import manager.ReperibilityManager2;
import manager.configurations.ConfigurationManager;
import manager.flows.AbsenceRequestManager.AbsenceRequestConfiguration;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.Role;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.flows.AbsenceRequest;
import models.flows.AbsenceRequestEvent;
import models.flows.CompetenceRequest;
import models.flows.CompetenceRequestEvent;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.CompetenceRequestEventType;
import models.flows.enumerate.CompetenceRequestType;
import org.apache.commons.compress.utils.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.jobs.Job;
import play.libs.F.Promise;

@Slf4j
public class CompetenceRequestManager {
  


  private ConfigurationManager configurationManager;
  private UsersRolesOfficesDao uroDao;
  private RoleDao roleDao;
  private NotificationManager notificationManager;
  private CompetenceRequestDao competenceRequestDao;  
  private ConsistencyManager consistencyManager;  
  private GroupDao groupDao;
  private PersonDao personDao;
  private PersonReperibilityDayDao repDao;
  private ReperibilityManager2 repManager2;

  @Data
  @RequiredArgsConstructor
  @ToString
  public class CompetenceRequestConfiguration {
    final Person person;
    final CompetenceRequestType type;
    boolean employeeApprovalRequired;
    boolean reperibilityManagerApprovalRequired;
  }

  /**
   * Injector.
   * @param configurationManager configurationManager per la sede
   * @param uroDao dao per gli usersRolesOffices
   * @param roleDao dao per i ruoli
   * @param notificationManager manager per le notifiche
   * @param competenceRequestDao dao per le richieste di competenza
   * @param consistencyManager manager per il conteggio dei residui
   * @param groupDao dao per i gruppi
   * @param personDao dao per la persona
   * @param repDao dao per la reperibiltà 
   * @param repManager2 manager per la reperibilità
   */
  @Inject
  public CompetenceRequestManager(ConfigurationManager configurationManager,
      UsersRolesOfficesDao uroDao, RoleDao roleDao, NotificationManager notificationManager,
      CompetenceRequestDao competenceRequestDao, ConsistencyManager consistencyManager, 
      GroupDao groupDao, PersonDao personDao,
      PersonReperibilityDayDao repDao,
      ReperibilityManager2 repManager2) {
    this.configurationManager = configurationManager;
    this.uroDao = uroDao;
    this.roleDao = roleDao;
    this.notificationManager = notificationManager;
    this.competenceRequestDao = competenceRequestDao;    
    this.consistencyManager = consistencyManager;    
    this.groupDao = groupDao;
    this.personDao = personDao;
    this.repDao = repDao;
    this.repManager2 = repManager2;
  }
  
  /**
   * Verifica che gruppi ed eventuali responsabile di sede siano presenti per
   * poter richiedere il tipo di competenza. 
   * 
   * @param requestType il tipo di competenza da controllare
   * @param person la persona per cui controllare il tipo di assenza
   * @return la lista degli eventuali problemi riscontrati.
   */
  public List<String> checkconfiguration(CompetenceRequestType requestType, Person person) {
    Verify.verifyNotNull(requestType);
    Verify.verifyNotNull(person);

    val problems = Lists.<String>newArrayList();
    val config = getConfiguration(requestType, person);

    if (person.user.hasRoles(Role.GROUP_MANAGER, Role.SEAT_SUPERVISOR)) {
      return Lists.newArrayList();
    }
    
    
    if (config.isReperibilityManagerApprovalRequired()         
        && groupDao.myGroups(person).isEmpty()) {
      problems.add(
          String.format("Approvazione del responsabile di gruppo richiesta. "
              + "La persona %s non ha impostato nessun responsabile di gruppo "
              + "e non appartiene ad alcun gruppo. "
              + "Contattare l'ufficio del personale.",
              person.getFullname())); 
    }
    
    if (config.isEmployeeApprovalRequired() 
        && personDao.byOffice(person.office).isEmpty()) {
      problems.add(String.format("Approvazione di un dipendente richiesta."
          + "L'ufficio %s non ha altri dipendenti."
          + "Contattare l'ufficio del personale.", person.office.getName()));
    }
    return problems;
  }

  /**
   * Verifica quali sono le approvazioni richiesta per questo tipo di competenza
   * per questa persona.
   * 
   * @param requestType il tipo di richiesta di competenza
   * @param person la persona.
   * 
   * @return la configurazione con i tipi di approvazione necessari.
   */
  public CompetenceRequestConfiguration getConfiguration(
      CompetenceRequestType requestType, Person person) {
    val competenceRequestConfiguration = new CompetenceRequestConfiguration(person, requestType);
    if (requestType.alwaysSkipReperibilityManagerApproval) {
      competenceRequestConfiguration.reperibilityManagerApprovalRequired = false;
    } else {
      if (requestType.reperibilityManagerApprovalRequired.isPresent()) {
        competenceRequestConfiguration.reperibilityManagerApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.reperibilityManagerApprovalRequired.get(), 
                LocalDate.now());  
      }
    }

    if (requestType.alwaysSkipEmployeeApproval) {
      competenceRequestConfiguration.employeeApprovalRequired = false;
    } else {
      if (requestType.employeeApprovalRequired.isPresent()) {
        competenceRequestConfiguration.employeeApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.employeeApprovalRequired.get(), 
                LocalDate.now());  
      }
    }
    
    return competenceRequestConfiguration;
  }
  
  /**
   * Imposta nella richiesta di competenza i tipi di approvazione necessari
   * in funzione del tipo di competenza e della configurazione specifica della
   * sede del dipendente.
   * 
   * @param competenceRequest la richiesta di assenza.
   */
  public void configure(CompetenceRequest competenceRequest) {
    Verify.verifyNotNull(competenceRequest.type);
    Verify.verifyNotNull(competenceRequest.person);

    val config = getConfiguration(competenceRequest.type, competenceRequest.person);
    
    competenceRequest.reperibilityManagerApprovalRequired = 
        config.reperibilityManagerApprovalRequired;
    
    competenceRequest.employeeApprovalRequired = 
        config.employeeApprovalRequired;
  }
  
  /**
   * Rimuove tutte le eventuali approvazioni ed impostata il flusso come
   * da avviare.
   * 
   * @param competenceRequest la richiesta di competenza
   */
  public void resetFlow(CompetenceRequest competenceRequest) {
    competenceRequest.flowStarted = false;
    competenceRequest.reperibilityManagerApproved = null;
    competenceRequest.employeeApproved = null;
  }

  /**
   * Metodo che verifica se la richiesta può essere approvata o se non è necessario.
   * @param competenceRequest la richiesta di competenza
   * @param approver chi deve approvarla
   * @param eventType che tipo di evento stiamo considerando
   * @return una stringa opzionale che contiene lo stato della richiesta.
   */
  public Optional<String> checkCompetenceRequestEvent(CompetenceRequest competenceRequest, 
      Person approver, CompetenceRequestEventType eventType) {
    if (eventType == CompetenceRequestEventType.STARTING_APPROVAL_FLOW) {
      if (!competenceRequest.person.equals(approver)) {
        return Optional.of("Il flusso può essere avviato solamente dal diretto interessato.");
      }
      if (competenceRequest.flowStarted) {
        return Optional.of("Flusso già avviato, impossibile avviarlo di nuovo.");
      }
    }
    
    if (eventType == CompetenceRequestEventType.REPERIBILITY_MANAGER_APPROVAL 
        || eventType == CompetenceRequestEventType.REPERIBILITY_MANAGER_REFUSAL) {
      if (!competenceRequest.reperibilityManagerApprovalRequired) {
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
      if (!competenceRequest.employeeApprovalRequired) {
        return Optional.of("Questa richiesta di competenza non prevede approvazione/rifiuto "
            + "da parte di un dipendente");
      }
      if (competenceRequest.isEmployeeApproved()) {
        return Optional.of("Questa richiesta di competenza è già stata approvata "
            + "da parte di un dipendente");
      }
      if (!uroDao.getUsersRolesOffices(approver.user, roleDao.getRoleByName(Role.EMPLOYEE),
          competenceRequest.person.office).isPresent()) {
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
   * @param person la persona che effettua l'approvazione.
   * @param eventType il tipo di evento.
   * @param note eventuali note da aggiungere all'evento generato.
   * @return l'eventuale problema riscontrati durante l'approvazione.
   */
  public Optional<String> executeEvent(
      CompetenceRequest competenceRequest, Person person, 
      CompetenceRequestEventType eventType, Optional<String> note) {

    val problem = checkCompetenceRequestEvent(competenceRequest, person, eventType);
    if (problem.isPresent()) {
      log.warn("Impossibile inserire la richiesta di {}. Problema: {}", 
          competenceRequest.type, problem.get());
      return problem;
    }

    switch (eventType) {
      case STARTING_APPROVAL_FLOW:
        competenceRequest.flowStarted = true;
        break;

      case REPERIBILITY_MANAGER_APPROVAL:
        competenceRequest.reperibilityManagerApproved = LocalDateTime.now();
        break;

      case REPERIBILITY_MANAGER_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        competenceRequest.flowEnded = true;
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;
        
      case EMPLOYEE_APPROVAL:
        competenceRequest.employeeApproved = LocalDateTime.now();
        break;

      case EMPLOYEE_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        competenceRequest.flowEnded = true;
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;

      case COMPLETE:
        competenceRequest.reperibilityManagerApproved = LocalDateTime.now();
        break;

      case DELETE:
        competenceRequest.flowEnded = true;
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
        .competenceRequest(competenceRequest).owner(person.user).eventType(eventType)
        .description(note.orNull())
        .build();
    event.save();

    log.info("Costruito evento per {}", event.competenceRequest.type);
    competenceRequest.save();
    checkAndCompleteFlow(competenceRequest);
    return Optional.absent();
  }
  
  /**
   * Controlla se una richiesta di competenza può essere terminata con successo,
   * in caso positivo effettua l'inserimento della competenza o evento.
   * 
   * @param competenceRequest la richiesta da verificare e da utilizzare per i dati
   *     dell'inserimento assenza.
   * @return un report con l'inserimento dell'assenze se è stato possibile farlo.
   */
  public boolean checkAndCompleteFlow(CompetenceRequest competenceRequest) {
    if (competenceRequest.isFullyApproved() && !competenceRequest.flowEnded) {
      return completeFlow(competenceRequest);
    }
    return false;
  }
  
  /**
   * Effettua l'inserimento dell'assenza.
   * 
   * @param absenceRequest la richiesta di assenza da cui prelevare i
   *     dati per l'inserimento. 
   * @return il report con i codici di assenza inseriti.
   */
  private boolean completeFlow(CompetenceRequest competenceRequest) {

    competenceRequest.flowEnded = true;
    competenceRequest.save();
    log.info("Flusso relativo a {} terminato. ", competenceRequest);

    if (competenceRequest.type == CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST) {
      LocalDate temp = competenceRequest.beginDateToGive;
      PersonReperibilityDay repDayAsker = null;
      PersonReperibilityDay repDayGiver = null;
      
      while (!temp.isAfter(competenceRequest.endDateToGive)) {
        //elimino le mie reperibilità
        Optional<PersonReperibilityDay> prd = 
            repDao.getPersonReperibilityDay(competenceRequest.person, temp);
        if (prd.isPresent()) {
          repDayGiver = prd.get();
        } else {
          throw new IllegalArgumentException();
        }
        
        repDayGiver.delete();
        temp = temp.plusDays(1);
      }

      temp = competenceRequest.beginDateToAsk;
      while (!temp.isAfter(competenceRequest.endDateToAsk)) {
        //elimino le reperibilità dell'altro reperibile
        Optional<PersonReperibilityDay> prd =
            repDao.getPersonReperibilityDay(competenceRequest.teamMate, temp);
        if (prd.isPresent()) {
          repDayAsker = prd.get();
        } else {
          throw new IllegalArgumentException();
        }
        
        repDayAsker.delete();
        temp = temp.plusDays(1);
      }
      
      JPA.em().flush();
      
      final List<Promise<Void>> results = new ArrayList<>();
      
      results.add(new Job<Void>() {
        
        @Override
        public void doJob() {
          List<Person> repList = Lists.newArrayList();
          repList.add(competenceRequest.person);
          repList.add(competenceRequest.teamMate);
          LocalDate temp = competenceRequest.beginDateToGive;
          while(!temp.isAfter(competenceRequest.endDateToGive)) {
            PersonReperibilityDay day = new PersonReperibilityDay();
            day.date = temp;
            day.reperibilityType = repDao.byListOfPerson(repList).get();
            
            if (repDao.byPersonDateAndType(competenceRequest.teamMate, temp, 
                day.reperibilityType).isPresent()) {
              day.personReperibility = 
                  repDao.byPersonDateAndType(competenceRequest.teamMate, temp, 
                      day.reperibilityType).get();
            } else {
              throw new IllegalArgumentException("Non è stato possibile inserire la "
                  + "giornata di reperibilità");
            }
            day.save();
            temp = temp.plusDays(1);
          }
          
          temp = competenceRequest.beginDateToAsk;
          while (!temp.isAfter(competenceRequest.endDateToAsk)) {
            PersonReperibilityDay day = new PersonReperibilityDay();
            day.date = temp;
            day.reperibilityType = repDao.byListOfPerson(repList).get();
            
            if (repDao.byPersonDateAndType(competenceRequest.person, temp, 
                day.reperibilityType).isPresent()) {
              day.personReperibility = 
                  repDao.byPersonDateAndType(competenceRequest.person, temp, 
                      day.reperibilityType).get();
            } else {
              throw new IllegalArgumentException("Non è stato possibile inserire la "
                  + "giornata di reperibilità");
            }
            
            day.save();
            temp = temp.plusDays(1);
          }
          
        }
      }.afterRequest());
      Promise.waitAll(results);
    }
    
    return true;
  }
  
  /**
   * Metodo di utilità per la verifica dell'esistenza di una richiesta di competenza.
   * @param competenceRequest la richiesta di competenza da controllare
   * @return la richiesta di competenza se esiste già con i parametri passati.
   */
  public CompetenceRequest checkCompetenceRequest(CompetenceRequest competenceRequest) {
    List<CompetenceRequest> existingList = 
        competenceRequestDao.existingCompetenceRequests(competenceRequest);
    for (CompetenceRequest request : existingList) {
      if (request.month == competenceRequest.month 
          && request.year == competenceRequest.year && request.type == competenceRequest.type) {
        return request;
      }
    }
    return null;
  }
  
  /**
   * Metodo di approvazione delle richieste.
   * @param competenceRequest la richiesta di competenza
   * @param user l'utente
   * @return true se l'approvazione dell'utente è andata a buon fine, false altrimenti
   */
  public boolean approval(CompetenceRequest competenceRequest, User user) {
    if (competenceRequest.employeeApprovalRequired && competenceRequest.employeeApproved == null
        && user.hasRoles(Role.EMPLOYEE)) {
      employeeApproval(competenceRequest.id, user);      
      if (competenceRequest.person.reperibility.stream()
          .anyMatch(pr -> pr.personReperibilityType.supervisor.equals(user.person))
          && competenceRequest.reperibilityManagerApprovalRequired) {
        //TODO: se il dipendente è anche supervisore del servizio faccio un'unica approvazione
        reperibilityManagerApproval(competenceRequest.id, user);
      }
      return true;
    }
    if (competenceRequest.reperibilityManagerApprovalRequired 
        && competenceRequest.reperibilityManagerApproved == null
        && competenceRequest.person.reperibility.stream()
        .anyMatch(pr -> pr.personReperibilityType.supervisor.equals(user.person))) {
      
      reperibilityManagerApproval(competenceRequest.id, user);
      return true;
    }
    return false;
  }
  
  
  /**
   * Approvazione richiesta competenza da parte del responsabile di gruppo.
   * @param id id della richiesta di competenza.
   */
  public void reperibilityManagerApproval(long id, User user) {

    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        competenceRequest, currentPerson, 
        CompetenceRequestEventType.REPERIBILITY_MANAGER_APPROVAL, Optional.absent());
    log.info("{} approvata dal responsabile di gruppo {}.",
        competenceRequest, currentPerson.getFullname());
    
    notificationManager.notificationCompetenceRequestPolicy(user, competenceRequest, true);
  }
  
  /**
   * Approvazione richiesta competenza da parte del responsabile di sede.
   * @param id id della richiesta di competenza.
   */
  public void employeeApproval(long id, User user) {

    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    if (competenceRequest.employeeApprovalRequired && competenceRequest.employeeApproved == null) {
      executeEvent(competenceRequest, currentPerson, 
          CompetenceRequestEventType.EMPLOYEE_APPROVAL, Optional.absent());
      log.info("{} approvata dal responsabile di sede {} nelle veci del responsabile di gruppo.",
          competenceRequest, currentPerson.getFullname());
    }
    executeEvent(
        competenceRequest, currentPerson, 
        CompetenceRequestEventType.EMPLOYEE_APPROVAL, Optional.absent());
    log.info("{} approvata dal responsabile di sede {}.",
        competenceRequest, currentPerson.getFullname());   
    notificationManager.notificationCompetenceRequestPolicy(user, competenceRequest, true);

  }
  
  /**
   * Metodo che permette la disapprovazione della richiesta.
   * @param id l'identificativo della richiesta di competenza
   */
  public void reperibilityManagerDisapproval(long id, String reason) {

    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        competenceRequest, currentPerson, 
        CompetenceRequestEventType.REPERIBILITY_MANAGER_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di gruppo {}.",
        competenceRequest, currentPerson.getFullname());

  }

  /**
   * Approvazione richiesta assenza da parte del responsabile di sede.
   * @param id id della richiesta di assenza.
   */
  public void employeeDisapproval(long id, String reason) {

    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        competenceRequest, currentPerson, 
        CompetenceRequestEventType.EMPLOYEE_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.",
        competenceRequest, currentPerson.getFullname());   

  }

}
