package manager;

import com.google.common.base.Verify;

import models.Absence;
import models.Notification;
import models.Person;
import models.Role;
import models.Stamping;
import models.enumerate.NotificationSubject;

/**
 * @author daniele
 * @since 23/06/16.
 */
public class NotificationManager {

  private final static String DTF = "dd/MM/YYYY - HH:mm";
  private final static String DF = "dd/MM/YYYY";

  public enum CRUD {
    CREATE,
    READ,
    UPDATE,
    DELETE
  }

  public void notifyStamping(Stamping stamping, CRUD operation) {
    Verify.verifyNotNull(stamping);
    final Person person = stamping.personDay.person;
    final String template;
    if (CRUD.CREATE == operation) {
      template = "%s ha inserito una nuova timbratura: %s";
    } else if (CRUD.UPDATE == operation) {
      template = "%s ha modificato una timbratura: %s";
    } else if (CRUD.DELETE == operation) {
      template = "%s ha eliminato una timbratura: %s";
    } else {
      template = null;
    }
    final String message = String.format(template, person.fullName(), stamping.date.toString(DTF));

    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN))
        .map(uro -> uro.user).forEach(user -> {
          if (operation != CRUD.DELETE) {
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

  public void notifyAbsence(Absence absence, CRUD operation) {
    Verify.verifyNotNull(absence);
    final Person person = absence.personDay.person;
    final String template;
    if (CRUD.CREATE == operation) {
      template = "%s ha inserito una nuova assenza: %s - %s";
    } else if (CRUD.UPDATE == operation) {
      template = "%s ha modificato un'assenza: %s - %s";
    } else if (CRUD.DELETE == operation) {
      template = "%s ha eliminato un'assenza: %s - %s";
    } else {
      template = null;
    }
    final String message = String.format(template, person.fullName(),
        absence.personDay.date.toString(DF), absence.absenceType.code);

    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN))
        .map(uro -> uro.user).forEach(user -> {
      Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.ABSENCE, absence.id).create();
    });
  }

}
