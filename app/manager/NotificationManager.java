package manager;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.inject.Inject;
import dao.RoleDao;
import helpers.TemplateExtensions;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import manager.configurations.EpasParam;

import models.Notification;
import models.Person;
import models.Role;
import models.Stamping;
import models.User;
import models.absences.Absence;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import models.enumerate.AccountRole;
import models.enumerate.NotificationSubject;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestType;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import play.Play;
import play.i18n.Messages;
import play.libs.Mail;

/**
 * Genera le notifiche da inviare agl utenti.
 * 
 * @author daniele
 * @since 23/06/16.
 */
@Slf4j
public class NotificationManager {

  private SecureManager secureManager;
  private RoleDao roleDao;

  @Inject
  public NotificationManager(SecureManager secureManager, RoleDao roleDao) {
    this.secureManager = secureManager;
    this.roleDao = roleDao;
  }

  private static final String DTF = "dd/MM/YYYY - HH:mm";
  private static final String DF = "dd/MM/YYYY";

  private static final String BASE_URL = Play.configuration.getProperty("application.baseUrl");
  private static final String PATH = "absencerequest/show";


  /**
   * Tipi di operazioni sulle entity.
   * @author cristian
   *
   */
  public enum Crud {
    CREATE,
    READ,
    UPDATE,
    DELETE
  }

