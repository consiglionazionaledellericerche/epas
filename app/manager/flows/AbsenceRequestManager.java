package manager.flows;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import controllers.Security;
import dao.AbsenceRequestDao;
import dao.GroupDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import dao.absences.AbsenceComponentDao;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.NotificationManager;
import manager.PersonDayManager;
import manager.configurations.ConfigurationManager;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Person;
import models.PersonDay;
import models.Role;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.flows.AbsenceRequest;
import models.flows.AbsenceRequestEvent;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import org.apache.commons.compress.utils.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.db.jpa.JPA;

/**
 * Operazioni sulle richiesta di assenza.
 * 
 * @author cristian
 *
 */
@Slf4j
public class AbsenceRequestManager {


  private ConfigurationManager configurationManager;
  private UsersRolesOfficesDao uroDao;
  private RoleDao roleDao;
  private NotificationManager notificationManager;
  private AbsenceService absenceService;
  private AbsenceManager absenceManager;
  private AbsenceComponentDao absenceDao;
  private PersonDayManager personDayManager;
  private ConsistencyManager consistencyManager;
  private AbsenceRequestDao absenceRequestDao;
  private GroupDao groupDao;

  @Data
  @RequiredArgsConstructor
  @ToString
  public class AbsenceRequestConfiguration {
    final Person person;
    final AbsenceRequestType type;
    boolean officeHeadApprovalRequired;
    boolean managerApprovalRequired;
    boolean administrativeApprovalRequired;
    boolean officeHeadApprovalForManagerRequired;
    boolean allDay;
  }

  /**
   * Inizializzazione con injection dei vari componenti necessari.
   * 
   * @param configurationManager Manager delle configurazioni
   * @param uroDao UsersRolesOfficesDao
   * @param roleDao RoleDao
   */
  @Inject
  public AbsenceRequestManager(ConfigurationManager configurationManager,
      UsersRolesOfficesDao uroDao, RoleDao roleDao, NotificationManager notificationManager,
      AbsenceService absenceService, AbsenceManager absenceManager, 
      AbsenceComponentDao absenceDao, PersonDayManager personDayManager, 
      ConsistencyManager consistencyManager, AbsenceRequestDao absenceRequestDao, 
      GroupDao groupDao) {
    this.configurationManager = configurationManager;
    this.uroDao = uroDao;
    this.roleDao = roleDao;
    this.notificationManager = notificationManager;
    this.absenceService = absenceService;
    this.absenceManager = absenceManager;
    this.absenceDao = absenceDao;
    this.personDayManager = personDayManager;
    this.consistencyManager = consistencyManager;
    this.absenceRequestDao = absenceRequestDao;
    this.groupDao = groupDao;
  }

  private static final String FERIE_CNR = "FERIE_CNR";
  private static final String RIPOSI_CNR = "RIPOSI_CNR";

  /**
   * Verifica che gruppi ed eventuali responsabile di sede siano presenti per
   * poter richiedere il tipo di assenza. 
   * 
   * @param requestType il tipo di assenza da controllare
   * @param person la persona per cui controllare il tipo di assenza
   * @return la lista degli eventuali problemi riscontrati.
   */
  public List<String> checkconfiguration(AbsenceRequestType requestType, Person person) {
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
    return problems;
  }

