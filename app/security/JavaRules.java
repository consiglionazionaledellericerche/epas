package security;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import manager.configurations.EpasParam;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.Role;
import models.Stamping;
import models.User;
import models.enumerate.CodesForEmployee;
import models.enumerate.StampTypes;

import play.mvc.results.Forbidden;

/**
 * @author daniele
 * @since 07/07/16.
 */
public class JavaRules {

  private final Provider<Optional<User>> currentUser;
  private final Provider<String> currentAction;

  @Inject
  JavaRules(Provider<Optional<User>> user, @Named("request.action") Provider<String> action) {
    currentUser = user;
    currentAction = action;
  }

  public void checkForAbsences(AbsenceType at, Person person) {
    final Optional<User> user = currentUser.get();
    if (user.isPresent()) {
      if (user.get().usersRolesOffices.stream().filter(uro -> uro.role.name.equals(Role.DEVELOPER)
          && uro.office.persons.contains(person)).findFirst().isPresent()) {
        return;
      }
      // L'utente ha il ruolo di amministratore sull'ufficio d'appartenenza della persona
      if (user.get().usersRolesOffices.stream().filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN)
          && uro.office.persons.contains(person)).findFirst().isPresent()) {
        return;
      }
      if ( // L'assenza è tra i codici abilitati per i dipendenti
          CodesForEmployee.getCodes().contains(at.code) &&
              // L'ufficio e' abilitato per l'inserimento di assenze fuori sede
              person.office.configurations.stream()
                  .filter(c -> c.epasParam == EpasParam.WORKING_OFF_SITE
                      && c.fieldValue.equals("true")).findFirst().isPresent() &&
              // La persona è abilitata in configurazione all'inserimento autonomo di quell'assenza
              person.personConfigurations.stream()
                  .filter(pc -> pc.epasParam == EpasParam.OFF_SITE_STAMPING
                      && pc.fieldValue.equals("true")).findFirst().isPresent() &&
              // L'assenza appartiene alla persona specificata
              user.get().usersRolesOffices.stream()
                  .filter(uro -> uro.role.name.equals(Role.EMPLOYEE)
                      && uro.office.persons.contains(person))
                  .findFirst().isPresent()) {
        return;
      }
    }
    throw new Forbidden("Access forbidden");
  }

  public void checkForAbsences(Absence absence) {
    final Optional<User> user = currentUser.get();

    if (user.isPresent()) {
      if (user.get().usersRolesOffices.stream().filter(uro -> uro.role.name.equals(Role.DEVELOPER)
          && uro.office.persons.contains(absence.personDay.person)).findFirst().isPresent()) {
        return;
      }
      // L'utente ha il ruolo di amministratore sull'ufficio d'appartenenza della persona
      if (user.get().usersRolesOffices.stream().filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN)
          && uro.office.persons.contains(absence.personDay.person)).findFirst().isPresent()) {
        return;
      }

      if ( // L'assenza è tra i codici abilitati per i dipendenti
          CodesForEmployee.getCodes().contains(absence.absenceType.code) &&
              // L'ufficio e' abilitato per l'inserimento di assenze fuori sede
              absence.personDay.person.office.configurations.stream()
                  .filter(c -> c.epasParam == EpasParam.WORKING_OFF_SITE
                      && c.fieldValue.equals("true")).findFirst().isPresent() &&
              // La persona è abilitata in configurazione all'inserimento autonomo di quell'assenza
              absence.personDay.person.personConfigurations.stream()
                  .filter(pc -> pc.epasParam == EpasParam.OFF_SITE_STAMPING
                      && pc.fieldValue.equals("true")).findFirst().isPresent() &&
              // L'assenza appartiene alla persona specificata
              user.get().usersRolesOffices.stream()
                  .filter(uro -> uro.role.name.equals(Role.EMPLOYEE)
                      && uro.office.persons.contains(absence.personDay.person))
                  .findFirst().isPresent()) {
        return;
      }
    }
    throw new Forbidden("Access forbidden");
  }

  public void checkForStamping(Stamping stamping) {
    final Optional<User> user = currentUser.get();

    if (user.isPresent()) {
      if (user.get().usersRolesOffices.stream().filter(uro -> uro.role.name.equals(Role.DEVELOPER)
          && uro.office.persons.contains(stamping.personDay.person)).findFirst().isPresent()) {
        return;
      }
      // L'utente ha il ruolo di amministratore sull'ufficio d'appartenenza della persona
      if (user.get().usersRolesOffices.stream().filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN)
          && uro.office.persons.contains(stamping.personDay.person)).findFirst().isPresent()) {
        return;
      }

      if ( // L'assenza è tra i codici abilitati per i dipendenti
          StampTypes.LAVORO_FUORI_SEDE == stamping.stampType &&
              // L'ufficio e' abilitato per l'inserimento di assenze fuori sede
              stamping.personDay.person.office.configurations.stream()
                  .filter(c -> c.epasParam == EpasParam.WORKING_OFF_SITE
                      && c.fieldValue.equals("true")).findFirst().isPresent() &&
              // La persona è abilitata in configurazione all'inserimento autonomo di quell'assenza
              stamping.personDay.person.personConfigurations.stream()
                  .filter(pc -> pc.epasParam == EpasParam.OFF_SITE_STAMPING
                      && pc.fieldValue.equals("true")).findFirst().isPresent() &&
              // L'assenza appartiene alla persona specificata
              user.get().usersRolesOffices.stream()
                  .filter(uro -> uro.role.name.equals(Role.EMPLOYEE)
                      && uro.office.persons.contains(stamping.personDay.person))
                  .findFirst().isPresent()) {
        return;
      }
    }
    throw new Forbidden("Access forbidden");
  }

}
