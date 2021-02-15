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

package manager;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.inject.Inject;
import dao.AbsenceDao;
import dao.GroupDao;
import dao.RoleDao;
import dao.absences.AbsenceComponentDao;
import helpers.TemplateExtensions;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Competence;
import models.Notification;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityType;
import models.Role;
import models.ShiftCategories;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.absences.Absence;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.definitions.DefaultGroup;
import models.enumerate.AccountRole;
import models.enumerate.NotificationSubject;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.CompetenceRequestType;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.Play;
import play.i18n.Messages;
import play.libs.Mail;

/**
 * Genera le notifiche da inviare agl utenti.
 * 
 * @author Daniele Murgia
 * @since 23/06/16
 */
@Slf4j
public class NotificationManager {

  private SecureManager secureManager;
  private RoleDao roleDao;
  private AbsenceDao absenceDao;
  private AbsenceComponentDao componentDao;
  private GroupDao groupDao;
  private ConfigurationManager configurationManager;


  /**
   * Default constructor.
   */
  @Inject
  public NotificationManager(SecureManager secureManager, RoleDao roleDao, AbsenceDao absenceDao,
      AbsenceComponentDao componentDao, GroupDao groupDao,
      ConfigurationManager configurationManager) {
    this.secureManager = secureManager;
    this.roleDao = roleDao;
    this.absenceDao = absenceDao;
    this.componentDao = componentDao;
    this.groupDao = groupDao;
    this.configurationManager = configurationManager;

  }

  private static final String WORKDAY_REPERIBILITY = "207";
  private static final String HOLIDAY_REPERIBILITY = "208";
  private static final String DTF = "dd/MM/YYYY - HH:mm";
  private static final String DF = "dd/MM/YYYY";

  private static final String BASE_URL = Play.configuration.getProperty("application.baseUrl");
  private static final String PATH = "absencerequests/show";
  private static final String COMPETENCE_PATH = "competencerequests/show";


  /**
   * Tipi di operazioni sulle entity.
   *
   * @author cristian
   *
   */
  public enum Crud {
    CREATE, READ, UPDATE, DELETE
  }

