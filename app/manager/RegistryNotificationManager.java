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

import com.google.common.base.Verify;
import models.Notification;
import models.Office;
import models.Person;
import models.Role;
import models.enumerate.NotificationSubject;

/**
 * Contiene le notifiche relative all'anagrafica del personale e delle sedi.
 *
 * @author Cristian Lucchesi
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
            + "la vecchia sede era %s (sedeId = %s)", person.getFullname(), person.getOffice().getName(),
            person.getOffice().getCodeId(), oldOffice.getName(), oldOffice.getCodeId());

    //Notifica ai nuovi amministratori della nuova persona da gestire.
    person.getOffice().getUsersRolesOffices().stream()
        .filter(uro -> uro.getRole().getName().equals(Role.PERSONNEL_ADMIN) 
            || uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.getUser()).forEach(user -> {
          Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.PERSON_HAS_CHANGED_OFFICE, person.id)          
          .create();
        });

    //Notifica ai vecchi amministratori della persona non più da gestire.
    oldOffice.getUsersRolesOffices().stream()
        .filter(uro -> uro.getRole().getName().equals(Role.PERSONNEL_ADMIN) 
            || uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.getUser()).forEach(user -> {
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
        person.getFullname(), person.getNumber());

    //Notifica ai nuovi amministratori della nuova persona da gestire.
    person.getOffice().getUsersRolesOffices().stream()
        .filter(uro -> uro.getRole().getName().equals(Role.PERSONNEL_ADMIN) 
            || uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))
        .map(uro -> uro.getUser()).forEach(user -> {
          Notification.builder().destination(user).message(message)
          .subject(NotificationSubject.PERSON_HAS_CHANGED_OFFICE, person.id)          
          .create();
        });
    
  }

}