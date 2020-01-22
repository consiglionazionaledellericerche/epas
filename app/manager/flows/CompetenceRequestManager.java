package manager.flows;


import java.util.List;
import javax.inject.Inject;
import org.apache.commons.compress.utils.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
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
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import dao.absences.AbsenceComponentDao;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.CompetenceManager;
import manager.ConsistencyManager;
import manager.NotificationManager;
import manager.PersonDayManager;
import manager.configurations.ConfigurationManager;
import manager.flows.AbsenceRequestManager.AbsenceRequestConfiguration;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.Role;
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
import play.db.jpa.JPA;

@Slf4j
public class CompetenceRequestManager {
  
  private static final String DAILY_OVERTIME = "S1";

  private ConfigurationManager configurationManager;
  private UsersRolesOfficesDao uroDao;
  private RoleDao roleDao;
  private NotificationManager notificationManager;
  private CompetenceRequestDao competenceRequestDao;  
  private PersonDayManager personDayManager;
  private ConsistencyManager consistencyManager;  
  private GroupDao groupDao;
  private PersonDao personDao;
  private CompetenceManager competenceManager;
  private CompetenceDao competenceDao;
  private CompetenceCodeDao competenceCodeDao;

  @Data
  @RequiredArgsConstructor
  @ToString
  public class CompetenceRequestConfiguration {
    final Person person;
    final CompetenceRequestType type;
    boolean employeeApprovalRequired;
    boolean officeHeadApprovalRequired;
    boolean managerApprovalRequired;
    boolean administrativeApprovalRequired;    
  }