  /**
   * Gestore delle notifiche per le timbrature.
   */
  private void notifyStamping(Stamping stamping, Crud operation) {
    Verify.verifyNotNull(stamping);
    final Person person = stamping.personDay.person;
    final String template;
    if (Crud.CREATE == operation) {
      template = "%s ha inserito una nuova timbratura: %s di tipo %s "
          + "nel luogo %s con motivazione %s";
    } else if (Crud.UPDATE == operation) {
      template = "%s ha modificato una timbratura: %s";
    } else if (Crud.DELETE == operation) {
      template = "%s ha eliminato una timbratura: %s";
    } else {
      template = null;
    }
    String verso = "";
    if (stamping.way.equals(WayType.in)) {
      verso = "ingresso";
    } else {
      verso = "uscita";
    }
    final String message = String.format(template, person.fullName(), stamping.date.toString(DTF), 
        verso, stamping.place, stamping.reason);

    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN)
        || uro.role.name.equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.user).forEach(user -> {
          if (operation != Crud.DELETE) {
            Notification.builder().destination(user).message(message)
            .subject(NotificationSubject.STAMPING, stamping.id).create();
          } else {
            // per la notifica delle delete niente redirect altrimenti tocca
            // andare a prelevare l'entity dallo storico
            Notification.builder().destination(user).message(message)
            .subject(NotificationSubject.STAMPING).create();
          }
        });
  }

  /**
   * Gestore delle notifiche per le assenze.
   */
  private void notifyAbsence(Absence absence, GroupAbsenceType groupAbsenceType, 
      User currentUser, Crud operation) {
    Verify.verifyNotNull(absence);
    final Person person = absence.personDay.person;
    String template;
    if (Crud.CREATE == operation) {
      template = "%s ha inserito una nuova assenza: %s - %s";
    } else if (Crud.UPDATE == operation) {
      template = "%s ha modificato un'assenza: %s - %s";
    } else if (Crud.DELETE == operation) {
      template = "%s ha eliminato un'assenza: %s - %s";
    } else {
      template = null;
    }
    String modifier = "";
    if (currentUser.roles.contains(AccountRole.MISSIONS_MANAGER)) {
      modifier = currentUser.username;
      template = template + String.format(" di %s", person.fullName());
    } else {
      modifier = person.fullName();
    }

    final String message = String.format(template, modifier, absence.personDay.date.toString(DF),
        absence.absenceType.code);
    // controllare se dalla configurazione è possibile notificare le assenze da flusso
    val config = configurationManager.configValue(person.office, EpasParam.SEND_FLOWS_NOTIFICATION,
        LocalDate.now());
    if (config.equals(Boolean.FALSE)) {
      return;
    }
    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN)
          || uro.role.name.equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.user).forEach(user -> {
          Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.ABSENCE, absence.id).create();
        });
    /*
     * Verifico se si tratta di un 661 e invio la mail al responsabile di gruppo se esiste...
     */
    if (groupAbsenceType.name.equals(DefaultGroup.G_661.name())) {
      val sendManagerNotification = configurationManager.configValue(person.office, 
          EpasParam.SEND_MANAGER_NOTIFICATION_FOR_661, LocalDate.now());
      if (sendManagerNotification.equals(Boolean.TRUE) 
          && !groupDao.myGroups(absence.personDay.person).isEmpty()) {
        log.debug("Invio la notifica anche al responsabile di gruppo...");
        groupDao.myGroups(absence.personDay.person).stream().map(p -> p.manager).forEach(m -> {
          //Mandare una mail solo nel caso del codice giornaliero o ad ore e minuti 
          //(non l'assenza oraria)
          if (absence.getJustifiedMinutes() != null) {
            sendEmailToManagerFor661(m, absence);
          }          
        });
      } else {
        log.debug("Non invio mail al responsabile di gruppo perchè non presente "
            + "o non attivato da configurazione");
      }
    }

  }

  /**
   * Il metodo che si occupa di generare la corretta notifica al giusto utente.
   *
   * @param absenceRequest la richiesta di assenza da notificare
   * @param operation l'operazione da notificare
   */
  private void notifyAbsenceRequest(AbsenceRequest absenceRequest, Crud operation) {
    Verify.verifyNotNull(absenceRequest);
    final Person person = absenceRequest.person;
    final String template;
    if (Crud.CREATE == operation) {
      template = "%s ha inserito una nuova richiesta di assenza: %s";
    } else if (Crud.UPDATE == operation) {
      template = "%s ha modificato una richiesta di assenza: %s";
    } else if (Crud.DELETE == operation) {
      template = "%s ha eliminato una richiesta di assenza: %s";
    } else {
      template = null;
    }
    final String message =
        String.format(template, person.fullName(), absenceRequest.startAt.toString(DF));

    // se il flusso è terminato notifico a chi ha fatto la richiesta...
    if (absenceRequest.isFullyApproved()) {
      Notification.builder().destination(person.user).message(message)
      .subject(NotificationSubject.ABSENCE_REQUEST, absenceRequest.id).create();
      // ...e all'amministratore del personale
      List<Absence> absence =
          absenceDao.findByPersonAndDate(absenceRequest.person, absenceRequest.startAtAsDate(),
              Optional.of(absenceRequest.endToAsDate()), Optional.absent()).list();
      GroupAbsenceType groupAbsenceType = null;
      if (absenceRequest.type == AbsenceRequestType.COMPENSATORY_REST) {
        groupAbsenceType =
            componentDao.groupAbsenceTypeByName(DefaultGroup.RIPOSI_CNR_DIPENDENTI.name()).get();
      } else if (absenceRequest.type == AbsenceRequestType.VACATION_REQUEST) {
        groupAbsenceType =
            componentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR_DIPENDENTI.name()).get();
      } else {
        groupAbsenceType =
            componentDao.groupAbsenceTypeByName(DefaultGroup.G_661.name()).get();
      }
      if (!absence.isEmpty()) {
        notificationAbsencePolicy(person.user, absence.get(0), groupAbsenceType, true, false,
            false);
      } else {
        log.warn("Nessuna assenza inserita e da notificare per la richiesta di assenza {}",
            absenceRequest);
        return;
      }

    }
    final Role roleDestination = getProperRole(absenceRequest);
    if (roleDestination == null) {
      log.info(
          "Non si è trovato il ruolo a cui inviare la notifica per la richiesta d'assenza di "
              + "{} di tipo {} con date {}, {}",
              absenceRequest.person, absenceRequest.type, absenceRequest.startAt, 
              absenceRequest.endTo);
      return;
    }
    List<User> users =
        person.office.usersRolesOffices.stream().filter(uro -> uro.role.equals(roleDestination))
        .map(uro -> uro.user).collect(Collectors.toList());
    if (roleDestination.name.equals(Role.GROUP_MANAGER)) {
      log.info("Notifica al responsabile di gruppo per {}", absenceRequest);
      List<Group> groups = 
          groupDao.groupsByOffice(person.office, Optional.absent(), Optional.of(false));
      log.debug("Gruppi da controllare {}", groups);
      for (User user : users) {
        for (Group group : groups) {
          if (group.manager.equals(user.person) && group.getPeople().contains(person)) {
            Notification.builder().destination(user).message(message)
            .subject(NotificationSubject.ABSENCE_REQUEST, absenceRequest.id).create();
          }
        }
      }
      return;
    } else {
      users.forEach(user -> {
        Notification.builder().destination(user).message(message)
        .subject(NotificationSubject.ABSENCE_REQUEST, absenceRequest.id).create();
      });
    }

  }

  /**
   * Metodo privato che ritorna il ruolo a cui inviare la notifica della richiesta d'assenza.
   *
   * @param absenceRequest la richiesta d'assenza
   * @return il ruolo a cui inviare la notifica della richiesta di assenza.
   */
  private Role getProperRole(AbsenceRequest absenceRequest) {
    Role role = null;

    if (absenceRequest.managerApprovalRequired && absenceRequest.managerApproved == null) {
      role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    }
    if (absenceRequest.administrativeApprovalRequired
        && absenceRequest.administrativeApproved == null
        && (absenceRequest.managerApproved != null || !absenceRequest.managerApprovalRequired)) {
      role = roleDao.getRoleByName(Role.PERSONNEL_ADMIN);
    }
    if (absenceRequest.officeHeadApprovalRequired && absenceRequest.officeHeadApproved == null
        && ((!absenceRequest.managerApprovalRequired
            && !absenceRequest.administrativeApprovalRequired)
            || (absenceRequest.managerApproved != null
            && !absenceRequest.administrativeApprovalRequired)
            || (absenceRequest.managerApproved != null
            && absenceRequest.administrativeApproved != null)
            || (!absenceRequest.managerApprovalRequired
                && absenceRequest.administrativeApproved != null))) {
      role = roleDao.getRoleByName(Role.SEAT_SUPERVISOR);
    }
    if (absenceRequest.officeHeadApprovalForManagerRequired
        && absenceRequest.officeHeadApproved == null && absenceRequest.person.isGroupManager()) {
      role = roleDao.getRoleByName(Role.SEAT_SUPERVISOR);
    }
    return role;
  }

  /**
   * Le politiche di notifica inserimenti/modiche di timbrature.
   *
   * @param currentUser user che ha eseguito la richiesta
   * @param stamping la timbratura inserita
   */
  public void notificationStampingPolicy(User currentUser, Stamping stamping, boolean insert,
      boolean update, boolean delete) {

    // Se l'user che ha fatto l'inserimento è utente di sistema esco
    if (currentUser.isSystemUser() || currentUser.person == null) {
      return;
    }

    // Se l'user che ha fatto l'inserimento è amministratore di se stesso esco
    if (secureManager.officesWriteAllowed(currentUser).contains(currentUser.person.office)) {
      return;
    }

    // Se l'user che ha fatto l'inserimento è tecnologo e può autocertificare le timbrature esco
    if (currentUser.person.office.checkConf(EpasParam.TR_AUTOCERTIFICATION, "true")
        && currentUser.person.qualification.qualification <= 3) {
      return;
    }

    // negli altri casi notifica agli amministratori del personale ed al responsabile sede
    // controllo se il parametro di abilitazione alle notifiche è true
    val config = configurationManager.configValue(currentUser.person.office,
        EpasParam.SEND_ADMIN_NOTIFICATION, LocalDate.now());
    if (config.equals(Boolean.FALSE)) {
      return;
    }

    if (insert) {
      notifyStamping(stamping, NotificationManager.Crud.CREATE);
      return;
    }
    if (update) {
      notifyStamping(stamping, NotificationManager.Crud.UPDATE);
      return;
    }
    if (delete) {
      notifyStamping(stamping, NotificationManager.Crud.DELETE);
      return;
    }
  }

  /**
   * Le politiche di notifica riguardo l'inserimento di assenze.
   *
   * @param currentUser utente che esegue la richiesta
   * @param absence assenza inserita
   * @param groupAbsenceType gruppo di inserimento
   */
  public void notificationAbsencePolicy(User currentUser, Absence absence,
      GroupAbsenceType groupAbsenceType, boolean insert, boolean update, boolean delete) {

    // Se l'user che ha fatto l'inserimento è utente di sistema esco
    if (currentUser.isSystemUser() && !currentUser.roles.contains(AccountRole.MISSIONS_MANAGER)) {
      return;
    }

    // Se l'user che ha fatto l'inserimento è amministratore di se stesso esco
    if (currentUser.person != null
        && secureManager.officesWriteAllowed(currentUser).contains(currentUser.person.office)) {
      return;
    }

    if (groupAbsenceType.name.equals(DefaultGroup.FERIE_CNR_DIPENDENTI.name())
        || groupAbsenceType.name.equals(DefaultGroup.MISSIONE_GIORNALIERA.name())
        || groupAbsenceType.name.equals(DefaultGroup.MISSIONE_ORARIA.name())
        || groupAbsenceType.name.equals(DefaultGroup.RIPOSI_CNR_DIPENDENTI.name())
        || groupAbsenceType.name.equals(DefaultGroup.G_661.name())
        || groupAbsenceType.name.equals(DefaultGroup.FERIE_CNR_PROROGA.name())
        || groupAbsenceType.name.equals(DefaultGroup.LAVORO_FUORI_SEDE.name())) {
      if (insert) {
        notifyAbsence(absence, groupAbsenceType, currentUser, NotificationManager.Crud.CREATE);
        return;
      }
      if (update) {
        notifyAbsence(absence, groupAbsenceType, currentUser, NotificationManager.Crud.UPDATE);
        return;
      }
      if (delete) {
        notifyAbsence(absence, groupAbsenceType, currentUser, NotificationManager.Crud.DELETE);
        return;
      }

    }
  }

  /**
   * Notifica che una richiesta di assenza è stata rifiutata da uno degli approvatori del flusso.
   *
   * @param absenceRequest la richiesta di assenza
   * @param refuser la persona che ha rifiutato la richiesta di assenza.
   */
  public void notificationAbsenceRequestRefused(AbsenceRequest absenceRequest, Person refuser) {

    Verify.verifyNotNull(absenceRequest);
    Verify.verifyNotNull(refuser);

    final String message = String.format(
        "La richiesta di assenza di tipo \"%s\" dal %s al %s " + "è stata rifiutata da %s",
        TemplateExtensions.label(absenceRequest.type),
        absenceRequest.type.isAllDay() ? TemplateExtensions.format(absenceRequest.startAtAsDate())
            : TemplateExtensions.format(absenceRequest.startAt),
            absenceRequest.type.isAllDay() ? TemplateExtensions.format(absenceRequest.endToAsDate())
                : TemplateExtensions.format(absenceRequest.endTo),
                refuser.getFullname());

    Notification.builder().destination(absenceRequest.person.user).message(message)
    .subject(NotificationSubject.ABSENCE_REQUEST, absenceRequest.id).create();

  }

  /**
   * Notifica che una richiesta di assenza è stata approvata da uno degli approvatori del flusso.
   *
   * @param absenceRequest la richiesta di assenza
   * @param approver la persona che ha rifiutato la richiesta di assenza.
   */
  public void notificationAbsenceRequestApproved(AbsenceRequest absenceRequest, Person approver) {

    Verify.verifyNotNull(absenceRequest);
    Verify.verifyNotNull(approver);

    final String message = String.format(
        "La richiesta di assenza di tipo \"%s\" dal %s al %s " + "è stata accettata da %s",
        TemplateExtensions.label(absenceRequest.type),
        absenceRequest.type.isAllDay() ? TemplateExtensions.format(absenceRequest.startAtAsDate())
            : TemplateExtensions.format(absenceRequest.startAt),
            absenceRequest.type.isAllDay() ? TemplateExtensions.format(absenceRequest.endToAsDate())
                : TemplateExtensions.format(absenceRequest.endTo),
                approver.getFullname());

    Notification.builder().destination(absenceRequest.person.user).message(message)
    .subject(NotificationSubject.ABSENCE_REQUEST, absenceRequest.id).create();

  }

  /**
   * Gestore delle notifiche per le assenze inserite in seguito all'approvazione di un richiesta di
   * assenza.
   */
  public void notifyAbsenceOnAbsenceRequestCompleted(List<Absence> absences, Person person,
      Role role) {
    Verify.verify(!absences.isEmpty());
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(role);

    final StringBuffer message = new StringBuffer(
        String.format("Flusso di richiesta assenza terminato, inserita una nuova assenza per %s.",
            person.getFullname()));

    absences.forEach(a -> {
      message.append(String.format(" %s - %s.", a.absenceType.code, a.personDay.date.toString(DF)));
    });

    person.office.usersRolesOffices.stream().filter(uro -> uro.role.name.equals(role.name))
        .map(uro -> uro.user).forEach(user -> {
          Notification.builder().destination(user).message(message.toString())
          .subject(NotificationSubject.ABSENCE, absences.stream().findFirst().get().id)
          .create();
        });
  }

  /**
   * Il metodo che fa partire la notifica al giusto livello della catena.
   *
   * @param currentUser l'utente che fa la richiesta
   * @param absenceRequest la richiesta di assenza via flusso
   * @param insert se si tratta di inserimento (per ora unico caso contemplato)
   */
  public void notificationAbsenceRequestPolicy(User currentUser, AbsenceRequest absenceRequest,
      boolean insert) {
    if (currentUser.isSystemUser()) {
      return;
    }
    if (insert) {
      notifyAbsenceRequest(absenceRequest, Crud.CREATE);
      return;
    }
  }

  /**
   * Metodo pubblico che chiama l'invio delle email ai destinatari all'approvazione della richiesta
   * d'assenza.
   *
   * @param currentUser l'utente corrente che esegue la chiamata
   * @param absenceRequest la richiesta d'assenza da processare
   * @param insert se stiamo facendo un inserimento di una nuova richiesta d'assenza
   */
  public void sendEmailAbsenceRequestPolicy(User currentUser, AbsenceRequest absenceRequest,
      boolean insert) {
    if (currentUser.isSystemUser()) {
      return;
    }
    if (insert) {
      sendEmailAbsenceRequest(absenceRequest);
    }
  }

  /**
   * Metodo che invia la mail all'utente responsabile dell'approvazione.
   *
   * @param absenceRequest la richiesta d'assenza
   * @param currentUser l'utente a cui inviare la mail
   */
  private void sendEmailAbsenceRequest(AbsenceRequest absenceRequest) {

    Verify.verifyNotNull(absenceRequest);
    final Person person = absenceRequest.person;

    final Role roleDestination = getProperRole(absenceRequest);
    if (roleDestination == null) {
      log.warn(
          "Non si è trovato il ruolo a cui inviare la mail per la richiesta d'assenza di "
              + "{} di tipo {} con date {}, {}",
              absenceRequest.person, absenceRequest.type, absenceRequest.startAt, 
              absenceRequest.endTo);
      return;
    }
    person.office.usersRolesOffices.stream().filter(uro -> uro.role.equals(roleDestination))
        .map(uro -> uro.user).forEach(user -> {
          SimpleEmail simpleEmail = new SimpleEmail();
          // Per i responsabili di gruppo l'invio o meno dell'email è parametrizzato.
          if (roleDestination.name.equals(Role.GROUP_MANAGER)) {
            Optional<Group> group = groupDao.checkManagerPerson(user.person, person);
            if (!group.isPresent()) {
              return;
            }
            if (!group.get().sendFlowsEmail) {
              log.info("Non verrà inviata la mail al responsabile del gruppo {} "
                  + "poichè l'invio è stato disattivato.", user.person.fullName());
              return;
            }
          }
          try {
            simpleEmail.addTo(user.person.email);
          } catch (EmailException e) {
            e.printStackTrace();
          }
          simpleEmail.setSubject("ePas Approvazione flusso");
          val mailBody = createAbsenceRequestEmail(absenceRequest, user);
          try {
            simpleEmail.setMsg(mailBody);
          } catch (EmailException e) {
            e.printStackTrace();
          }
          Mail.send(simpleEmail);
          log.info(
              "Inviata email per richiesta di flusso richiesta: {}. "
                  + "Mail: \n\tTo: {}\n\tSubject: {}\n\tbody: {}",
                  absenceRequest, user.person.email, simpleEmail.getSubject(), mailBody);
        });
  }

  /**
   * Metodo privato che invia la mail al richiedente la ferie/riposo compensativo.
   *
   * @param absenceRequest la richiesta d'assenza
   */
  private void sendEmailAbsenceRequestConfirmation(AbsenceRequest absenceRequest) {
    Verify.verifyNotNull(absenceRequest);
    final Person person = absenceRequest.person;
    SimpleEmail simpleEmail = new SimpleEmail();
    try {
      simpleEmail.addTo(person.email);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    String requestType = "";
    if (absenceRequest.type == AbsenceRequestType.COMPENSATORY_REST) {
      requestType = Messages.get("AbsenceRequestType.COMPENSATORY_REST");
    } else if (absenceRequest.type == AbsenceRequestType.PERSONAL_PERMISSION) {
      requestType = Messages.get("AbsenceRequestType.PERSONAL_PERMISSION");
    } else {
      requestType = Messages.get("AbsenceRequestType.VACATION_REQUEST");
    }
    simpleEmail.setSubject("ePas Approvazione flusso");
    final StringBuilder message =
        new StringBuilder().append(String.format("Gentile %s,\r\n", person.fullName()));
    message.append(String.format("\r\nè stata approvata la sua richiesta di : %s", requestType));
    message.append(String.format("\r\n per i giorni %s - %s", absenceRequest.startAt.toLocalDate(),
        absenceRequest.endTo.toLocalDate()));
    val mailBody = message.toString();
    try {
      simpleEmail.setMsg(mailBody);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    Mail.send(simpleEmail);
    log.info(
        "Inviata email per approvazione di flusso richiesta: {}. "
            + "Mail: \n\tTo: {}\n\tSubject: {}\n\tbody: {}",
            absenceRequest, person.email, simpleEmail.getSubject(), mailBody);

  }

  /**
   * Metodo che compone il corpo della mail da inviare.
   *
   * @param absenceRequest la richiesta d'assenza
   * @param user l'utente a cui inviare la mail
   * @return il corpo della mail da inviare all'utente responsabile dell'approvazione.
   */
  private String createAbsenceRequestEmail(AbsenceRequest absenceRequest, User user) {
    final String dateFormatter = "dd/MM/YYYY";
    String requestType = "";
    if (absenceRequest.type == AbsenceRequestType.COMPENSATORY_REST) {
      requestType = Messages.get("AbsenceRequestType.COMPENSATORY_REST");
    } else if (absenceRequest.type == AbsenceRequestType.PERSONAL_PERMISSION) {
      requestType = Messages.get("AbsenceRequestType.PERSONAL_PERMISSION");
    } else {
      requestType = Messages.get("AbsenceRequestType.VACATION_REQUEST");
    }
    final StringBuilder message =
        new StringBuilder().append(String.format("Gentile %s,\r\n", user.person.fullName()));
    message.append(String.format("\r\nLe è stata notificata la richiesta di : %s",
        absenceRequest.person.fullName()));
    message.append(String.format("\r\n per una assenza di tipo: %s", requestType));
    if (absenceRequest.startAt.isEqual(absenceRequest.endTo)) {
      message.append(String.format("\r\n per il giorno: %s",
          absenceRequest.startAt.toLocalDate().toString(dateFormatter)));
    } else {
      message.append(String.format("\r\n dal: %s",
          absenceRequest.startAt.toLocalDate().toString(dateFormatter)));
      message.append(
          String.format("  al: %s", absenceRequest.endTo.toLocalDate().toString(dateFormatter)));
    }
    String baseUrl = BASE_URL;
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }

    baseUrl = baseUrl + PATH + "?id=" + absenceRequest.id + "&type=" + absenceRequest.type;

    message.append(String.format("\r\n Verifica cliccando sul link seguente: %s", baseUrl));

    return message.toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  // Notifiche per competence request
  //////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Notifica che una richiesta di competenza è stata rifiutata da uno degli 
   * approvatori del flusso.
   *
   * @param competenceRequest la richiesta di competenza
   * @param refuser la persona che ha rifiutato la richiesta di competenza.
   */
  public void notificationCompetenceRequestRefused(
      CompetenceRequest competenceRequest, Person refuser) {

    Verify.verifyNotNull(competenceRequest);
    Verify.verifyNotNull(refuser);

    final String message = 
        String.format("La richiesta di tipo \"%s\" per il %s "
            + "è stata rifiutata da %s",
            TemplateExtensions.label(competenceRequest.type),
            TemplateExtensions.format(competenceRequest.startAt),
            refuser.getFullname());

    Notification.builder().destination(competenceRequest.person.user).message(message)
    .subject(NotificationSubject.COMPETENCE_REQUEST, competenceRequest.id).create();

  }

  /**
   * Gestore delle notifiche per le competenze.
   */
  private void notifyCompetence(Competence competence, User currentUser, Crud operation) {
    Verify.verifyNotNull(competence);
    final Person person = competence.person;
    String template;
    if (Crud.CREATE == operation) {
      template = "%s ha inserito una nuova: %s - %s";
    } else if (Crud.UPDATE == operation) {
      template = "%s ha modificato una: %s - %s";
    } else if (Crud.DELETE == operation) {
      template = "%s ha eliminato una: %s - %s";
    } else {
      template = null;
    }
    String modifier = person.fullName();
    YearMonth yearMonth = new YearMonth(competence.year, competence.month);
    final String message = String.format(template, modifier,
        yearMonth.toString(DF), competence.competenceCode.code);
    //controllare se dalla configurazione è possibile notificare le competenze da flusso 
    val config = configurationManager
        .configValue(person.office, EpasParam.SEND_FLOWS_NOTIFICATION, LocalDate.now());
    if (config.equals(Boolean.FALSE)) {
      return;
    }
    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN) 
            || uro.role.name.equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.user).forEach(user -> {
          Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.COMPETENCE, competence.id).create();
        });

    //sendEmailAbsenceRequestConfirmation(absenceRequest);

  }


  /**
   * Metodo void che chiama il metodo privato che invia la mail al richiedente l'assenza
   * o la competenza.
   *
   * @param absenceRequest la richiesta d'assenza con tutti i parametri.
   * @param competenceRequest la richiesta di competenza con tutti i parametri.
   */
  public void sendEmailToUser(Optional<AbsenceRequest> absenceRequest, 
      Optional<CompetenceRequest> competenceRequest) {
    if (absenceRequest.isPresent()) {
      sendEmailAbsenceRequestConfirmation(absenceRequest.get());
    }
    if (competenceRequest.isPresent()) {
      sendEmailCompetenceRequestConfirmation(competenceRequest.get());
    }

  }

  private void sendEmailCompetenceRequestConfirmation(CompetenceRequest competenceRequest) {
    Verify.verifyNotNull(competenceRequest);
    final Person person = competenceRequest.person;
    SimpleEmail simpleEmail = new SimpleEmail();
    try {
      simpleEmail.addTo(person.email);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    String requestType = "";
    if (competenceRequest.type == CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST) {
      requestType = Messages.get("CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST");
    } 
    simpleEmail.setSubject("ePas Approvazione flusso");
    final StringBuilder message = new StringBuilder()
        .append(String.format("Gentile %s,\r\n", person.fullName()));
    message.append(String.format("\r\nè stata approvata la sua richiesta di : %s",
        requestType));
    message.append(String.format("\r\n per i giorni %s - %s", 
        competenceRequest.startAt.toLocalDate(), competenceRequest.endTo.toLocalDate()));
    val mailBody = message.toString();
    try {
      simpleEmail.setMsg(mailBody);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    Mail.send(simpleEmail);
    log.info("Inviata email per approvazione di flusso richiesta: {}. "
        + "Mail: \n\tTo: {}\n\tSubject: {}\n\tbody: {}", 
        competenceRequest, person.email, simpleEmail.getSubject(), mailBody);

  }

  /**
   * Metodo pubblico intermedio per chiamare il privato che fa la notifica.
   *
   * @param currentUser l'utente che fa la richiesta
   * @param competenceRequest la richiesta di competenza
   * @param insert se si sta facendo un inserimento
   */
  public void notificationCompetenceRequestPolicy(User currentUser, 
      CompetenceRequest competenceRequest,
      boolean insert) {
    if (currentUser.isSystemUser()) {
      return;
    }
    if (insert) {
      notifyCompetenceRequest(competenceRequest, Crud.CREATE);
      return;
    }    
  }

  /**
   * Metodo pubblico che fa da interfaccia per la chiamata al metodo privato che invia la mail.
   *
   * @param currentUser l'utente corrente
   * @param competenceRequest la richiesta di competenza
   * @param insert se si sta facendo un inserimento
   */
  public void sendEmailCompetenceRequestPolicy(User currentUser, 
      CompetenceRequest competenceRequest,
      boolean insert) {
    if (currentUser.isSystemUser()) {
      return;
    }
    if (insert) {
      sendEmailCompetenceRequest(competenceRequest);
    }    
  }

  private void sendEmailCompetenceRequest(CompetenceRequest competenceRequest) {
    Verify.verifyNotNull(competenceRequest);
    SimpleEmail simpleEmail = new SimpleEmail();
    final User userDestination = getProperUser(competenceRequest); 
    if (userDestination == null) {
      log.warn("Non si è trovato il ruolo a cui inviare la mail per la richiesta d'assenza di "
          + "{} di tipo {} con date {}, {}", 
          competenceRequest.person, competenceRequest.type, competenceRequest.startAt, 
          competenceRequest.endTo);
      return;
    }

    try {
      simpleEmail.addTo(userDestination.person.email);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    simpleEmail.setSubject("ePas Approvazione flusso");
    val mailBody = createCompetenceRequestEmail(competenceRequest, userDestination);
    try {
      simpleEmail.setMsg(mailBody);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    Mail.send(simpleEmail);
    log.info("Inviata email per richiesta di flusso richiesta: {}. "
        + "Mail: \n\tTo: {}\n\tSubject: {}\n\tbody: {}", 
        competenceRequest, userDestination.person.email, simpleEmail.getSubject(), mailBody);

  }

  private String createCompetenceRequestEmail(CompetenceRequest competenceRequest, User user) {
    final String dateFormatter = "dd/MM/YYYY";
    String requestType = "";
    if (competenceRequest.type == CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST) {
      requestType = Messages.get("CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST");
    }
    final StringBuilder message = new StringBuilder()
        .append(String.format("Gentile %s,\r\n", user.person.fullName()));
    message.append(String.format("\r\nLe è stata notificata la richiesta di : %s",
        competenceRequest.person.fullName()));
    message.append(String.format("\r\ndi tipo: %s", requestType));
    if (competenceRequest.beginDateToAsk.isEqual(competenceRequest.endDateToAsk)) {
      message.append(String.format("\r\nper il giorno: %s", 
          competenceRequest.beginDateToAsk.toString(dateFormatter)));
      message.append(String.format("\r\nin cambio del giorno: %s", 
          competenceRequest.beginDateToGive));
    } else {
      message.append(String.format("\r\n dal: %s", 
          competenceRequest.beginDateToAsk.toString(dateFormatter)));
      message.append(String.format("  al: %s", 
          competenceRequest.endDateToAsk.toString(dateFormatter)));
      message.append(String.format("\r\n in cambio dei giorni dal %s", 
          competenceRequest.beginDateToGive.toString(dateFormatter)));
      message.append(String.format("  al: %s", 
          competenceRequest.endDateToGive.toString(dateFormatter)));
    }
    message.append(String.format("\r\ncondestinatario: %s", competenceRequest.teamMate.fullName()));
    String baseUrl = BASE_URL;
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }

    baseUrl = baseUrl + COMPETENCE_PATH + "?id=" + competenceRequest.id 
        + "&type=" + competenceRequest.type;

    message.append(String.format("\r\n Verifica cliccando sul link seguente: %s", baseUrl));

    return message.toString();
  }

  /**
   * Metodo che ritorna il corretto ruolo da chiamare in base alla richiesta di competenza.
   *
   * @param competenceRequest la richiesta di competenza.
   * @return il ruolo corretto per l'approvazione della richiesta.
   */
  private User getProperUser(CompetenceRequest competenceRequest) {

    User user = null;
    if (competenceRequest.reperibilityManagerApprovalRequired 
        && competenceRequest.reperibilityManagerApproved == null) {
      for (PersonReperibility pr : competenceRequest.person.reperibility) {
        for (PersonReperibility tmPr : competenceRequest.teamMate.reperibility) {
          if (pr.personReperibilityType.equals(tmPr.personReperibilityType)) {
            user = pr.personReperibilityType.supervisor.user;
          }
        }
      }
      
    }

    if (competenceRequest.employeeApprovalRequired 
        && competenceRequest.employeeApproved == null) {
      user = competenceRequest.teamMate.user;
      
    }
    return user;
  }

  /**
   * Il metodo che si occupa di generare la corretta notifica al giusto utente.
   *
   * @param competenceRequest la richiesta da notificare
   * @param operation l'operazione da notificare
   */
  private void notifyCompetenceRequest(CompetenceRequest competenceRequest, Crud operation) {
    Verify.verifyNotNull(competenceRequest);
    final Person person = competenceRequest.person;
    final String template;
    String typeOfRequest = "";
    if (Crud.CREATE == operation) {
      template = "%s ha inserito una nuova richiesta di: %s";
    } else if (Crud.UPDATE == operation) {
      template = "%s ha modificato una richiesta di: %s";
    } else if (Crud.DELETE == operation) {
      template = "%s ha eliminato una richiesta di: %s";
    } else {
      template = null;
    }
    if (competenceRequest.type.equals(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST)) {
      typeOfRequest = "cambio reperibilità";
    }
    final String message = 
        String.format(template, person.fullName(), typeOfRequest);

    //se il flusso è terminato notifico a chi ha fatto la richiesta...
    if (competenceRequest.isFullyApproved()) {
      Notification.builder().destination(person.user).message(message)
      .subject(NotificationSubject.COMPETENCE_REQUEST, competenceRequest.id).create();

      if (competenceRequest.type.equals(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST)) {
        //TODO: verificare se abbia senso informare qualche altro ruolo del cambio di reperibilità
      }
    }
    final User userDestination = getProperUser(competenceRequest); 
    if (userDestination == null) {
      log.info("Non si è trovato l'utente a cui inviare la notifica per la richiesta di "
          + "{} di tipo {} con date {}, {}", 
          competenceRequest.person, competenceRequest.type, competenceRequest.beginDateToAsk, 
          competenceRequest.endDateToAsk);
      return;
    }

    Notification.builder().destination(userDestination).message(message)
    .subject(NotificationSubject.COMPETENCE_REQUEST, competenceRequest.id).create();
    return;


  }

  /**
   * Le politiche di notifica riguardo l'inserimento di competenze.
   *
   * @param currentUser utente che esegue la richiesta
   * @param competence competenza inserita
   */
  public void notificationCompetencePolicy(User currentUser, Competence competence, 
      boolean insert, boolean update, boolean delete) {

    //Se l'user che ha fatto l'inserimento è utente di sistema esco
    if (currentUser.isSystemUser() && !currentUser.roles.contains(AccountRole.MISSIONS_MANAGER)) {
      return;
    }

    //Se l'user che ha fatto l'inserimento è amministratore di se stesso esco
    if (currentUser.person != null 
        && secureManager.officesWriteAllowed(currentUser).contains(currentUser.person.office)) {
      return;
    }

    if (competence.competenceCode.code.equals(WORKDAY_REPERIBILITY)
        || competence.competenceCode.code.equals(HOLIDAY_REPERIBILITY)) {
      if (insert) {
        notifyCompetence(competence, currentUser, NotificationManager.Crud.CREATE);
        return;
      }
      if (update) {
        notifyCompetence(competence, currentUser, NotificationManager.Crud.UPDATE);
        return;
      }
      if (delete) {
        notifyCompetence(competence, currentUser, NotificationManager.Crud.DELETE);
        return;
      }

    }
  }


  /**
   * Chiama il metodo privato per l'invio della mail al responsabile del servizio per informarlo che
   * ci sono sovrapposizioni tra le date di reperibilità/turno e la richiesta di assenza.
   *
   * @param absenceRequest la richiesta d'assenza
   * @param shift l'eventuale servizio di turno
   * @param rep l'eventuale servizio di reperibilità
   * @param dates la lista di date incriminate
   */
  public void sendEmailToSupervisorOrManager(AbsenceRequest absenceRequest, Person receiver,
      Optional<ShiftCategories> shift, Optional<PersonReperibilityType> rep, 
      List<LocalDate> dates) {
    sendEmailToServiceSupervisorOrManager(absenceRequest, receiver, shift, rep, dates);
  }

  /**
   * Metodo privato che invia la mail al responsabile del servizio di turno/reperibilità per
   * informarlo della coincidenza della data in oggetto con quelle in cui il dipendente è in
   * turno/reperibilità.
   *
   * @param absenceRequest la richiesta d'assenza
   * @param shift se presente, il servizio di turno
   * @param rep se presente, il servizio di reperibilità
   * @param dates la lista di date incriminate
   */
  private void sendEmailToServiceSupervisorOrManager(AbsenceRequest absenceRequest, Person receiver,
      Optional<ShiftCategories> shift, Optional<PersonReperibilityType> rep, 
      List<LocalDate> dates) {
    Verify.verifyNotNull(absenceRequest);

    SimpleEmail simpleEmail = new SimpleEmail();
    try {
      simpleEmail.addTo(receiver.email);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    String service = "";
    String type = "";
    if (shift.isPresent()) {

      service = shift.get().description;
      type = "turno";
    }
    if (rep.isPresent()) {

      service = rep.get().description;
      type = "reperibilità";
    }
    String requestType = "";
    if (absenceRequest.type == AbsenceRequestType.COMPENSATORY_REST) {
      requestType = Messages.get("AbsenceRequestType.COMPENSATORY_REST");
    } else if (absenceRequest.type == AbsenceRequestType.PERSONAL_PERMISSION) {
      requestType = Messages.get("AbsenceRequestType.PERSONAL_PERMISSION");
    } else if (absenceRequest.type 
        == AbsenceRequestType.VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST) {
      requestType = Messages.get("AbsenceRequestType.VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST");
    } else {
      requestType = Messages.get("AbsenceRequestType.VACATION_REQUEST");
    }
    simpleEmail.setSubject("ePas Approvazione flusso");
    final StringBuilder message =
        new StringBuilder().append(String.format("Gentile %s,\r\n", receiver.getFullname()));
    message.append(String.format("\r\n è stata approvata la richiesta di : %s", requestType));
    message.append(String.format("\r\n per i giorni %s - %s", absenceRequest.startAt.toLocalDate(),
        absenceRequest.endTo.toLocalDate()));
    message.append(String.format("\r\n per il dipendente %s", absenceRequest.person.getFullname()));
    if (dates.size() == 1) {
      message
          .append(String.format("\r\n Nel giorno %s il dipendente risulta però in %s", 
              dates.get(0), type));
    } else {
      String datesToCheck = "";
      for (LocalDate date : dates) {
        datesToCheck = datesToCheck + date.toString() + " ";
      }
      message
          .append(String.format("\r\n Nei giorni %s il dipendente risulta però in %s", 
              datesToCheck, type));
    }

    message.append(String.format("\r\n per il servizio %s", service));
    message.append(String.format("\r\n Verificare la compatibilità delle date delle assenze con "
        + "la schedulazione del dipendente nel servizio"));
    val mailBody = message.toString();
    try {
      simpleEmail.setMsg(mailBody);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    Mail.send(simpleEmail);
    log.info("Inviata email al responsabile/gestore per informazione giorni di reperibilità/turno "
        + "concomitanti coi giorni di richiesta: {}. " 
        + "Mail: \n\tTo: {}\n\tSubject: {}\n\tbody: {}",
        absenceRequest, receiver.email, simpleEmail.getSubject(), mailBody);
  }

  /**
   * Metodo privato che invia al responsabile la mail di notifica di chiusura di un flusso
   * di richiesta per permesso personale di un dipendente appartenente al gruppo del 
   * responsabile.
   *
   * @param manager il responsabile destinatario della mail
   * @param absence l'assenza per permesso personale da notificare
   */
  private void sendEmailToManagerFor661(Person manager, Absence absence) {
    SimpleEmail simpleEmail = new SimpleEmail();
    try {
      simpleEmail.addTo(manager.email);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    String requestType = Messages.get("AbsenceRequestType.PERSONAL_PERMISSION");
    String justifiedTime = "";
    if (absence.justifiedType.name.equals(JustifiedType.JustifiedTypeName.all_day)) {
      justifiedTime = "tutto il giorno";
    } else {
      int hours = absence.getJustifiedMinutes() / 60;
      int minutes = absence.getJustifiedMinutes() % 60;
      justifiedTime = "" + hours + " ore e " + minutes + " minuti";
    }
    simpleEmail.setSubject("ePas Notifica terminazione flusso permesso personale");
    final StringBuilder message =
        new StringBuilder().append(String.format("Gentile %s,\r\n", manager.getFullname()));
    message.append(String.format("\r\n è stata approvata la richiesta di : %s", requestType));
    message.append(String.format("\r\n per il giorno %s", absence.date));
    message.append(String.format("\r\n che giustifica %s", justifiedTime));
    message.append(String.format("\r\n per il dipendente %s", 
        absence.getPersonDay().person.getFullname()));
    val mailBody = message.toString();
    try {
      simpleEmail.setMsg(mailBody);
    } catch (EmailException e) {
      e.printStackTrace();
    }
    Mail.send(simpleEmail);
    log.info("Inviata email al responsabile/gestore per informazione "
        + "chiusura flusso permesso personale");
  }
}