  /**
   * Verifica quali sono le approvazioni richiesta per questo tipo di assenza
   * per questa persona.
   * 
   * @param requestType il tipo di richiesta di assenza
   * @param person la persona.
   * 
   * @return la configurazione con i tipi di approvazione necessari.
   */
  public AbsenceRequestConfiguration getConfiguration(
      AbsenceRequestType requestType, Person person) {
    val absenceRequestConfiguration = new AbsenceRequestConfiguration(person, requestType);

    if (requestType.alwaysSkipAdministrativeApproval) {
      absenceRequestConfiguration.administrativeApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.administrativeApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.administrativeApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.administrativeApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.administrativeApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }

    if (requestType.alwaysSkipManagerApproval || person.isGroupManager()) {
      absenceRequestConfiguration.managerApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.managerApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.managerApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.managerApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.managerApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }    
    if (requestType.alwaysSkipOfficeHeadApproval) {
      absenceRequestConfiguration.officeHeadApprovalRequired = false;
    } else {      
      if (person.isTopQualification() 
          && requestType.officeHeadApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.officeHeadApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.officeHeadApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.officeHeadApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.officeHeadApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.officeHeadApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
      
    }
    if (requestType.alwaysSkipOfficeHeadApprovalForManager) {
      absenceRequestConfiguration.officeHeadApprovalForManagerRequired = false;
    } else {
      if (person.isGroupManager()) {
        absenceRequestConfiguration.officeHeadApprovalForManagerRequired =
            (Boolean) configurationManager.configValue(person.office, 
                requestType.officeHeadApprovalRequiredForManager.get(), LocalDate.now());
      }
    }
    absenceRequestConfiguration.allDay = requestType.allDay;
    return absenceRequestConfiguration;
  } 

  /**
   * Imposta nella richiesta di assenza i tipi di approvazione necessari
   * in funzione del tipo di assenza e della configurazione specifica della
   * sede del dipendente.
   * 
   * @param absenceRequest la richiesta di assenza.
   */
  public void configure(AbsenceRequest absenceRequest) {
    Verify.verifyNotNull(absenceRequest.type);
    Verify.verifyNotNull(absenceRequest.person);

    val config = getConfiguration(absenceRequest.type, absenceRequest.person);

    absenceRequest.officeHeadApprovalRequired = config.officeHeadApprovalRequired;
    absenceRequest.managerApprovalRequired = config.managerApprovalRequired;
    absenceRequest.administrativeApprovalRequired = config.administrativeApprovalRequired;
    absenceRequest.officeHeadApprovalForManagerRequired = 
        config.officeHeadApprovalForManagerRequired;
  }

  /**
   * Rimuove tutte le eventuali approvazioni ed impostata il flusso come
   * da avviare.
   * 
   * @param absenceRequest la richiesta di assenza
   */
  public void resetFlow(AbsenceRequest absenceRequest) {
    absenceRequest.flowStarted = false;
    absenceRequest.managerApproved = null;
    absenceRequest.administrativeApproved = null;
    absenceRequest.officeHeadApproved = null;
  }

  /**
   * Verifica se il tipo di evento è eseguibile dall'utente indicato.
   * 
   * @param absenceRequest la richiesta di assenza. 
   * @param approver la persona che effettua l'approvazione.
   * @param eventType il tipo di evento.
   * @return l'eventuale problema riscontrati durante l'approvazione.
   */
  public Optional<String> checkAbsenceRequestEvent(
      AbsenceRequest absenceRequest, Person approver, AbsenceRequestEventType eventType) {

    if (eventType == AbsenceRequestEventType.STARTING_APPROVAL_FLOW) {
      if (!absenceRequest.person.equals(approver)) {
        return Optional.of("Il flusso può essere avviato solamente dal diretto interessato.");
      }
      if (absenceRequest.flowStarted) {
        return Optional.of("Flusso già avviato, impossibile avviarlo di nuovo.");
      }
    }

    if (eventType == AbsenceRequestEventType.MANAGER_APPROVAL 
        || eventType == AbsenceRequestEventType.MANAGER_REFUSAL) {
      if (!absenceRequest.managerApprovalRequired) {
        return Optional.of("Questa richiesta di assenza non prevede approvazione/rifiuto "
            + "da parte del responsabile di gruppo.");
      }
      if (absenceRequest.isManagerApproved()) {
        return Optional.of("Questa richiesta di assenza è già stata approvata "
            + "da parte del responsabile di gruppo.");
      }

    }

    if (eventType == AbsenceRequestEventType.ADMINISTRATIVE_APPROVAL 
        || eventType == AbsenceRequestEventType.ADMINISTRATIVE_REFUSAL) {
      if (!absenceRequest.administrativeApprovalRequired) {
        return Optional.of("Questa richiesta di assenza non prevede approvazione/rifiuto "
            + "da parte dell'amministrazione del personale.");
      }
      if (absenceRequest.isAdministrativeApproved()) {
        return Optional.of("Questa richiesta di assenza è già stata approvata "
            + "da parte dell'amministrazione del personale.");
      }
      if (!uroDao.getUsersRolesOffices(
          absenceRequest.person.user, roleDao.getRoleByName(Role.PERSONNEL_ADMIN),
          absenceRequest.person.office).isPresent()) {
        return Optional.of(
            String.format("L'evento %s non può essere eseguito da %s perché non ha"
                + " il ruolo di amministratore del personale.", eventType, approver.getFullname()));
      }
    }

    if (eventType == AbsenceRequestEventType.OFFICE_HEAD_APPROVAL 
        || eventType == AbsenceRequestEventType.OFFICE_HEAD_REFUSAL) {
      if (!absenceRequest.officeHeadApprovalRequired 
          && !absenceRequest.officeHeadApprovalForManagerRequired) {
        return Optional.of("Questa richiesta di assenza non prevede approvazione/rifiuto "
            + "da parte del responsabile di sede.");
      }
      if (absenceRequest.isOfficeHeadApproved()) {
        return Optional.of("Questa richiesta di assenza è già stata approvata "
            + "da parte del responsabile di sede.");
      }
      if (!uroDao.getUsersRolesOffices(
          approver.user, roleDao.getRoleByName(Role.SEAT_SUPERVISOR),
          absenceRequest.person.office).isPresent()) {
        return Optional.of(
            String.format("L'evento %s non può essere eseguito da %s perché non ha"
                + " il ruolo di responsabile di sede.", eventType, approver.getFullname()));
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
      AbsenceRequest absenceRequest, Person person, 
      AbsenceRequestEventType eventType, Optional<String> note) {

    val problem = checkAbsenceRequestEvent(absenceRequest, person, eventType);
    if (problem.isPresent()) {
      log.warn("Impossibile inserire la richiesta di assenza {}. Problema: {}", 
          absenceRequest, problem.get());
      return problem;
    }

    switch (eventType) {
      case STARTING_APPROVAL_FLOW:
        absenceRequest.flowStarted = true;
        break;

      case MANAGER_APPROVAL:
        absenceRequest.managerApproved = LocalDateTime.now();
        break;

      case MANAGER_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        absenceRequest.flowEnded = true;
        notificationManager.notificationAbsenceRequestRefused(absenceRequest, person);
        break;

      case ADMINISTRATIVE_APPROVAL:
        absenceRequest.administrativeApproved = LocalDateTime.now();
        break;

      case ADMINISTRATIVE_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        absenceRequest.flowEnded = true;
        notificationManager.notificationAbsenceRequestRefused(absenceRequest, person);
        break;

      case OFFICE_HEAD_APPROVAL:
        absenceRequest.officeHeadApproved = LocalDateTime.now();
        break;

      case OFFICE_HEAD_REFUSAL:
        //si riparte dall'inizio del flusso.
        //resetFlow(absenceRequest);
        absenceRequest.flowEnded = true;
        notificationManager.notificationAbsenceRequestRefused(absenceRequest, person);
        break;

      case COMPLETE:
        absenceRequest.managerApproved = LocalDateTime.now();
        break;

      case DELETE:
        absenceRequest.flowEnded = true;
        break;
      case EPAS_REFUSAL:
        resetFlow(absenceRequest);
        notificationManager.notificationAbsenceRequestRefused(absenceRequest, person);
        break;

      default:
        throw new IllegalStateException(
            String.format("Evento di richiesta assenza %s non previsto", eventType));
    }

    val event = AbsenceRequestEvent.builder()
        .absenceRequest(absenceRequest).owner(person.user).eventType(eventType)
        .description(note.orNull())
        .build();
    event.save();

    log.info("Costruito evento per richiesta di assenza {}", event);
    absenceRequest.save();
    checkAndCompleteFlow(absenceRequest);
    return Optional.absent();
  }


  /**
   * Controlla se una richiesta di assenza può essere terminata con successo,
   * in caso positivo effettua l'inserimento delle assenze.
   * 
   * @param absenceRequest la richiesta da verificare e da utilizzare per i dati
   *     dell'inserimento assenza.
   * @return un report con l'inserimento dell'assenze se è stato possibile farlo.
   */
  public Optional<InsertReport> checkAndCompleteFlow(AbsenceRequest absenceRequest) {
    if (absenceRequest.isFullyApproved() && !absenceRequest.flowEnded) {
      return Optional.of(completeFlow(absenceRequest));
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
  private InsertReport completeFlow(AbsenceRequest absenceRequest) {

    absenceRequest.flowEnded = true;
    absenceRequest.save();
    log.info("Flusso relativo a {} terminato. Inserimento in corso delle assenze.", absenceRequest);
    GroupAbsenceType groupAbsenceType = getGroupAbsenceType(absenceRequest);
    AbsenceType absenceType = null;
    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(absenceRequest.person, absenceRequest.startAtAsDate(), null,
            absenceRequest.endToAsDate(), null, groupAbsenceType, false, absenceType, 
            null, null, null, false, true);
    InsertReport insertReport = absenceService.insert(absenceRequest.person, 
        absenceForm.groupSelected, absenceForm.from, absenceForm.to,
        absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected, 
        null, null, false, absenceManager);
    if (insertReport.criticalErrors.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager
            .getOrCreateAndPersistPersonDay(absenceRequest.person, absence.getAbsenceDate());
        absence.personDay = personDay;
        if (absenceForm.justifiedTypeSelected.name.equals(JustifiedTypeName.recover_time)) {

          absence = absenceManager.handleRecoveryAbsence(absence, absenceRequest.person, null);
        }        
        personDay.absences.add(absence);
        absence.save();
        personDay.save();

        notificationManager.notificationAbsencePolicy(Security.getUser().get(), 
            absence, groupAbsenceType, true, false, false);
      }
      if (!insertReport.reperibilityShiftDate().isEmpty()) {
        absenceManager.sendReperibilityShiftEmail(absenceRequest.person, 
            insertReport.reperibilityShiftDate());
        log.info("Inserite assenze con reperibilità e turni {} {}. Le email sono disabilitate.",
            absenceRequest.person.fullName(), insertReport.reperibilityShiftDate());
      }
      JPA.em().flush();
      consistencyManager.updatePersonSituation(absenceRequest.person.id, absenceForm.from);
      
    }

    return insertReport;
  }

  /**
   * Approvazione richiesta assenza da parte del responsabile di gruppo.
   * @param id id della richiesta di assenza.
   */
  public void managerApproval(long id, User user) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        absenceRequest, currentPerson, 
        AbsenceRequestEventType.MANAGER_APPROVAL, Optional.absent());
    log.info("{} approvata dal responsabile di gruppo {}.",
        absenceRequest, currentPerson.getFullname());
    
    notificationManager.notificationAbsenceRequestPolicy(user, absenceRequest, true);
  }

  /**
   * Approvazione richiesta assenza da parte del responsabile di sede.
   * @param id id della richiesta di assenza.
   */
  public void officeHeadApproval(long id, User user) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    if (absenceRequest.managerApprovalRequired && absenceRequest.managerApproved == null) {
      executeEvent(absenceRequest, currentPerson, 
          AbsenceRequestEventType.MANAGER_APPROVAL, Optional.absent());
      log.info("{} approvata dal responsabile di sede {} nelle veci del responsabile di gruppo.",
          absenceRequest, currentPerson.getFullname());
    }
    executeEvent(
        absenceRequest, currentPerson, 
        AbsenceRequestEventType.OFFICE_HEAD_APPROVAL, Optional.absent());
    log.info("{} approvata dal responsabile di sede {}.",
        absenceRequest, currentPerson.getFullname());   
    notificationManager.notificationAbsenceRequestPolicy(user, absenceRequest, true);

  }

  /**
   * Approvazione della richiesta di assenza da parte dell'amministratore del personale.
   * @param id l'id della richiesta di assenza.
   */
  public void personnelAdministratorApproval(long id, User user) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        absenceRequest, currentPerson, 
        AbsenceRequestEventType.ADMINISTRATIVE_APPROVAL, Optional.absent());
    log.info("{} approvata dall'amministratore del personale {}.",
        absenceRequest, currentPerson.getFullname());   
    notificationManager.notificationAbsenceRequestPolicy(user, absenceRequest, true);

  }

  /**
   * Metodo che permette la disapprovazione della richiesta.
   * @param id l'identificativo della richiesta di assenza
   */
  public void managerDisapproval(long id, String reason) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        absenceRequest, currentPerson, 
        AbsenceRequestEventType.MANAGER_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di gruppo {}.",
        absenceRequest, currentPerson.getFullname());

  }

  /**
   * Approvazione richiesta assenza da parte del responsabile di sede.
   * @param id id della richiesta di assenza.
   */
  public void officeHeadDisapproval(long id, String reason) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        absenceRequest, currentPerson, 
        AbsenceRequestEventType.OFFICE_HEAD_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.",
        absenceRequest, currentPerson.getFullname());   

  }

  /**
   * Approvazione della richiesta di assenza da parte dell'amministratore del personale.
   * @param id l'id della richiesta di assenza.
   */
  public void personnelAdministratorDisapproval(long id, String reason) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        absenceRequest, currentPerson, 
        AbsenceRequestEventType.ADMINISTRATIVE_REFUSAL, Optional.fromNullable(reason));
    log.info("{} disapprovata dall'amministratore del personale {}.",
        absenceRequest, currentPerson.getFullname());
  }

  /**
   * Approvazione della richiesta d'assenza da parte del manager per se stesso in 
   *    caso di approvazione senza passare dal responsabile di sede.
   * @param id l'id della richiesta d'assenza
   * @param user l'utente che sta provando l'approvazione della richiesta
   */
  public void managerSelfApproval(long id, User user) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(
        absenceRequest, currentPerson, 
        AbsenceRequestEventType.COMPLETE, Optional.absent());
    log.info("{} auto approvata dal responsabile del gruppo {}.",
        absenceRequest, currentPerson.getFullname());
  }
  
  /**
   * Metodo che ritorna il gruppo di assenze per inoltrare la richiesta.
   * @param absenceRequest la richiesta d'assenza
   * @return il gruppo di assenza corretto rispetto alla richiesta di assenza.
   */
  public GroupAbsenceType getGroupAbsenceType(AbsenceRequest absenceRequest) {
    Optional<GroupAbsenceType> group = null;

    GroupAbsenceType groupAbsenceType = null;
    switch (absenceRequest.type) {
      case VACATION_REQUEST:
        group = absenceDao.groupAbsenceTypeByName(FERIE_CNR);
        if (group.isPresent()) {
          groupAbsenceType = group.get();
        }        
        break;
      case COMPENSATORY_REST:
        group = absenceDao.groupAbsenceTypeByName(RIPOSI_CNR);
        if (group.isPresent()) {
          groupAbsenceType = group.get();
        }
        break;
      default: 
        log.error("Caso {} di richiesta non trattato", absenceRequest.type);
        break;
    }
    return groupAbsenceType;
  }
  
  /**
   * Verifica che non esistano richieste d'assenza con le stesse date già in process per 
   *     l'utente che ha fatto la richiesta.
   * @param absenceRequest la richiesta d'assenza da verificare
   * @return true se la richiesta d'assenza è ammissibile, false altrimenti.
   */
  public AbsenceRequest checkAbsenceRequest(AbsenceRequest absenceRequest) {
    List<AbsenceRequest> existingList = absenceRequestDao.existingAbsenceRequests(absenceRequest);
    for (AbsenceRequest ar : existingList) {
      DateInterval interval = new DateInterval(ar.startAt.toLocalDate(), ar.endTo.toLocalDate());
      if (DateUtility.isDateIntoInterval(absenceRequest.startAtAsDate(), interval)
          || DateUtility.isDateIntoInterval(absenceRequest.endToAsDate(), interval)
          || DateUtility.intervalIntersection(
              new DateInterval(absenceRequest.startAtAsDate(), 
                  absenceRequest.endToAsDate()), interval) != null) {
        return ar;
      } 
    }  
    return null;  
  }
  
}