  @Inject
  public CompetenceRequestManager(ConfigurationManager configurationManager,
      UsersRolesOfficesDao uroDao, RoleDao roleDao, NotificationManager notificationManager,
      CompetenceRequestDao competenceRequestDao, PersonDayManager personDayManager, 
      ConsistencyManager consistencyManager, GroupDao groupDao, PersonDao personDao,
      CompetenceManager competenceManager, CompetenceDao competenceDao, 
      CompetenceCodeDao competenceCodeDao) {
    this.configurationManager = configurationManager;
    this.uroDao = uroDao;
    this.roleDao = roleDao;
    this.notificationManager = notificationManager;
    this.competenceRequestDao = competenceRequestDao;    
    this.personDayManager = personDayManager;
    this.consistencyManager = consistencyManager;    
    this.groupDao = groupDao;
    this.personDao = personDao;
    this.competenceManager = competenceManager;
    this.competenceDao = competenceDao;
    this.competenceCodeDao = competenceCodeDao;
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
    if (config.isAdministrativeApprovalRequired() 
        && uroDao.getUsersWithRoleOnOffice(
            roleDao.getRoleByName(Role.PERSONNEL_ADMIN), person.office).isEmpty()) {
      problems.add(
          String.format("Approvazione dell'amministratore del personale richiesta. "
              + "L'ufficio %s non ha impostato nessun amministratore del personale. "
              + "Contattare l'ufficio del personale.",
              person.office.getName())); 
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

    if (config.isOfficeHeadApprovalRequired() 
        && uroDao.getUsersWithRoleOnOffice(
            roleDao.getRoleByName(Role.SEAT_SUPERVISOR), person.office).isEmpty()) {
      problems.add(
          String.format("Approvazione del responsabile di sede richiesta. "
              + "L'ufficio %s non ha impostato nessun responsabile di sede. "
              + "Contattare l'ufficio del personale.",
              person.office.getName())); 
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
    if (requestType.alwaysSkipAdministrativeApproval) {
      competenceRequestConfiguration.administrativeApprovalRequired = false;
    } else {        
      if (requestType.administrativeApprovalRequiredTechnicianLevel.isPresent()) {
        competenceRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.administrativeApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now()); 
      }
    }
    if (requestType.alwaysSkipManagerApproval || person.isGroupManager()) {
      competenceRequestConfiguration.managerApprovalRequired = false;
    } else {
      if (!person.isTopQualification() 
          && requestType.managerApprovalRequiredTechnicianLevel.isPresent()) {
        competenceRequestConfiguration.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.managerApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }
    if (requestType.alwaysSkipOfficeHeadApproval) {
      competenceRequestConfiguration.officeHeadApprovalRequired = false;
    } else {
      if (!person.isTopQualification() 
          && requestType.officeHeadApprovalRequiredTechnicianLevel.isPresent()) {
        competenceRequestConfiguration.officeHeadApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.officeHeadApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }
    if (requestType.alwaysSkipEmployeeApproval) {
      competenceRequestConfiguration.employeeApprovalRequired = false;
    } else {
      if (!person.isTopQualification() 
          && requestType.employeeApprovalRequired.isPresent()) {
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

    competenceRequest.officeHeadApprovalRequired = config.officeHeadApprovalRequired;
    competenceRequest.managerApprovalRequired = config.managerApprovalRequired;
    competenceRequest.administrativeApprovalRequired = config.administrativeApprovalRequired;
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
    competenceRequest.managerApproved = null;
    competenceRequest.administrativeApproved = null;
    competenceRequest.officeHeadApproved = null;
    competenceRequest.employeeApproved = null;
  }

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
    
    if (eventType == CompetenceRequestEventType.MANAGER_APPROVAL 
        || eventType == CompetenceRequestEventType.MANAGER_REFUSAL) {
      if (!competenceRequest.managerApprovalRequired) {
        return Optional.of("Questa richiesta non prevede approvazione/rifiuto "
            + "da parte del responsabile di gruppo.");
      }
      if (competenceRequest.isManagerApproved()) {
        return Optional.of("Questa richiesta è già stata approvata "
            + "da parte del responsabile di gruppo.");
      }
    }
    
    if (eventType == CompetenceRequestEventType.ADMINISTRATIVE_APPROVAL 
        || eventType == CompetenceRequestEventType.ADMINISTRATIVE_REFUSAL) {
      if (!competenceRequest.administrativeApprovalRequired) {
        return Optional.of("Questa richiesta non prevede approvazione/rifiuto "
            + "da parte dell'amministrazione del personale.");
      }
      if (competenceRequest.isAdministrativeApproved()) {
        return Optional.of("Questa richiesta è già stata approvata "
            + "da parte dell'amministrazione del personale.");
      }
      if (!uroDao.getUsersRolesOffices(
          competenceRequest.person.user, roleDao.getRoleByName(Role.PERSONNEL_ADMIN),
          competenceRequest.person.office).isPresent()) {
        return Optional.of(
            String.format("L'evento %s non può essere eseguito da %s perché non ha"
                + " il ruolo di amministratore del personale.", eventType, approver.getFullname()));
      }
    }
    
    if (eventType == CompetenceRequestEventType.OFFICE_HEAD_APPROVAL 
        || eventType == CompetenceRequestEventType.OFFICE_HEAD_REFUSAL) {
      if (!competenceRequest.officeHeadApprovalRequired) {
        return Optional.of("Questa richiesta non prevede approvazione/rifiuto "
            + "da parte del responsabile di sede.");
      }
      if (competenceRequest.isOfficeHeadApproved()) {
        return Optional.of("Questa richiesta è già stata approvata "
            + "da parte del responsabile di sede.");
      }
      if (!uroDao.getUsersRolesOffices(
          approver.user, roleDao.getRoleByName(Role.SEAT_SUPERVISOR),
          competenceRequest.person.office).isPresent()) {
        return Optional.of(
            String.format("L'evento %s non può essere eseguito da %s perché non ha"
                + " il ruolo di responsabile di sede.", eventType, approver.getFullname()));
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
   * @param absenceRequest la richiesta di assenza. 
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
      log.warn("Impossibile inserire la richiesta di assenza {}. Problema: {}", 
          competenceRequest, problem.get());
      return problem;
    }

    switch (eventType) {
      case STARTING_APPROVAL_FLOW:
        competenceRequest.flowStarted = true;
        break;

      case MANAGER_APPROVAL:
        competenceRequest.managerApproved = LocalDateTime.now();
        break;

      case MANAGER_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        competenceRequest.flowEnded = true;
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;

      case ADMINISTRATIVE_APPROVAL:
        competenceRequest.administrativeApproved = LocalDateTime.now();
        break;

      case ADMINISTRATIVE_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        competenceRequest.flowEnded = true;
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;

      case OFFICE_HEAD_APPROVAL:
        competenceRequest.officeHeadApproved = LocalDateTime.now();
        break;

      case OFFICE_HEAD_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        competenceRequest.flowEnded = true;
        notificationManager.notificationCompetenceRequestRefused(competenceRequest, person);
        break;

      case COMPLETE:
        competenceRequest.managerApproved = LocalDateTime.now();
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
   * @param absenceRequest la richiesta da verificare e da utilizzare per i dati
   *     dell'inserimento assenza.
   * @return un report con l'inserimento dell'assenze se è stato possibile farlo.
   */
  public Optional<Competence> checkAndCompleteFlow(CompetenceRequest competenceRequest) {
    if (competenceRequest.isFullyApproved() && !competenceRequest.flowEnded) {
      return Optional.of(completeFlow(competenceRequest));
    }
    return Optional.absent();
  }
  
  /**
   * Effettua l'inserimento dell'assenza.
   * 
   * @param absenceRequest la richiesta di assenza da cui prelevare i
   *     dati per l'inserimento. 
   * @return il report con i codici di assenza inseriti.
   */
  private Competence completeFlow(CompetenceRequest competenceRequest) {

    competenceRequest.flowEnded = true;
    competenceRequest.save();
    log.info("Flusso relativo a {} terminato. ", competenceRequest);
    CompetenceCode code = null;
    Optional<Competence> competence = Optional.absent();
    if (competenceRequest.type == CompetenceRequestType.OVERTIME_REQUEST) {
      code = competenceCodeDao.getCompetenceCodeByCode(DAILY_OVERTIME);      
      competence = competenceDao.getCompetence(competenceRequest.person, 
          competenceRequest.year, competenceRequest.month, code);
      if (!competence.isPresent()) {
        return null;
      }
      competenceManager.saveCompetence(competence.get(), competenceRequest.value);
      consistencyManager.updatePersonSituation(competenceRequest.person.id,
          new LocalDate(competenceRequest.year, competenceRequest.month, 1));
    }

    return competence.get();
  }
  
  public CompetenceRequest checkCompetenceRequest(CompetenceRequest competenceRequest) {
    List<CompetenceRequest> existingList = competenceRequestDao.existingCompetenceRequests(competenceRequest);
    for (CompetenceRequest request : existingList) {
      if (request.month == competenceRequest.month 
          && request.year == competenceRequest.year && request.type == competenceRequest.type) {
        return request;
      }
    }
    return null;
  }

}
