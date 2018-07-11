package manager;

import com.google.common.base.Verify;
import com.google.inject.Inject;
import helpers.TemplateExtensions;
import java.util.List;
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

/**
 * Genera le notifiche da inviare agl utenti.
 * 
 * @author daniele
 * @since 23/06/16.
 */
public class NotificationManager {

  private SecureManager secureManager;

  @Inject
  public NotificationManager(SecureManager secureManager) {
    this.secureManager = secureManager;
  }
  
  private static final String DTF = "dd/MM/YYYY - HH:mm";
  private static final String DF = "dd/MM/YYYY";

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
      //notifyAbsence(absence, currentUser, NotificationManager.CRUD.CREATE);
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
        String.format("La richiesta di assenza di tipo \"%s\" dal {} al {} "
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

}
