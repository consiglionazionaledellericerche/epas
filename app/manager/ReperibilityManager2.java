package manager;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import controllers.Security;
import dao.history.HistoricalDao;
import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ReperibilityTypeMonth;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
      //FIXME: non si è stabilito ancora se ci devono essere anche qui i gestori come nei turni
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

  /**
   * cancella il personShiftDay.
   * @param personShiftDay il personShiftDay da cancellare
   */
  public void delete(PersonReperibilityDay personReperibilityDay) {

    Verify.verifyNotNull(personReperibilityDay).delete();
    recalculate(personReperibilityDay);
  }


  private void recalculate(PersonReperibilityDay personReperibilityDay) {

    final PersonReperibilityType reperibilityType = personReperibilityDay.reperibilityType;

    // Aggiornamento dello ShiftTypeMonth
    if (reperibilityType != null) {

      //FIXME: servono questi due controlli???
      // Ricalcoli sul turno
      if (personReperibilityDay.isPersistent()) {
        checkReperibilityValid(personReperibilityDay);
      }

      // Ricalcoli sui giorni coinvolti dalle modifiche
      checkReperibilityDayValid(personReperibilityDay.date, reperibilityType);

      /*
       *  Recupera la data precedente dallo storico e verifica se c'è stato un 
       *  cambio di date sul turno. In tal caso effettua il ricalcolo anche 
       *  sul giorno precedente (spostamento di un turno da un giorno all'altro)
       */
      HistoricalDao.lastRevisionsOf(PersonReperibilityDay.class, personReperibilityDay.id)
      .stream().limit(1).map(historyValue -> {
        PersonReperibilityDay pd = (PersonReperibilityDay) historyValue.value;
        return pd.date;
      }).filter(Objects::nonNull).distinct()
      .forEach(localDate -> {
        if (!localDate.equals(personReperibilityDay.date)) {
          checkReperibilityDayValid(localDate, reperibilityType);
        }
      });

      // Aggiornamento del relativo ReperibilityTypeMonth (per incrementare il campo version)
      ReperibilityTypeMonth newStatus = 
          reperibilityType.monthStatusByDate(personReperibilityDay.date)
          .orElse(new ReperibilityTypeMonth());

      if (newStatus.personReperibilityType != null) {
        newStatus.updatedAt = LocalDateTime.now();
      } else {
        newStatus.yearMonth = new YearMonth(personReperibilityDay.date);
        newStatus.personReperibilityType = reperibilityType;
      }
      newStatus.save();

    }
  }

  public void checkReperibilityValid(PersonReperibilityDay personReperibilityDay) {
    /*
     * 0. Dev'essere una reperibilità persistente.
     * 1. Non ci siano assenze giornaliere
     * 2. Non ci devono essere già reperibili per quel giorno
     * 3. 
     */
    //TODO: va implementato davvero?
  }
  
  public void checkReperibilityDayValid(LocalDate date, PersonReperibilityType type) {
    //TODO: va implementato davvero?
  }
}