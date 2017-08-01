package manager;


import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import controllers.Security;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;
import dao.ShiftTypeMonthDao;
import dao.history.HistoricalDao;

import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.services.PairStamping;

import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonDay;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftDayInTrouble;
import models.PersonShiftShiftType;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.User;
import models.enumerate.ShiftTroubles;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.YearMonth;

import play.i18n.Messages;


/**
 * Gestiore delle operazioni sui turni ePAS.
 *
 * @author arianna
 */
@Slf4j
public class ShiftManager2 {

  private static final String codShiftNight = "T2";
  private static final String codShiftHolyday = "T3";
  private static final String codShift = "T1";
  private static final int SIXTY_MINUTES = 60;

  private final PersonDayManager personDayManager;
  private final PersonShiftDayDao personShiftDayDao;
  private final PersonDayDao personDayDao;
  private final ShiftDao shiftDao;
  private final CompetenceCodeDao competenceCodeDao;
  private final CompetenceDao competenceDao;
  private final ShiftTypeMonthDao shiftTypeMonthDao;


  @Inject
  public ShiftManager2(PersonDayManager personDayManager, PersonShiftDayDao personShiftDayDao,
      PersonDayDao personDayDao, ShiftDao shiftDao, CompetenceUtility competenceUtility,
      CompetenceCodeDao competenceCodeDao, CompetenceDao competenceDao,
      PersonMonthRecapDao personMonthRecapDao, ShiftTypeMonthDao shiftTypeMonthDao) {

    this.personDayManager = personDayManager;
    this.personShiftDayDao = personShiftDayDao;
    this.personDayDao = personDayDao;
    this.shiftDao = shiftDao;
    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;
    this.shiftTypeMonthDao = shiftTypeMonthDao;
  }

