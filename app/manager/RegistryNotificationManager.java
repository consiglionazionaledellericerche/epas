package manager;

import com.google.common.base.Verify;
import models.Notification;
import models.Office;
import models.Person;
import models.Role;
import models.enumerate.NotificationSubject;

/**
 * Contiene le notifiche relative all'anagrafica del personale e delle sedi.
 * 
 * @author cristian
 *
 */
public class RegistryNotificationManager {


  /**
   * Notifica agli amministratori del personale delle nuova e della vecchia 
   * sede che c'è stato un cambio di assegnazione.
   * 
   * @param person la persona che ha cambiato sede
   * @param oldOffice la vecchia sede della persona
   */
  public void notifyPersonHasChangedOffice(Person person, Office oldOffice) {

    Verify.verifyNotNull(person);
    Verify.verifyNotNull(oldOffice);

    final String message = String.format(
        "La persona %s ha cambiato sede, la nuova sede è %s (sedeId = %s), "
            + "la vecchia sede era %s (sedeId = %s)", person.getFullname(), person.office.getName(),
            person.office.codeId, oldOffice.getName(), oldOffice.codeId);

    //Notifica ai nuovi amministratori della nuova persona da gestire.
    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN) 
            || uro.role.name.equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.user).forEach(user -> {
          Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.PERSON_HAS_CHANGED_OFFICE, person.id)          
          .create();
        });

    //Notifica ai vecchi amministratori della persona non più da gestire.
    oldOffice.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN) 
            || uro.role.name.equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.user).forEach(user -> {
          Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.PERSON_HAS_CHANGED_OFFICE)          
          .create();
        });
  }

  /**
   * Notifica agli amministratori della sede l'ingresso di una nuova persona.
   * 
   * @param person la nuova persona.
   */
  public void notifyNewPerson(Person person) {
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.id);
    
    final String message = String.format(
        "Una nuova persona è stata associata alla tua sede: %s (matricola = %s)", 
        person.getFullname(), person.number);

    //Notifica ai nuovi amministratori della nuova persona da gestire.
    person.office.usersRolesOffices.stream()
        .filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN) 
            || uro.role.name.equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.user).forEach(user -> {
          Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.PERSON_HAS_CHANGED_OFFICE, person.id)          
          .create();
        });
    
  }

}
