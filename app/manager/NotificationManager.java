package manager;

import com.google.common.base.Verify;

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

  public void notifyStamping(Stamping stamping) {
    Verify.verifyNotNull(stamping);
    final Person person = stamping.personDay.person;
    final String message = String
        .format("%s ha inserito una nuova timbratura: %s", person.fullName(),
            stamping.date.toString(DTF));

    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN))
        .map(uro -> uro.user).forEach(user -> {
      Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.STAMPING, stamping.id).create();
    });
  }
}