  /**
   * @return la lista delle attività associate all'utente che ne fa richiesta.
   */
  public List<ShiftType> getUserActivities() {
    List<ShiftType> activities = Lists.newArrayList();
    User currentUser = Security.getUser().get();
    Person person = currentUser.person;
    if (person != null) {
      if (!person.shiftCategories.isEmpty()) {
        activities.addAll(person.shiftCategories.stream()
            .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
            .sorted(Comparator.comparing(o -> o.type))
            .collect(Collectors.toList()));

      }
      if (!person.categories.isEmpty()) {
        activities.addAll(person.categories.stream()
            .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
            .sorted(Comparator.comparing(o -> o.type))
            .collect(Collectors.toList()));
      }
      if (person.personShift != null) {
        activities.addAll(person.personShift.personShiftShiftTypes.stream()
            .map(psst -> psst.shiftType)
            .sorted(Comparator.comparing(o -> o.type))
            .collect(Collectors.toList()));
      }
    } else {
      if (currentUser.isSystemUser()) {
        activities.addAll(ShiftType.findAll());
      }
    }
    return activities.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Controlla se il PersonShiftDay è compatibile con la presenza in Istituto in
   * un determinato giorno:
   * - assenza o missione
   *
   * @return ShiftTroubles.PERSON_IS_ABSENT, ""
   */
  public String checkShiftDayCompatibilityWhithAllDayPresence(PersonShiftDay shift,
      LocalDate date) {
    String errCode = "";
    Optional<PersonDay> personDay = personDayDao.getPersonDay(shift.personShift.person, date);

    // controlla che il nuovo turno non coincida con un giorno di assenza del turnista 
    if (personDayManager.isAllDayAbsences(personDay.get())) {
      errCode = ShiftTroubles.PERSON_IS_ABSENT.toString();
    }
    return errCode;
  }


  /**
   * popola la tabella PersonShift andando a cercare nel db tutte le persone che son già
   * state abilitate a usufruire dell'indennità di turno.
   */
  public void populatePersonShiftTable() {
    CompetenceCode shift = competenceCodeDao.getCompetenceCodeByCode(codShift);
    CompetenceCode shiftNight = competenceCodeDao.getCompetenceCodeByCode(codShiftNight);
    CompetenceCode shiftHoliday = competenceCodeDao.getCompetenceCodeByCode(codShiftHolyday);
    List<CompetenceCode> codeList = Lists.newArrayList();
    codeList.add(shift);
    codeList.add(shiftNight);
    codeList.add(shiftHoliday);
    List<PersonCompetenceCodes> shiftPeople = competenceCodeDao
        .listByCodes(codeList, Optional.fromNullable(LocalDate.now()));
    shiftPeople.forEach(item -> {
      if (personShiftDayDao.getPersonShiftByPerson(item.person) == null) {
        PersonShift personShift = new PersonShift();
        personShift.description = "turni di " + item.person.fullName();
        personShift.person = item.person;
        personShift.save();
      } else {
        log.info("Dipendente {} {} già associato all'interno della tabella person_shift",
            item.person.name, item.person.surname);
      }

    });
  }


  /* **********************************************************************************/
  /* Sezione di metodi utilizzati al bootstrap per sistemare le situazioni sui turni  */
  /* **********************************************************************************/


  /**
   * Verifica se un turno puo' essere inserito senza violare le regole dei turni
   *
   * @param personShiftDay il personShiftDay da inserire
   * @return l'eventuale stringa contenente l'errore evidenziato in fase di inserimento del turno.
   */
  public Optional<String> shiftPermitted(PersonShiftDay personShiftDay) {

    /**
     * 0. Verificare se la persona è segnata in quell'attività in quel giorno
     *    return shift.personInactive
     * 1. La Persona non deve essere già in un turno per quel giorno
     * 2. La Persona non deve avere assenze giornaliere.
     * 3. Il Turno per quello slot non sia già presente    
     */

    //Verifica se la persona è attiva in quell'attività in quel giorno
    final boolean isActive = personShiftDay.shiftType.personShiftShiftTypes.stream().anyMatch(
        personShiftShiftType -> personShiftShiftType.personShift.equals(personShiftDay.personShift)
            && personShiftShiftType.dateRange().contains(personShiftDay.date));
    if (!isActive) {
      return Optional.of(Messages.get("shift.personInactive"));
    }

    // Verifica che la persona non abbia altri turni nello stesso giorno (anche su altre attività)
    // TODO: 06/06/17 Verificare se questo vincolo va bene o deve esistere solo per 2
    // turni
    // sulla stessa attività
    final Optional<PersonShiftDay> personShift = personShiftDayDao
        .byPersonAndDate(personShiftDay.personShift.person, personShiftDay.date);

    if (personShift.isPresent()) {
      return Optional.of(Messages.get("shift.alreadyInShift", personShift.get().shiftType));
    }

    final Optional<PersonDay> personDay = personDayDao
        .getPersonDay(personShiftDay.personShift.person, personShiftDay.date);

    if (personDay.isPresent() && personDayManager.isAllDayAbsences(personDay.get())) {
      return Optional.of(Messages.get("shift.absenceInDay"));
    }

    List<PersonShiftDay> list = personShiftDayDao
        .byTypeInPeriod(personShiftDay.date, personShiftDay.date,
            personShiftDay.shiftType, Optional.absent());

    for (PersonShiftDay registeredDay : list) {
      //controlla che il turno in quello slot sia già stato assegnato ad un'altra persona
      if (registeredDay.shiftSlot == personShiftDay.shiftSlot) {
        return Optional.of(Messages
            .get("shift.slotAlreadyAssigned", registeredDay.personShift.person.fullName()));
      }
    }
    return Optional.absent();
  }


  /**
   * crea il personShiftDayInTrouble per i parametri passati.
   *
   * @param shift il personShiftDay con problemi
   * @param cause la causa da aggiungere ai problemi
   */
  public void setShiftTrouble(final PersonShiftDay shift, ShiftTroubles cause) {

    final PersonShiftDayInTrouble trouble = new PersonShiftDayInTrouble(shift, cause);

    if (!shift.hasError(trouble.cause)) {
      trouble.save();
      shift.troubles.add(trouble);
      log.info("Nuovo personShiftDayInTrouble {} - {} - {}",
          shift.personShift.person.getFullname(), shift.date, cause);
    }
  }

  /**
   * rimuove il trouble se il problema è stato risolto.
   *
   * @param shift il personShiftDay con problemi
   * @param cause la causa da rimuovere dai problemi
   */
  public void fixShiftTrouble(final PersonShiftDay shift, ShiftTroubles cause) {

    java.util.Optional<PersonShiftDayInTrouble> psdit = shift.troubles.stream()
        .filter(trouble -> trouble.cause == cause).findFirst();

    if (psdit.isPresent()) {
      shift.troubles.remove(psdit.get());
      psdit.get().delete();
      log.info("Rimosso personShiftDayInTrouble {} - {} - {}",
          shift.personShift.person.getFullname(), shift.date, cause);
    }
  }

  /**
   * Verifica che il turno in questione sia valido e persiste nei Troubles
   * gli eventuali errori (li rimuove nel caso siano risolti).
   *
   * @param personShiftDay il personShiftDay da controllare
   */
  public void checkShiftValid(PersonShiftDay personShiftDay) {
    /*
     * 0. Dev'essere un turno persistente.
     * 1. Non ci siano assenze giornaliere
     * 2. Controlli sul tempo a lavoro (Soglie, copertura orario, pause durante il turno etc...)
     * 3. 
     */
    final ShiftType shiftType = Verify.verifyNotNull(personShiftDay.shiftType);
    final LocalDate today = LocalDate.now();

    log.debug("Ricalcoli sul turno di {} in data {}", personShiftDay.personShift.person,
        personShiftDay.date);

    final List<ShiftTroubles> shiftTroubles = new ArrayList<>();
    
    //Verifica se la persona è attiva in quell'attività in quel giorno
    final boolean isActive = shiftType.personShiftShiftTypes.stream().anyMatch(
        personShiftShiftType -> personShiftShiftType.personShift.equals(personShiftDay.personShift)
            && personShiftShiftType.dateRange().contains(personShiftDay.date));
    if (!isActive) {
      shiftTroubles.add(ShiftTroubles.PERSON_NOT_ASSIGNED);
    }

    Optional<PersonDay> optionalPersonDay =
        personDayDao.getPersonDay(personShiftDay.personShift.person, personShiftDay.date);
    int exceededThresholds = 0;
    
    if (optionalPersonDay.isPresent()) {
      final PersonDay personDay = optionalPersonDay.get();

      // 1. Controlli sulle assenze
      if (personDayManager.isAllDayAbsences(personDay)) {
        shiftTroubles.add(ShiftTroubles.PERSON_IS_ABSENT);
      }

      // Nelle date passate posso effettuare i controlli sul tempo a lavoro
      if (personShiftDay.date.isBefore(today)) {

        final LocalTime slotBegin = personShiftDay.slotBegin();
        final LocalTime slotEnd = personShiftDay.slotEnd();

        // 2. Controlli sulle timbrature...

        List<PairStamping> validPairStampings = personDayManager
            .getValidPairStampings(personDay.stampings);

        // Le coppie che si sovrappongono anche solo per un minuto con la finestra del turno
        List<PairStamping> shiftPairs = validPairStampings.stream()
            .filter(pair -> Range.closed(slotBegin, slotEnd).isConnected(
                Range.closed(pair.first.date.toLocalTime(), pair.second.date.toLocalTime())))
            .collect(Collectors.toList());

        // Se ci sono timbrature valide..
        if (slotBegin != null && slotEnd != null && !shiftPairs.isEmpty()) {

    

          // 2.a Verifiche sulle soglie

          // Soglia d'ingresso
          final LocalTime entranceStamping = shiftPairs.get(0).first.date.toLocalTime();
          final LocalTime entranceMaxThreshold = slotBegin
              .plusMinutes(shiftType.entranceMaxTolerance);
          final LocalTime entranceThreshold = slotBegin.plusMinutes(shiftType.entranceTolerance);

          if (entranceStamping.isAfter(entranceMaxThreshold)) {
            // Ingresso fuori dalla tolleranza massima (turno non valido)
            shiftTroubles.add(ShiftTroubles.MAX_ENTRANCE_TOLERANCE_EXCEEDED);
          } else if (entranceStamping.isAfter(entranceThreshold)
              && !entranceStamping.isAfter(entranceMaxThreshold)) {
            exceededThresholds++;
            // Ingresso tra la tolleranza minima e quella massima (turno decurtato di 1 ora)
            shiftTroubles.add(ShiftTroubles.MIN_ENTRANCE_TOLERANCE_EXCEEDED);
          }

          // Soglia di uscita
          final LocalTime exitStamping = shiftPairs.get(shiftPairs.size() - 1).second.date
              .toLocalTime();
          final LocalTime exitMaxThreshold = slotEnd.minusMinutes(shiftType.entranceMaxTolerance);
          final LocalTime exitThreshold = slotEnd.minusMinutes(shiftType.entranceTolerance);

          // Uscita fuori dalla tolleranza massima (turno non valido)
          if (exitStamping.isBefore(exitMaxThreshold)) {
            shiftTroubles.add(ShiftTroubles.MAX_EXIT_TOLERANCE_EXCEEDED);
          } else if (!exitStamping.isBefore(exitMaxThreshold)
              && exitStamping.isBefore(exitThreshold)) {
            // Uscita tra la tolleranza minima e quella massima (turno decurtato di 1 ora)
            exceededThresholds++;
            shiftTroubles.add(ShiftTroubles.MIN_EXIT_TOLERANCE_EXCEEDED);
          }

          RangeSet<LocalTime> rangeSet = TreeRangeSet.create();
          shiftPairs.forEach(pairStamping -> {
            rangeSet.add(Range.closed(pairStamping.first.date.toLocalTime(), pairStamping.second
                .date.toLocalTime()));
          });

          /*
           *  L'unico modo per capire se la pausa pranzo è contenuta nel turno è guardare 
           *  la timetable associata all'attività
           */
          final LocalTime lunchBreakStart = personShiftDay.lunchTimeBegin();
          final LocalTime lunchBreakEnd = personShiftDay.lunchTimeEnd();

          if (Range.open(slotBegin, slotEnd)
              .encloses(Range.closed(lunchBreakStart, lunchBreakEnd))) {
            rangeSet.add(Range.closed(lunchBreakStart,lunchBreakEnd));
          }    

          // Conteggio dei minuti di pausa fatti durante il turno
          Iterator<Range<LocalTime>> iterator = rangeSet.asRanges().iterator();
          Range<LocalTime> previousPair = iterator.next();
          int totalBreakMinutes = 0;

          while (iterator.hasNext()) {
            Range<LocalTime> nextPair = iterator.next();
            totalBreakMinutes += DateUtility.toMinute(nextPair.lowerEndpoint()) - DateUtility
                .toMinute(previousPair.upperEndpoint());
            previousPair = nextPair;
          }

          // La pausa fatta durante il turno supera quella permessa (turno non valido)
          if (totalBreakMinutes > shiftType.breakMaxInShift) {
            shiftTroubles.add(ShiftTroubles.MAX_BREAK_TOLERANCE_EXCEEDED);
          } else if (totalBreakMinutes > shiftType.breakInShift
              && totalBreakMinutes <= shiftType.breakMaxInShift) {
            exceededThresholds++;
            shiftTroubles.add(ShiftTroubles.MIN_BREAK_TOLERANCE_EXCEEDED);

          }

          if (exceededThresholds > shiftType.maxToleranceAllowed) {
            shiftTroubles.add(ShiftTroubles.TOO_MANY_EXCEEDED_THRESHOLDS);
          }

        } else {
          // Non ci sono abbastanza timbrature o non è possibile identificare lo slot del turno
          shiftTroubles.add(ShiftTroubles.NOT_ENOUGH_WORKING_TIME);

        }

      } else {
        shiftTroubles.add(ShiftTroubles.FUTURE_DAY);
      }

    } else { // Non esiste il PersonDay
      // E' una data passata senza nessuna informazione per quel giorno
      if (personShiftDay.date.isBefore(today)) {
        shiftTroubles.add(ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
      } else {
        shiftTroubles.add(ShiftTroubles.FUTURE_DAY);
      }

    }

    // Setto gli errori effettivi e rimuovo gli altri per ripulire eventuali calcoli precedenti
    List<ShiftTroubles> shiftErrors = ShiftTroubles.shiftSpecific();
    shiftTroubles.forEach(trouble -> {
      setShiftTrouble(personShiftDay, trouble);
      shiftErrors.remove(trouble);
    });

    shiftErrors.forEach(trouble -> fixShiftTrouble(personShiftDay, trouble));
    personShiftDay.exceededThresholds = exceededThresholds;
    personShiftDay.save();

  }


  /**
   * Verifica che i turni di un'attività in un determinato giorno siano tutti validi
   * inserisce l'errore PROBLEMS_ON_OTHER_SLOT sugli altri turni se uno dei turni
   * ha degli errori (o li rimuove in caso contrario).
   *
   * @param activity l'attività su cui ricercare i personshiftday
   * @param date la data in cui ricercare i personshiftday dell'attività activity
   */
  public void checkShiftDayValid(LocalDate date, ShiftType activity) {

    log.debug("Ricalcolo del giorno di turno {} - {}", activity, date);
    List<PersonShiftDay> shifts = shiftDao.getShiftDaysByPeriodAndType(date, date, activity);

    // 1. Controllo che siano coperti tutti gli slot
    int slotNumber = activity.shiftTimeTable.slotCount();

    if (slotNumber > shifts.size()) {
      shifts.forEach(shift -> setShiftTrouble(shift, ShiftTroubles.SHIFT_INCOMPLETED));
    } else {
      shifts.forEach(shift -> fixShiftTrouble(shift, ShiftTroubles.SHIFT_INCOMPLETED));
    }

    // 2. Verifica che gli slot siano tutti validi e setta PROBLEMS_ON_OTHER_SLOT su quelli da
    // invalidare a causa degli altri turni non rispettati
    List<ShiftTroubles> invalidatingTroubles = ShiftTroubles.invalidatingTroubles();

    List<PersonShiftDay> shiftsWithTroubles = shifts.stream()
        .filter(shift -> {
          return shift.hasOneOfErrors(invalidatingTroubles);
        }).collect(Collectors.toList());

    if (shiftsWithTroubles.isEmpty()) {
      shifts.forEach(shift -> fixShiftTrouble(shift, ShiftTroubles.PROBLEMS_ON_OTHER_SLOT));
    } else {
      shiftsWithTroubles
          .forEach(shift -> fixShiftTrouble(shift, ShiftTroubles.PROBLEMS_ON_OTHER_SLOT));

      shifts.removeAll(shiftsWithTroubles);
      shifts.forEach(shift -> setShiftTrouble(shift, ShiftTroubles.PROBLEMS_ON_OTHER_SLOT));
    }
  }

  /**
   * Effettua il calcolo dei minuti di turno maturati nel mese su un'attività per ogni persona in
   * turno
   *
   * @param activity attività sulla quale effettuare i calcoli
   * @param from data di inizio da cui calcolare
   * @param to data di fine
   * @return Restituisce una mappa con i minuti di turno maturati per ogni persona.
   */
  public Map<Person, Integer> calculateActivityShiftCompetences(ShiftType activity,
      LocalDate from, LocalDate to) {

    final Map<Person, Integer> shiftCompetences = new HashMap<>();
    
    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if (to.isAfter(today)) {
      lastDay = today;
    } else {
      lastDay = to;
    }
    involvedShiftWorkers(activity, from, to).forEach(person -> {

      int competences = calculatePersonShiftCompetencesInPeriod(activity, person, from, lastDay);
      shiftCompetences.put(person, competences);
    });

    return shiftCompetences;
  }


  /**
   * Recupera le eventuali competenze residue relative ai turnisti nel mese più recente rispetto
   * a quello specificato.
   *
   * @param people la lista dei turnisti.
   * @param yearMonth il mese a partire dal quale effettuare il controllo
   * @return una mappa contenente per ogni turnista i residui al mese più recente antecedente quello
   *     specificato.
   */
  public Map<Person, Integer> residualCompetences(List<Person> people, YearMonth yearMonth) {

    final Map<Person, Integer> residualShiftCompetences = new HashMap<>();

    people.forEach(person -> {
      int competences = getPersonResidualShiftCompetence(person, yearMonth);
      residualShiftCompetences.put(person, competences);
    });

    return residualShiftCompetences;
  }

  /**
   * @param activity attività di turno
   * @param from data di inizio
   * @param to data di fine
   * @return Una lista di persone che sono effettivamente coinvolte nei turni in un determinato
   *     periodo (Dipendenti con i turni schedulati in quel periodo).
   */
  public List<Person> involvedShiftWorkers(ShiftType activity, LocalDate from, LocalDate to) {
    return personShiftDayDao.byTypeInPeriod(from, to, activity, Optional.absent())
        .stream().map(shift -> shift.personShift.person).distinct().collect(Collectors.toList());
  }


  /**
   * @param activity attività di turno
   * @param person Persona sulla quale effettuare i calcoli
   * @param from data iniziale
   * @param to data finale
   * @return il numero di minuti di competenza maturati in base ai turni effettuati nel periodo
   *     selezionato (di norma serve calcolarli su un intero mese al massimo).
   */
  public int calculatePersonShiftCompetencesInPeriod(ShiftType activity, Person person,
      LocalDate from, LocalDate to) {

    // TODO: 08/06/17 Sicuramente vanno differenziati per tipo di competenza.....
    // c'è sono da capire qual'è la discriminante
    int shiftCompetences = 0;
    int paidMinutes = activity.shiftTimeTable.paidMinutes;
    final List<PersonShiftDay> shifts = personShiftDayDao
        .byTypeInPeriod(from, to, activity, Optional.of(person));

    // I conteggi funzionano nel caso lo stato dei turni sia aggiornato
    for (PersonShiftDay shift : shifts) {
      // Nessun errore sul turno
      if (!shift.hasOneOfErrors(ShiftTroubles.invalidatingTroubles())) {
        shiftCompetences += paidMinutes - (shift.exceededThresholds * SIXTY_MINUTES);
        log.info("Competenza calcolata sul turno di {}-{}: {}", 
            person.fullName(), shift.date, paidMinutes 
            - (shift.exceededThresholds * SIXTY_MINUTES));
      }
      
    }

    return shiftCompetences;
  }


  /**
   * @param activity attività di turno
   * @param person Persona
   * @param from data iniziale
   * @param to data finale
   * @return true se per tutti i turni dell'attività,persona e periodo specificati non contengono
   *     problemi, false altrimenti.
   */
  public List<ShiftTroubles> allValidShifts(ShiftType activity, 
      Person person, LocalDate from, LocalDate to) {

    return personShiftDayDao.byTypeInPeriod(from, to, activity, Optional.of(person)).stream()
        .map(shift -> shift.troubles)
        .flatMap(troubles ->   troubles.stream())
        .map(trouble -> trouble.cause)
        .distinct().collect(Collectors.toList());

  }

  /**
   * @param person Person della quale recuperare il residuo dei turni dai mesi precedenti
   * @param yearMonth Mese rispetto al quale verificare i residui
   * @return restituisce il residuo delle competenze di turno dal mese più recente antecedente
   *     quello specificato dal parametro yearMonth della persona richiesta.
   */
  public int getPersonResidualShiftCompetence(Person person, YearMonth yearMonth) {

    CompetenceCode shiftCode = competenceCodeDao.getCompetenceCodeByCode(codShift);

    Competence lastShiftCompetence = competenceDao.getLastPersonCompetenceInYear(person,
        yearMonth.getYear(), yearMonth.getMonthOfYear(), shiftCode);

    int residualCompetence = 0;

    if (lastShiftCompetence != null && lastShiftCompetence.exceededMins != null) {
      residualCompetence = lastShiftCompetence.exceededMins;
    }
    return residualCompetence;
  }

  /**
   * @param activity attività di turno
   * @param start data di inizio del periodo
   * @param end data di fine del periodo
   * @return La lista di tutte le persone abilitate su quell'attività nell'intervallo di tempo
   *     specificato.
   */
  public List<PersonShiftShiftType> shiftWorkers(ShiftType activity, LocalDate start,
      LocalDate end) {
    if (activity.isPersistent() && start != null && end != null) {
      return activity.personShiftShiftTypes.stream()
          .filter(personShiftShiftType -> personShiftShiftType.dateRange().isConnected(
              Range.closed(start, end)))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  //  /**
  //   * @param pairStampings la lista di coppie valide di entrata/uscita
  //   * @param begin l'ora di inizio del turno
  //   * @param end l'ora di fine del turno
  //   * @return la lista di coppie di timbrature di uscita/entrata appartenenti 
  //   * all'intervallo di turno
  //   * che vanno considerate per controllare se il tempo trascorso in pausa eccede quello previsto
  //   * dalla configurazione di turno.
  //   */
  //  private List<PairStamping> getBreakPairStampings(List<PairStamping> pairStampings,
  //      LocalTime begin, LocalTime end) {
  //    List<PairStamping> allGapPairs = Lists.newArrayList();
  //    PairStamping previous = null;
  //    for (PairStamping validPair : pairStampings) {
  //      if (previous != null) {
  //        if ((previous.second.stampType == null
  //            || previous.second.stampType.isGapLunchPairs())
  //            && (validPair.first.stampType == null
  //            || validPair.first.stampType.isGapLunchPairs())) {
  //
  //          allGapPairs.add(new PairStamping(previous.second, validPair.first));
  //        }
  //      }
  //      previous = validPair;
  //    }
  //    List<PairStamping> gapPairs = Lists.newArrayList();
  //    for (PairStamping gapPair : allGapPairs) {
  //      LocalTime first = gapPair.first.date.toLocalTime();
  //      LocalTime second = gapPair.second.date.toLocalTime();
  //
  //      boolean isInIntoBreakTime = !first.isBefore(begin) && !first.isAfter(end);
  //      boolean isOutIntoBreakTime = !second.isBefore(begin) && !second.isAfter(end);
  //
  //      if (!isInIntoBreakTime && !isOutIntoBreakTime) {
  //        if (second.isBefore(begin) || first.isAfter(end)) {
  //          continue;
  //        }
  //      }
  //
  //      LocalTime inForCompute = gapPair.first.date.toLocalTime();
  //      LocalTime outForCompute = gapPair.second.date.toLocalTime();
  //      if (!isInIntoBreakTime) {
  //        inForCompute = begin;
  //      }
  //      if (!isOutIntoBreakTime) {
  //        outForCompute = end;
  //      }
  //      int timeInPair = 0;
  //      timeInPair -= DateUtility.toMinute(inForCompute);
  //      timeInPair += DateUtility.toMinute(outForCompute);
  //      gapPair.timeInPair = timeInPair;
  //      gapPairs.add(gapPair);
  //    }
  //    return gapPairs;
  //  }

  /**
   * Effettua i calcoli delle competenze relative ai turni sulle attività approvate per le persone
   * coinvolte in una certa attività e un determinato mese.   *
   * Da utilizzare in seguito ad ogni approvazione/disapprovazione dei turni.
   *
   * @param shiftTypeMonth lo stato dell'attività di turno in un determinato mese.
   */
  public void assignShiftCompetences(ShiftTypeMonth shiftTypeMonth) {

    Verify.verifyNotNull(shiftTypeMonth);

    final LocalDate monthBegin = shiftTypeMonth.yearMonth.toLocalDate(1);
    final LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    final int year = shiftTypeMonth.yearMonth.getYear();
    final int month = shiftTypeMonth.yearMonth.getMonthOfYear();
    
    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if (monthEnd.isAfter(today)) {
      lastDay = today;
    } else {
      lastDay = monthEnd;
    }
    
    final List<Person> involvedShiftPeople = involvedShiftWorkers(shiftTypeMonth.shiftType,
        monthBegin, monthEnd);

    Map<Person, Integer> totalPeopleCompetences = new HashMap<>();
    
    // Recupero tutte le attività approvate in quel mese
    shiftTypeMonthDao.approvedInMonthRelatedWith(shiftTypeMonth.yearMonth, involvedShiftPeople)
        .forEach(monthStatus -> {
          // Per ogni attività calcolo le competenze di ogni persona coinvolta
          involvedShiftPeople.forEach(person -> {
            int activityCompetence = calculatePersonShiftCompetencesInPeriod(monthStatus.shiftType,
                person, monthBegin, lastDay);
            // Somma algebrica delle competenze delle persone derivanti da ogni attività sulla
            // quale ha svolto i turni
            totalPeopleCompetences.merge(person, activityCompetence, (previousValue, newValue) -> newValue + previousValue);
          });
        });

    CompetenceCode shiftCode = competenceCodeDao.getCompetenceCodeByCode(codShift);

    involvedShiftPeople.forEach(person -> {

      // Verifico che per le person coinvolte ci siano o no eventuali residui dai mesi precedenti
      int lastShiftCompetence = getPersonResidualShiftCompetence(person, shiftTypeMonth.yearMonth);
      Integer calculatedCompetences = totalPeopleCompetences.get(person);

      // TODO: 12/06/17 sicuramente andranno differenziate tra T1 e T2
      int totalShiftMinutes;
      if (calculatedCompetences != null) {
        totalShiftMinutes = calculatedCompetences + lastShiftCompetence;
      } else {
        totalShiftMinutes = lastShiftCompetence;
      }

      Optional<Competence> shiftCompetence = competenceDao
          .getCompetence(person, year, month, shiftCode);

      Competence newCompetence = shiftCompetence.or(new Competence(person, shiftCode, year, month));
      newCompetence.valueApproved = totalShiftMinutes / 60;
      newCompetence.exceededMins = totalShiftMinutes % 60;
      // newCompetence.valueRequested = ; e qui cosa ci va?

      newCompetence.save();

      log.info("Salvata {}", newCompetence);
    });

  }

  /**
   * salva il personShiftDay ed effettua i ricalcoli.
   * @param personShiftDay il personshiftDay da salvare
   */
  public void save(PersonShiftDay personShiftDay) {

    Verify.verifyNotNull(personShiftDay).save();
    recalculate(personShiftDay);
  }
  
  /**
   * cancella il personShiftDay.
   * @param personShiftDay il personShiftDay da cancellare
   */
  public void delete(PersonShiftDay personShiftDay) {
    
    Verify.verifyNotNull(personShiftDay).delete();
    recalculate(personShiftDay);
  }
  
  private void recalculate(PersonShiftDay personShiftDay) {

    final ShiftType shiftType = personShiftDay.shiftType;

    // Aggiornamento dello ShiftTypeMonth
    if (shiftType != null) {

      // Ricalcoli sul turno
      if (personShiftDay.isPersistent()) {
        checkShiftValid(personShiftDay);
      }
           
      // Ricalcoli sui giorni coinvolti dalle modifiche
      checkShiftDayValid(personShiftDay.date, shiftType);
      
      /*
       *  Recupera la data precedente dallo storico e verifica se c'è stato un 
       *  cambio di date sul turno. In tal caso effettua il ricalcolo anche 
       *  sul giorno precedente (spostamento di un turno da un giorno all'altro)
       */
      HistoricalDao.lastRevisionsOf(PersonShiftDay.class, personShiftDay.id)
          .stream().limit(1).map(historyValue -> {
            PersonShiftDay pd = (PersonShiftDay) historyValue.value;
            return pd.date;
          }).filter(Objects::nonNull).distinct()
          .forEach(localDate -> {
            if (!localDate.equals(personShiftDay.date)) {
              checkShiftDayValid(localDate, shiftType);
            }
          });

      // Aggiornamento del relativo ShiftTypeMonth (per incrementare il campo version)
      ShiftTypeMonth newStatus = shiftType.monthStatusByDate(personShiftDay.date)
          .orElse(new ShiftTypeMonth());

      if (newStatus.shiftType != null) {
        newStatus.updatedAt = LocalDateTime.now();
      } else {
        newStatus.yearMonth = new YearMonth(personShiftDay.date);
        newStatus.shiftType = shiftType;
      }
      newStatus.save();

    }
  }
  
  

}
