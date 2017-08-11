package manager;

import com.google.common.base.Optional;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import controllers.Security;

import dao.CompetenceCodeDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.history.HistoricalDao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ReperibilityTypeMonth;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.User;
import models.enumerate.ShiftTroubles;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;

import play.i18n.Messages;

/**
 * Gestiore delle operazioni sulla reperibilità ePAS.
 *
 * @author dario
 */
@Slf4j
public class ReperibilityManager2 {

  private static final String REPERIBILITY_WORKDAYS = "207";
  private static final String REPERIBILITY_HOLIDAYS = "208";

  private final PersonReperibilityDayDao reperibilityDayDao;
  private final PersonDayDao personDayDao;
  private final PersonDayManager personDayManager;
  private final CompetenceCodeDao competenceCodeDao;

  @Inject
  public ReperibilityManager2(PersonReperibilityDayDao reperibilityDayDao, 
      PersonDayDao personDayDao, PersonDayManager personDayManager, 
      CompetenceCodeDao competenceCodeDao) {
    this.reperibilityDayDao = reperibilityDayDao;
    this.personDayDao = personDayDao;
    this.personDayManager = personDayManager;
    this.competenceCodeDao = competenceCodeDao;
  }
  
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
   * salva il personReperibilityDay ed effettua i ricalcoli.
   * @param personReperibilityDay il personReperibilityDay da salvare
   */
  public void save(PersonReperibilityDay personReperibilityDay) {

    Verify.verifyNotNull(personReperibilityDay).save();
    recalculate(personReperibilityDay);
  }

  /**
   * cancella il personReperibilityDay.
   * @param personReperibilityDay il personReperibilityDay da cancellare
   */
  public void delete(PersonReperibilityDay personReperibilityDay) {

    Verify.verifyNotNull(personReperibilityDay).delete();
    recalculate(personReperibilityDay);
  }


