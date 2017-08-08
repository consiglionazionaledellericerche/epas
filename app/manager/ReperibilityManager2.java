package manager;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import controllers.Security;

import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityType;
import models.PersonShiftShiftType;
import models.ShiftType;
import models.User;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiore delle operazioni sulla reperibilità ePAS.
 *
 * @author dario
 */
@Slf4j
public class ReperibilityManager2 {
  
  /**
   * 
   * @return la lista delle attività di reperibilità visibili all'utente che ne fa la richiesta.
   */
  public List<PersonReperibilityType> getUserActivities() {
    List<PersonReperibilityType> activities = Lists.newArrayList();
    User currentUser = Security.getUser().get();
    Person person = currentUser.person;
    if (person != null) {
      if (!person.reperibilityTypes.isEmpty()) {
        activities.addAll(person.reperibilityTypes.stream()
            
            .sorted(Comparator.comparing(o -> o.description))
            .collect(Collectors.toList()));

      }
//      if (!person.categories.isEmpty()) {
//        activities.addAll(person.categories.stream()
//            .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
//            .sorted(Comparator.comparing(o -> o.type))
//            .collect(Collectors.toList()));
//      }
      if (person.reperibility != null) {
        activities.add(person.reperibility.personReperibilityType);
      }
    } else {
      if (currentUser.isSystemUser()) {
        activities.addAll(PersonReperibilityType.findAll());
      }
    }
    return activities.stream().distinct().collect(Collectors.toList());
  }
  
  /**
   * @param reperibilityType attività di reperibilità
   * @param start data di inizio del periodo
   * @param end data di fine del periodo
   * @return La lista di tutte le persone abilitate su quell'attività nell'intervallo di tempo
   *     specificato.
   */
  public List<PersonReperibility> reperibilityWorkers(
      PersonReperibilityType reperibilityType, LocalDate start,
      LocalDate end) {
    if (reperibilityType.isPersistent() && start != null && end != null) {
      return reperibilityType.personReperibilities.stream()
          .filter(pr -> pr.dateRange().isConnected(
              Range.closed(start, end)))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }
}