  /**
   * Gestore delle notifiche per le timbrature.
   */
  private void notifyStamping(Stamping stamping, Crud operation) {
    Verify.verifyNotNull(stamping);
    final Person person = stamping.personDay.person;
    final String template;
    if (Crud.CREATE == operation) {
      template = "%s ha inserito una nuova timbratura: %s";
    } else if (Crud.UPDATE == operation) {
      template = "%s ha modificato una timbratura: %s";
    } else if (Crud.DELETE == operation) {
      template = "%s ha eliminato una timbratura: %s";
    } else {
      template = null;
    }
    final String message = String.format(template, person.fullName(), stamping.date.toString(DTF));

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
  private void notifyAbsence(Absence absence, User currentUser, Crud operation) {
    Verify.verifyNotNull(absence);
    final Person person = absence.personDay.person;
    final String template;
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
    } else {
      modifier = person.fullName();
    }
    final String message = String.format(template, modifier,
        absence.personDay.date.toString(DF), absence.absenceType.code);

    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN) 
        || uro.role.name.equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.user).forEach(user -> {
          Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.ABSENCE, absence.id).create();
        });
  }

  /**
   * Il metodo che si occupa di generare la corretta notifica al giusto utente.
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

    final Role roleDestination = getProperRole(absenceRequest); 
    if (roleDestination == null) {
      log.info("Non si è trovato il ruolo a cui inviare la notifica per la richiesta d'assenza di "
          + "{} di tipo {} con date {}, {}", 
          absenceRequest.person, absenceRequest.type, absenceRequest.startAt, absenceRequest.endTo);
      return;
    }
    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.equals(roleDestination))
          .map(uro -> uro.user).forEach(user -> {
            Notification.builder().destination(user).message(message)
            .subject(NotificationSubject.ABSENCE_REQUEST, absenceRequest.id).create();
          });
  }

  /**
   * Metodo privato che ritorna il ruolo a cui inviare la notifica della richiesta d'assenza.
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
    return role;
  }

  /**
   * Le politiche di notifica inserimenti/modiche di timbrature.
   * @param currentUser user che ha eseguito la richiesta
   * @param stamping la timbratura inserita
   */
  public void notificationStampingPolicy(User currentUser, Stamping stamping, 
      boolean insert, boolean update, boolean delete) {

    //Se l'user che ha fatto l'inserimento è utente di sistema esco
    if (currentUser.isSystemUser()) {
      return;
    }

    //Se l'user che ha fatto l'inserimento è amministratore di se stesso esco
    if (secureManager.officesWriteAllowed(currentUser).contains(currentUser.person.office)) {
      return;
    }

    //Se l'user che ha fatto l'inserimento è tecnologo e può autocertificare le timbrature esco
    if (currentUser.person.office.checkConf(EpasParam.TR_AUTOCERTIFICATION, "true")
        && currentUser.person.qualification.qualification <= 3) {
      return;
    }

    //negli altri casi notifica agli amministratori del personale ed al responsabile sede

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
   * @param currentUser utente che esegue la richiesta
   * @param absence assenza inserita
   * @param groupAbsenceType gruppo di inserimento
   */
  public void notificationAbsencePolicy(User currentUser, Absence absence, 
      GroupAbsenceType groupAbsenceType, boolean insert, boolean update, boolean delete) {

    //Se l'user che ha fatto l'inserimento è utente di sistema esco
    if (currentUser.isSystemUser() && !currentUser.roles.contains(AccountRole.MISSIONS_MANAGER)) {
      return;
    }

    //Se l'user che ha fatto l'inserimento è amministratore di se stesso esco
    if (currentUser.person != null 
        && secureManager.officesWriteAllowed(currentUser).contains(currentUser.person.office)) {
      return;
    }

    if (groupAbsenceType.name.equals(DefaultGroup.FERIE_CNR_DIPENDENTI.name()) 
        || groupAbsenceType.name.equals(DefaultGroup.MISSIONE.name())
        || groupAbsenceType.name.equals(DefaultGroup.RIPOSI_CNR_DIPENDENTI.name())
        || groupAbsenceType.name.equals(DefaultGroup.LAVORO_FUORI_SEDE.name())) {
      if (insert) {
        notifyAbsence(absence, currentUser, NotificationManager.Crud.CREATE);
        return;
      }
      if (update) {
        notifyAbsence(absence, currentUser, NotificationManager.Crud.UPDATE);
        return;
      }
      if (delete) {
        notifyAbsence(absence, currentUser, NotificationManager.Crud.DELETE);
        return;
      }

    }
  }

  /**
   * Notifica che una richiesta di assenza è stata rifiutata da uno degli 
   * approvatori del flusso.
   * 
   * @param absenceRequest la richiesta di assenza
   * @param refuser la persona che ha rifiutato la richiesta di assenza.
   */
  public void notificationAbsenceRequestRefused(
      AbsenceRequest absenceRequest, Person refuser) {

    Verify.verifyNotNull(absenceRequest);
    Verify.verifyNotNull(refuser);

    final String message = 
        String.format("La richiesta di assenza di tipo \"%s\" dal %s al %s "
            + "è stata rifiutata da %s",
            TemplateExtensions.label(absenceRequest.type),
            absenceRequest.type.isAllDay() 
            ? TemplateExtensions.format(absenceRequest.startAtAsDate()) 
                : TemplateExtensions.format(absenceRequest.startAt),
                absenceRequest.type.isAllDay() 
                ? TemplateExtensions.format(absenceRequest.endToAsDate()) 
                    : TemplateExtensions.format(absenceRequest.endTo),
                    refuser.getFullname());

    Notification.builder().destination(absenceRequest.person.user).message(message)
    .subject(NotificationSubject.ABSENCE_REQUEST, absenceRequest.id).create();

  }
  
  /**
   * Notifica che una richiesta di assenza è stata approvata da uno degli 
   * approvatori del flusso.
   * 
   * @param absenceRequest la richiesta di assenza
   * @param approver la persona che ha rifiutato la richiesta di assenza.
   */
  public void notificationAbsenceRequestApproved(
      AbsenceRequest absenceRequest, Person approver) {

    Verify.verifyNotNull(absenceRequest);
    Verify.verifyNotNull(approver);

    final String message = 
        String.format("La richiesta di assenza di tipo \"%s\" dal %s al %s "
            + "è stata accettata da %s",
            TemplateExtensions.label(absenceRequest.type),
            absenceRequest.type.isAllDay() 
            ? TemplateExtensions.format(absenceRequest.startAtAsDate()) 
                : TemplateExtensions.format(absenceRequest.startAt),
                absenceRequest.type.isAllDay() 
                ? TemplateExtensions.format(absenceRequest.endToAsDate()) 
                    : TemplateExtensions.format(absenceRequest.endTo),
                    approver.getFullname());

    Notification.builder().destination(absenceRequest.person.user).message(message)
    .subject(NotificationSubject.ABSENCE_REQUEST, absenceRequest.id).create();

  }

  /**
   * Gestore delle notifiche per le assenze inserite in seguito all'approvazione
   * di un richiesta di assenza.
   */
  public void notifyAbsenceOnAbsenceRequestCompleted(
      List<Absence> absences, Person person, Role role) {    
    Verify.verify(!absences.isEmpty());
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(role);

    final StringBuffer message = 
        new StringBuffer(
            String.format(
                "Flusso di richiesta assenza terminato, inserita una nuova assenza per %s.", 
                person.getFullname()));

    absences.forEach(a -> {
      message.append(String.format(" %s - %s.", a.absenceType.code, a.personDay.date.toString(DF)));
    });

    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(role.name))
          .map(uro -> uro.user).forEach(user -> {
            Notification.builder().destination(user).message(message.toString())
            .subject(NotificationSubject.ABSENCE, absences.stream().findFirst().get().id).create();
          });
  }

  /**
   * Il metodo che fa partire la notifica al giusto livello della catena.
   * @param currentUser l'utente che fa la richiesta
   * @param absenceRequest la richiesta di assenza via flusso
   * @param insert se si tratta di inserimento (per ora unico caso contemplato)
   */
  public void notificationAbsenceRequestPolicy(User currentUser, 
      AbsenceRequest absenceRequest, boolean insert) {
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
   *     d'assenza.
   * @param currentUser l'utente corrente che esegue la chiamata
   * @param absenceRequest la richiesta d'assenza da processare
   * @param insert se stiamo facendo un inserimento di una nuova richiesta d'assenza
   */
  public void sendEmailAbsenceRequestPolicy(User currentUser, 
      AbsenceRequest absenceRequest, boolean insert) {
    if (currentUser.isSystemUser()) {
      return;
    }
    if (insert) {
      sendEmailAbsenceRequest(absenceRequest);
    }
  }

  /**
   * Metodo che invia la mail all'utente responsabile dell'approvazione.
   * @param absenceRequest la richiesta d'assenza
   * @param currentUser l'utente a cui inviare la mail
   */
  private void sendEmailAbsenceRequest(AbsenceRequest absenceRequest) {

    Verify.verifyNotNull(absenceRequest);
    final Person person = absenceRequest.person;
    SimpleEmail simpleEmail = new SimpleEmail();
    final Role roleDestination = getProperRole(absenceRequest); 
    if (roleDestination == null) {
      log.warn("Non si è trovato il ruolo a cui inviare la mail per la richiesta d'assenza di "
          + "{} di tipo {} con date {}, {}", 
          absenceRequest.person, absenceRequest.type, absenceRequest.startAt, absenceRequest.endTo);
      return;
    }
    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.equals(roleDestination))
          .map(uro -> uro.user).forEach(user -> {
            try {
              simpleEmail.addTo(user.person.email);
            } catch (EmailException e) {
              e.printStackTrace();
            }
            simpleEmail.setSubject("ePas Approvazione flusso");
            try {
              simpleEmail.setMsg(createAbsenceRequestEmail(absenceRequest, user));
            } catch (EmailException e) {
              e.printStackTrace();
            }
            Mail.send(simpleEmail);
          });

  }

  /**
   * Metodo che compone il corpo della mail da inviare.
   * @param absenceRequest la richiesta d'assenza
   * @param user l'utente a cui inviare la mail
   * @return il corpo della mail da inviare all'utente responsabile dell'approvazione.
   */
  private String createAbsenceRequestEmail(AbsenceRequest absenceRequest, User user) {
    final String dateFormatter = "dd/MM/YYYY";
    String requestType = "";
    if (absenceRequest.type == AbsenceRequestType.COMPENSATORY_REST) {
      requestType = Messages.get("AbsenceRequestType.COMPENSATORY_REST");
    } else {
      requestType = Messages.get("AbsenceRequestType.VACATION_REQUEST");
    }
    final StringBuilder message = new StringBuilder()
        .append(String.format("Gentile %s,\r\n", user.person.fullName()));
    message.append(String.format("\r\nLe è stata notificata la richiesta di : %s",
        absenceRequest.person.fullName()));
    message.append(String.format("\r\n per una assenza di tipo: %s", requestType));
    if (absenceRequest.startAt.isEqual(absenceRequest.endTo)) {
      message.append(String.format("\r\n per il giorno: %s", 
          absenceRequest.startAt.toLocalDate().toString(dateFormatter)));
    } else {
      message.append(String.format("\r\n dal: %s", 
          absenceRequest.startAt.toLocalDate().toString(dateFormatter)));
      message.append(String.format("  al: %s", 
          absenceRequest.endTo.toLocalDate().toString(dateFormatter)));
    }
    String baseUrl = BASE_URL;
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }

    baseUrl = baseUrl + PATH + "?id=" + absenceRequest.id + "&type=" + absenceRequest.type;

    message.append(String.format("\r\n Verifica cliccando sul link seguente: %s", baseUrl));

    return message.toString();
  }
}