  private void recalculate(PersonReperibilityDay personReperibilityDay) {

    final PersonReperibilityType reperibilityType = personReperibilityDay.reperibilityType;

    // Aggiornamento del ReperibilityTypeMonth
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

  /**
   * Verifica se un turno puo' essere inserito senza violare le regole dei turni
   *
   * @param personReperibilityDay il personShiftDay da inserire
   * @return l'eventuale stringa contenente l'errore evidenziato in fase di inserimento del turno.
   */
  public Optional<String> reperibilityPermitted(PersonReperibilityDay personReperibilityDay) {

    /**
     * 0. Verificare se la persona è segnata in quell'attività in quel giorno
     *    return shift.personInactive
     * 1. La Persona non deve essere già reperibile per quel giorno
     * 2. La Persona non deve avere assenze giornaliere.
     * 3. La reperibilità non sia già presente  
     * 4. Controllare anche il quantitativo di giorni di reperibilità feriale e festiva massimi?  
     */

    //Verifica se la persona è attiva in quell'attività in quel giorno
    final boolean isActive = personReperibilityDay.personReperibility.dateRange()
        .contains(personReperibilityDay.date);
    if (!isActive) {
      return Optional.of(Messages.get("reperibility.personInactive"));
    }

    // Verifica che la persona non abbia altre reperibilità nello stesso giorno 
    final Optional<PersonReperibilityDay> personReperibility = reperibilityDayDao
        .getPersonReperibilityDay(
            personReperibilityDay.personReperibility.person, personReperibilityDay.date);

    if (personReperibility.isPresent()) {
      return Optional.of(Messages.get("reperibility.alreadyInReperibility", 
          personReperibility.get().reperibilityType));
    }

    // verifica che la persona non sia assente nel giorno
    final Optional<PersonDay> personDay = personDayDao
        .getPersonDay(personReperibilityDay.personReperibility.person, personReperibilityDay.date);

    if (personDay.isPresent() && personDayManager.isAllDayAbsences(personDay.get())) {
      return Optional.of(Messages.get("reperibility.absenceInDay"));
    }

    List<PersonReperibilityDay> list = reperibilityDayDao
        .getPersonReperibilityDayFromPeriodAndType(
            personReperibilityDay.date, personReperibilityDay.date,
            personReperibilityDay.reperibilityType, Optional.absent());

    //controlla che la reperibilità nel giorno sia già stata assegnata ad un'altra persona
    if (!list.isEmpty()) {
      return Optional.of(Messages
          .get("reperibility.dayAlreadyAssigned", 
              personReperibilityDay.personReperibility.person.fullName()));
    }

    return Optional.absent();
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

  /**
   *
   * @param reperibility attività sulla quale effettuare i calcoli
   * @param from data di inizio da cui calcolare
   * @param to data di fine
   * @return Restituisce una mappa con i giorni di reperibilità maturati per ogni persona.
   */
  public Map<Person, Integer> calculateReperibilityWorkDaysCompetences(
      PersonReperibilityType reperibility, LocalDate from, LocalDate to) {

    final Map<Person, Integer> reperibilityWorkDaysCompetences = new HashMap<>();


    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if (to.isAfter(today)) {
      lastDay = today;
    } else {
      lastDay = to;
    }
    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode(REPERIBILITY_WORKDAYS);
    involvedReperibilityWorkers(reperibility, from, to).forEach(person -> {

      int competences = 
          calculatePersonReperibilityCompetencesInPeriod(reperibility, person, from, lastDay, code);
      reperibilityWorkDaysCompetences.put(person, competences);
    });

    return reperibilityWorkDaysCompetences;
  }

  /**
   * @param reperibility attività di reperibilità
   * @param from data di inizio
   * @param to data di fine
   * @return Una lista di persone che sono effettivamente coinvolte in reperibilità in un 
   *     determinato periodo (Dipendenti con le reperibilità attive in quel periodo).
   */
  public List<Person> involvedReperibilityWorkers(PersonReperibilityType reperibility, 
      LocalDate from, LocalDate to) {
    return reperibilityDayDao.byTypeAndPeriod(reperibility, from, to)
        .stream().map(rep -> rep.person).distinct().collect(Collectors.toList());
  }

  /**
   * @param reperibility attività di turno
   * @param person Persona sulla quale effettuare i calcoli
   * @param from data iniziale
   * @param to data finale
   * @return il numero di giorni di competenza maturati in base alle reperibilità effettuate
   *     nel periodo selezionato (di norma serve calcolarli su un intero mese al massimo).
   */
  public int calculatePersonReperibilityCompetencesInPeriod(
      PersonReperibilityType reperibility, Person person, 
      LocalDate from, LocalDate to, CompetenceCode code) {

    // TODO: 08/06/17 Sicuramente vanno differenziati per tipo di competenza.....
    // c'è sono da capire qual'è la discriminante
    int reperibilityCompetences = 0;
    final List<PersonReperibilityDay> reperibilities = reperibilityDayDao
        .getPersonReperibilityDaysByPeriodAndType(from, to, reperibility, person);

    if (code.codeToPresence.equalsIgnoreCase(REPERIBILITY_WORKDAYS)) {
      reperibilityCompetences = (int) reperibilities.stream()
          .filter(rep -> !personDayManager.isHoliday(person, rep.date)).count();
    } else {
      reperibilityCompetences = (int) reperibilities.stream()
          .filter(rep -> personDayManager.isHoliday(person, rep.date)).count();
    }

    return reperibilityCompetences;
  }

  /**
   * 
   * @param reperibility il tipo di reperibilità 
   * @param start la data di inizio da cui conteggiare
   * @param end la data di fine entro cui conteggiare
   * @return la mappa contenente i giorni di reperibilità festiva per ogni dipendente reperibile.
   */
  public Map<Person, Integer> calculateReperibilityHolidaysCompetences(
      PersonReperibilityType reperibility, LocalDate start, LocalDate end) {

    final Map<Person, Integer> reperibilityHolidaysCompetences = new HashMap<>();

    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if (end.isAfter(today)) {
      lastDay = today;
    } else {
      lastDay = end;
    }
    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode(REPERIBILITY_HOLIDAYS);
    involvedReperibilityWorkers(reperibility, start, end).forEach(person -> {

      int competences = calculatePersonReperibilityCompetencesInPeriod(reperibility, 
          person, start, lastDay, code);
      reperibilityHolidaysCompetences.put(person, competences);
    });

    return reperibilityHolidaysCompetences;
  }
}