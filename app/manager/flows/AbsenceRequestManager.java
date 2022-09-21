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
import com.google.common.collect.Maps;
import controllers.Security;
import dao.AbsenceRequestDao;
import dao.GeneralSettingDao;
import dao.GroupDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import dao.absences.AbsenceComponentDao;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.NotificationManager;
import manager.NotificationManager.Crud;
import manager.PersonDayManager;
import manager.configurations.ConfigurationManager;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.model.DayInPeriod.TemplateRow;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShiftDay;
import models.Role;
import models.ShiftCategories;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import models.flows.AbsenceRequest;
import models.flows.AbsenceRequestEvent;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import org.apache.commons.compress.utils.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.db.jpa.JPA;

/**
 * Operazioni sulle richiesta di assenza.
 *
 * @author Cristian Lucchesi
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
  private PersonReperibilityDayDao personReperibilityDayDao;
  private PersonShiftDayDao personShiftDayDao;
  private GeneralSettingDao generalSettingDao;
  
  /**
   * DTO per la configurazione delle AbsenceRequest.
   */
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
      AbsenceService absenceService, AbsenceManager absenceManager, AbsenceComponentDao absenceDao,
      PersonDayManager personDayManager, ConsistencyManager consistencyManager,
      AbsenceRequestDao absenceRequestDao, GroupDao groupDao,
      PersonReperibilityDayDao personReperibilityDayDao, PersonShiftDayDao personShiftDayDao,
      GeneralSettingDao generalSettingDao) {
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
    this.personReperibilityDayDao = personReperibilityDayDao;
    this.personShiftDayDao = personShiftDayDao;
    this.generalSettingDao = generalSettingDao;
  }

  private static final String FERIE_CNR = "FERIE_CNR";
  private static final String RIPOSI_CNR = "RIPOSI_CNR";
  private static final String PERMESSI_PERSONALI = "G_661";
  private static final String FERIE_37 = "FERIE_CNR_PROROGA";

  /**
   * Verifica che gruppi ed eventuali responsabile di sede siano presenti per poter richiedere il
   * tipo di assenza.
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
    if (config.isAdministrativeApprovalRequired() && uroDao
        .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.PERSONNEL_ADMIN), person.office)
        .isEmpty()) {
      problems.add(String.format("Approvazione dell'amministratore del personale richiesta. "
          + "L'ufficio %s non ha impostato nessun amministratore del personale. "
          + "Contattare l'ufficio del personale.", person.office.getName()));
    }

    if (config.isManagerApprovalRequired()

        && groupDao.myGroups(person).isEmpty()) {
      problems.add(String.format(
          "Approvazione del responsabile di gruppo richiesta. "
              + "La persona %s non ha impostato nessun responsabile di gruppo "
              + "e non appartiene ad alcun gruppo. " + "Contattare l'ufficio del personale.",
              person.getFullname()));
    }

    if (config.isOfficeHeadApprovalRequired() && uroDao
        .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.SEAT_SUPERVISOR), person.office)
        .isEmpty()) {
      problems.add(String.format("Approvazione del responsabile di sede richiesta. "
          + "L'ufficio %s non ha impostato nessun responsabile di sede. "
          + "Contattare l'ufficio del personale.", person.office.getName()));
    }
    return problems;
  }

  /**
   * Verifica quali sono le approvazioni richiesta per questo tipo di assenza per questa persona.
   *
   * @param requestType il tipo di richiesta di assenza
   * @param person la persona.
   *
   * @return la configurazione con i tipi di approvazione necessari.
   */
  public AbsenceRequestConfiguration getConfiguration(AbsenceRequestType requestType,
      Person person) {
    val absenceRequestConfiguration = new AbsenceRequestConfiguration(person, requestType);
    
    //Per i livelli I-III in configurazione generale può esserci scritto di
    //non abilitare la possibilità di chiedere autorizzazioni, in questo caso i flussi 
    //dei livelli I-III non hanno mai approvazioni richieste.
    val skipTopLevelAuthorization = 
        person.isTopQualification() && requestType.canBeInsertedByTopLevelWithoutApproval
          && !generalSettingDao.generalSetting().enableAbsenceTopLevelAuthorization
        ;

    if (requestType.alwaysSkipAdministrativeApproval || skipTopLevelAuthorization) {
      absenceRequestConfiguration.administrativeApprovalRequired = false;
    } else {
      if (person.isTopQualification()
          && requestType.administrativeApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.administrativeApprovalRequired =
            (Boolean) configurationManager.configValue(person.office,
                requestType.administrativeApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification()
          && requestType.administrativeApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.administrativeApprovalRequired =
            (Boolean) configurationManager.configValue(person.office,
                requestType.administrativeApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }
    }

    if (requestType.alwaysSkipManagerApproval || person.isGroupManager() || 
        skipTopLevelAuthorization) {
      absenceRequestConfiguration.managerApprovalRequired = false;
    } else {
      if (person.isTopQualification() && requestType.managerApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.managerApprovalRequired =
            (Boolean) configurationManager.configValue(person.office,
                requestType.managerApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification()
          && requestType.managerApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.managerApprovalRequired =
            (Boolean) configurationManager.configValue(person.office,
                requestType.managerApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }
    }
    if (requestType.alwaysSkipOfficeHeadApproval || skipTopLevelAuthorization) {
      absenceRequestConfiguration.officeHeadApprovalRequired = false;
    } else {
      if (person.isTopQualification()
          && requestType.officeHeadApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.officeHeadApprovalRequired =
            (Boolean) configurationManager.configValue(person.office,
                requestType.officeHeadApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification()
          && requestType.officeHeadApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.officeHeadApprovalRequired =
            (Boolean) configurationManager.configValue(person.office,
                requestType.officeHeadApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }

    }
    if (requestType.alwaysSkipOfficeHeadApprovalForManager || skipTopLevelAuthorization) {
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
   * Imposta nella richiesta di assenza i tipi di approvazione necessari in funzione del tipo di
   * assenza e della configurazione specifica della sede del dipendente.
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
   * Rimuove tutte le eventuali approvazioni ed impostata il flusso come da avviare.
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
  public Optional<String> checkAbsenceRequestEvent(AbsenceRequest absenceRequest, Person approver,
      AbsenceRequestEventType eventType) {

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
      if (!uroDao
          .getUsersRolesOffices(absenceRequest.person.user,
              roleDao.getRoleByName(Role.PERSONNEL_ADMIN), absenceRequest.person.office)
          .isPresent()) {
        return Optional.of(String.format("L'evento %s non può essere eseguito da %s perché non ha"
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
      if (!uroDao.getUsersRolesOffices(approver.user, roleDao.getRoleByName(Role.SEAT_SUPERVISOR),
          absenceRequest.person.office).isPresent()) {
        return Optional.of(String.format("L'evento %s non può essere eseguito da %s perché non ha"
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
  public Optional<String> executeEvent(AbsenceRequest absenceRequest, Person person,
      AbsenceRequestEventType eventType, Optional<String> note) {

    val problem = checkAbsenceRequestEvent(absenceRequest, person, eventType);
    if (problem.isPresent()) {
      log.warn("Impossibile inserire la richiesta di assenza {}. Problema: {}", absenceRequest,
          problem.get());
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
        // si riparte dall'inizio del flusso.
        // resetFlow(absenceRequest);
        // Impostato a true per evitare di completare il flusso inserendo l'assenza
        absenceRequest.flowEnded = true;
        notificationManager.notificationAbsenceRequestRefused(absenceRequest, person);
        break;

      case ADMINISTRATIVE_APPROVAL:
        absenceRequest.administrativeApproved = LocalDateTime.now();
        break;

      case ADMINISTRATIVE_REFUSAL:
        // si riparte dall'inizio del flusso.
        // resetFlow(absenceRequest);
        // Impostato flowEnded a true per evitare di completare il flusso inserendo l'assenza
        absenceRequest.flowEnded = true;
        notificationManager.notificationAbsenceRequestRefused(absenceRequest, person);
        break;

      case OFFICE_HEAD_APPROVAL:
        absenceRequest.officeHeadApproved = LocalDateTime.now();
        break;

      case OFFICE_HEAD_REFUSAL:
        // si riparte dall'inizio del flusso.
        // resetFlow(absenceRequest);
        // Impostato flowEnded a true per evitare di completare il flusso inserendo l'assenza
        absenceRequest.flowEnded = true;
        notificationManager.notificationAbsenceRequestRefused(absenceRequest, person);
        break;

      case COMPLETE:
        absenceRequest.managerApproved = LocalDateTime.now();
        break;

      case DELETE:
        // Impostato flowEnded a true per evitare di completare il flusso inserendo l'assenza
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

    val event = AbsenceRequestEvent.builder().absenceRequest(absenceRequest).owner(person.user)
        .eventType(eventType).description(note.orNull()).build();
    event.save();

    log.info("Costruito evento per richiesta di assenza {}", event);
    absenceRequest.save();
    checkAndCompleteFlow(absenceRequest);
    return Optional.absent();
  }


  /**
   * Controlla se una richiesta di assenza può essere terminata con successo, in caso positivo
   * effettua l'inserimento delle assenze.
   *
   * @param absenceRequest la richiesta da verificare e da utilizzare per i dati dell'inserimento
   *        assenza.
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
   * @param absenceRequest la richiesta di assenza da cui prelevare i dati per l'inserimento.
   * @return il report con i codici di assenza inseriti.
   */
  private InsertReport completeFlow(AbsenceRequest absenceRequest) {

    absenceRequest.flowEnded = true;
    absenceRequest.save();
    log.debug("Flusso relativo a {} terminato. Inserimento in corso delle assenze.",
        absenceRequest);
    GroupAbsenceType groupAbsenceType = getGroupAbsenceType(absenceRequest);
    Integer hours = null;
    Integer minutes = null;
    AbsenceType absenceType = null;
    JustifiedType type = null;
    if (absenceRequest.type.equals(AbsenceRequestType.PERSONAL_PERMISSION)) {
      hours = absenceRequest.hours;
      minutes = absenceRequest.minutes;
      if (hours != null && minutes != null) {
        absenceType = absenceDao.absenceTypeByCode("661M").get();
        type = absenceDao
            .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);

      }
    }
    if (absenceRequest.type.equals(AbsenceRequestType.VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST)) {
      absenceType = absenceDao.absenceTypeByCode(DefaultAbsenceType.A_37.getCode()).get();
    }
    AbsenceForm absenceForm = absenceService.buildAbsenceForm(absenceRequest.person,
        absenceRequest.startAtAsDate(), null, absenceRequest.endToAsDate(), null, groupAbsenceType,
        false, absenceType, type, hours, minutes, false, true);
    InsertReport insertReport =
        absenceService.insert(absenceRequest.person, absenceForm.groupSelected, absenceForm.from,
            absenceForm.to, absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected,
            hours, minutes, false, absenceManager);
    if (insertReport.criticalErrors.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(absenceRequest.person,
            absence.getAbsenceDate());
        absence.personDay = personDay;
        if (absenceForm.justifiedTypeSelected.name.equals(JustifiedTypeName.recover_time)) {

          absence = absenceManager.handleRecoveryAbsence(absence, absenceRequest.person, null);
        }
        personDay.absences.add(absence);
        absence.save();
        personDay.save();

        notificationManager.notificationAbsencePolicy(Security.getUser().get(), absence,
            groupAbsenceType, true, false, false);
      }
      if (!insertReport.reperibilityShiftDate().isEmpty()) {
        absenceManager.sendReperibilityShiftEmail(absenceRequest.person,
            insertReport.reperibilityShiftDate());
        //TODO: aggiungere metodo che invia la stessa mail che mando al 
        //dipendente anche al responsabile
        // del turno o della reperibilità
        warnSupervisorAndManager(absenceRequest);

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
   *
   * @param id id della richiesta di assenza.
   */
  public void managerApproval(long id, User user) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(absenceRequest, currentPerson, AbsenceRequestEventType.MANAGER_APPROVAL,
        Optional.absent());
    log.info("{} approvata dal responsabile di gruppo {}.", absenceRequest,
        currentPerson.getFullname());

    notificationManager.notificationAbsenceRequestPolicy(user, absenceRequest, Crud.UPDATE);
  }

  /**
   * Approvazione richiesta assenza da parte del responsabile di sede.
   *
   * @param id id della richiesta di assenza.
   */
  public void officeHeadApproval(long id, User user) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    if (absenceRequest.managerApprovalRequired && absenceRequest.managerApproved == null) {
      executeEvent(absenceRequest, currentPerson, AbsenceRequestEventType.MANAGER_APPROVAL,
          Optional.absent());
      log.info("{} approvata dal responsabile di sede {} nelle veci del responsabile di gruppo.",
          absenceRequest, currentPerson.getFullname());
    }
    executeEvent(absenceRequest, currentPerson, AbsenceRequestEventType.OFFICE_HEAD_APPROVAL,
        Optional.absent());
    log.info("{} approvata dal responsabile di sede {}.", absenceRequest,
        currentPerson.getFullname());
    notificationManager.notificationAbsenceRequestPolicy(user, absenceRequest, Crud.UPDATE);

  }

  /**
   * Approvazione della richiesta di assenza da parte dell'amministratore del personale.
   *
   * @param id l'id della richiesta di assenza.
   */
  public void personnelAdministratorApproval(long id, User user) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(absenceRequest, currentPerson, AbsenceRequestEventType.ADMINISTRATIVE_APPROVAL,
        Optional.absent());
    log.info("{} approvata dall'amministratore del personale {}.", absenceRequest,
        currentPerson.getFullname());
    notificationManager.notificationAbsenceRequestPolicy(user, absenceRequest, Crud.UPDATE);

  }

  /**
   * Metodo che permette la disapprovazione della richiesta.
   *
   * @param id l'identificativo della richiesta di assenza
   */
  public void managerDisapproval(long id, String reason) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(absenceRequest, currentPerson, AbsenceRequestEventType.MANAGER_REFUSAL,
        Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di gruppo {}.", absenceRequest,
        currentPerson.getFullname());

  }

  /**
   * Approvazione richiesta assenza da parte del responsabile di sede.
   *
   * @param id id della richiesta di assenza.
   */
  public void officeHeadDisapproval(long id, String reason) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(absenceRequest, currentPerson, AbsenceRequestEventType.OFFICE_HEAD_REFUSAL,
        Optional.fromNullable(reason));
    log.info("{} disapprovata dal responsabile di sede {}.", absenceRequest,
        currentPerson.getFullname());

  }

  /**
   * Approvazione della richiesta di assenza da parte dell'amministratore del personale.
   *
   * @param id l'id della richiesta di assenza.
   */
  public void personnelAdministratorDisapproval(long id, String reason) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(absenceRequest, currentPerson, AbsenceRequestEventType.ADMINISTRATIVE_REFUSAL,
        Optional.fromNullable(reason));
    log.info("{} disapprovata dall'amministratore del personale {}.", absenceRequest,
        currentPerson.getFullname());
  }

  /**
   * Esegue l'approvazione del flusso controllando i vari casi possibili.
   *
   * @param absenceRequest id della richiesta di assenza
   * @param user l'utente che sta approvando il flusso
   * @return true se il flusso è stato approvato correttamente, false altrimenti
   */
  public boolean approval(AbsenceRequest absenceRequest, User user) {

    // verifico se posso inserire l'assenza
    if (!absenceRequest.officeHeadApprovalForManagerRequired && user.hasRoles(Role.GROUP_MANAGER)
        && absenceRequest.person.equals(user.person)) {
      managerSelfApproval(absenceRequest.id, user);
      return true;
    }
    if (absenceRequest.managerApprovalRequired && absenceRequest.managerApproved == null
        && user.hasRoles(Role.GROUP_MANAGER)) {
      // caso di approvazione da parte del responsabile di gruppo.
      managerApproval(absenceRequest.id, user);
      if (user.usersRolesOffices.stream()
          .anyMatch(uro -> uro.role.name.equals(Role.SEAT_SUPERVISOR))
          && absenceRequest.officeHeadApprovalRequired) {
        // se il responsabile di gruppo è anche responsabile di sede faccio un'unica approvazione
        officeHeadApproval(absenceRequest.id, user);
      }
      return true;
    }
    if (absenceRequest.administrativeApprovalRequired
        && absenceRequest.administrativeApproved == null && user.hasRoles(Role.PERSONNEL_ADMIN)) {
      // caso di approvazione da parte dell'amministratore del personale
      personnelAdministratorApproval(absenceRequest.id, user);
      return true;
    }
    if (absenceRequest.officeHeadApprovalForManagerRequired
        && absenceRequest.officeHeadApproved == null && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      officeHeadApproval(absenceRequest.id, user);
      return true;
    }
    if (!absenceRequest.isFullyApproved() && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      // caso di approvazione da parte del responsabile di sede
      officeHeadApproval(absenceRequest.id, user);
      return true;
    }
    return false;
  }

  /**
   * Approvazione della richiesta d'assenza da parte del manager per se stesso in caso di
   * approvazione senza passare dal responsabile di sede.
   *
   * @param id l'id della richiesta d'assenza
   * @param user l'utente che sta provando l'approvazione della richiesta
   */
  public void managerSelfApproval(long id, User user) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    val currentPerson = Security.getUser().get().person;
    executeEvent(absenceRequest, currentPerson, AbsenceRequestEventType.COMPLETE,
        Optional.absent());
    log.info("{} auto approvata dal responsabile del gruppo {}.", absenceRequest,
        currentPerson.getFullname());
  }
  
  /**
   * Da configurazione è possibile fare in modo che le richieste di assenza
   * dei livelli I-III siano automaticamente approvate.
   */
  public void topLevelSelfApproval(AbsenceRequest absenceRequest, Person person) {
    executeEvent(absenceRequest, person, AbsenceRequestEventType.COMPLETE,
        Optional.absent());
    log.info("{} auto approvata dal dipendente (livello I-III) ", absenceRequest,
        person.getFullname());
  }

  /**
   * Metodo che ritorna il gruppo di assenze per inoltrare la richiesta.
   *
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
      case PERSONAL_PERMISSION:
        group = absenceDao.groupAbsenceTypeByName(PERMESSI_PERSONALI);
        if (group.isPresent()) {
          groupAbsenceType = group.get();
        }
        break;
      case VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST:
        group = absenceDao.groupAbsenceTypeByName(FERIE_37);
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
   * Verifica che non esistano richieste d'assenza con le stesse date già in process per l'utente
   * che ha fatto la richiesta.
   *
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
              new DateInterval(absenceRequest.startAtAsDate(), absenceRequest.endToAsDate()),
              interval) != null) {
        return ar;
      }
    }
    return null;
  }

  /**
   * true se la persona è in turno/reperibilità nella data, false altrimenti.
   *
   * @param person la persona da cercare
   * @param date la data su cui verificare la presenza di turno/reperibilità
   * @return true se la persona è in turno/reperibilità nella data, false altrimenti.
   */
  private boolean checkIfAbsenceInShiftOrReperibility(Person person, LocalDate date) {
    // controllo se la persona è in reperibilità
    Optional<PersonReperibilityDay> prd =
        personReperibilityDayDao.getPersonReperibilityDay(person, date);
    // controllo se la persona è in turno
    Optional<PersonShiftDay> psd = personShiftDayDao.getPersonShiftDay(person, date);
    return psd.isPresent() || prd.isPresent();
  }

  /**
   * La lista di date in cui è presente una reperibilità o un turno e su cui si vuole
   * inserire un'assenza.
   *
   * @param absenceRequest la richiesta di assenza
   * @return la lista di date in cui è presente una reperibilità o un turno e su cui si vuole
   *         inserire un'assenza.
   */
  public List<LocalDate> getTroubleDays(AbsenceRequest absenceRequest) {
    LocalDate temp = absenceRequest.startAtAsDate();
    Map<LocalDate, Boolean> map = Maps.newHashMap();
    List<LocalDate> troubleDays = Lists.newArrayList();
    while (!temp.isAfter(absenceRequest.endToAsDate())) {
      if (checkIfAbsenceInShiftOrReperibility(absenceRequest.person, temp)) {
        map.put(temp, Boolean.TRUE);
      } else {
        map.put(temp, Boolean.FALSE);
      }
      temp = temp.plusDays(1);
    }
    for (Map.Entry<LocalDate, Boolean> entry : map.entrySet()) {
      if (true == entry.getValue().booleanValue()) {
        troubleDays.add(entry.getKey());
      }
    }
    return troubleDays;
  }

  /**
   * la stringa da inserire nelle note della richiesta di assenza contenente le date in cui,
   * inserendo l'assenza, si troverebbero giorni di reperibilità o di turno.
   *
   * @param troubleDays la lista di date che generano problemi tra turni/reperibilità e assenze
   * @return la stringa da inserire nelle note della richiesta di assenza contenente le date in cui,
   *         inserendo l'assenza, si troverebbero giorni di reperibilità o di turno.
   */
  public String generateNoteForShiftOrReperibility(List<LocalDate> troubleDays) {
    DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYYY");
    String string = "Evidenziati giorni con reperibilità/turno nelle date di: ";
    for (LocalDate date : troubleDays) {
      string = string + date.toString(dtf) + " ";
    }
    return string;
  }

  /**
   * Metodo void che controlla i giorni in cui la richiesta d'assenza matcha con i giorni di
   * reperibilità e/o turno del dipendente e informa il responsabile e i gestori 
   * del servizio via mail.
   *
   * @param absenceRequest la richiesta d'assenza
   */
  public void warnSupervisorAndManager(AbsenceRequest absenceRequest) {

    LocalDate temp = absenceRequest.startAtAsDate();
    Map<PersonReperibilityType, List<LocalDate>> repMap = Maps.newHashMap();
    Map<ShiftCategories, List<LocalDate>> shiftMap = Maps.newHashMap();
    
    //splitto le date incriminate tra reperibilità e turno
    while (!temp.isAfter(absenceRequest.endToAsDate())) {
      Optional<PersonReperibilityDay> prd =
          personReperibilityDayDao.getPersonReperibilityDay(absenceRequest.person, temp);
      if (prd.isPresent()) {        
        List<LocalDate> list = repMap.get(prd.get().reperibilityType);
        if (list == null) {
          list = Lists.newArrayList();
        }
        list.add(temp);
        repMap.put(prd.get().reperibilityType, list);        
      }

      Optional<PersonShiftDay> psd =
          personShiftDayDao.getPersonShiftDay(absenceRequest.person, temp);
      if (psd.isPresent()) {
        List<LocalDate> list = shiftMap.get(psd.get().shiftType.shiftCategories);
        if (list == null) {
          list = Lists.newArrayList();
        }
        list.add(temp);
        shiftMap.put(psd.get().shiftType.shiftCategories, list);        
      }
      temp = temp.plusDays(1);
    }
    /*
     *  per ogni giorno incriminato informo il responsabile e i gestori del servizio 
     *  di reperibilità
     */
    for (Map.Entry<PersonReperibilityType, List<LocalDate>> entry : repMap.entrySet()) {
      
      notificationManager.sendEmailToSupervisorOrManager(absenceRequest,
          entry.getKey().supervisor, Optional.<ShiftCategories>absent(),
          Optional.fromNullable(entry.getKey()), entry.getValue());
      for (Person manager : entry.getKey().managers) {
        notificationManager.sendEmailToSupervisorOrManager(absenceRequest, manager,
            Optional.<ShiftCategories>absent(), Optional.fromNullable(entry.getKey()),
            entry.getValue());
      }
    }    
    /*
     *  per ogni giorno incriminato informo il responsabile e i gestori del servizio
     *  di turno
     */
    for (Map.Entry<ShiftCategories, List<LocalDate>> entry : shiftMap.entrySet()) {
      notificationManager.sendEmailToSupervisorOrManager(absenceRequest,
          entry.getKey().supervisor,
          Optional.fromNullable(entry.getKey()),
          Optional.<PersonReperibilityType>absent(), entry.getValue());
      for (Person manager : entry.getKey().managers) {
        notificationManager.sendEmailToSupervisorOrManager(absenceRequest, manager,
            Optional.fromNullable(entry.getKey()),
            Optional.<PersonReperibilityType>absent(), entry.getValue());
      }
    }
  }

  /**
   * Metodo di utilità che corregge le date nella richiesta di assenza.
   * L'absenceRequest viene corretta se la richiesta contiene una data di fine che è
   * successiva alla data massima inseribile nella richiesta per via delle assenze disponibili.
   *
   * @param absenceRequest la richiesta di assenza 
   * @param insertReport il report derivante dai parametri di richiesta di assenza
   */
  public void checkAbsenceRequestDates(AbsenceRequest absenceRequest, 
      InsertReport insertReport) {
    LocalDate checkDate = null;
    for (TemplateRow row : insertReport.insertTemplateRows) {
      checkDate = row.date;
    }
    if (checkDate != null && absenceRequest.endTo.toLocalDate().isAfter(checkDate)) {
      absenceRequest.endTo = checkDate.toLocalDateTime(new LocalTime(0, 0, 0));
    }
  }

}