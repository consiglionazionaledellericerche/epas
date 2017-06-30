package manager;


import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import controllers.Security;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;
import dao.ShiftTypeMonthDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;
import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.DateUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import models.ShiftTimeTable;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.User;
import models.enumerate.ShiftTroubles;
import org.joda.time.LocalDate;
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
  private final IWrapperFactory wrapperFactory;
  private final ShiftTypeMonthDao shiftTypeMonthDao;


  @Inject
  public ShiftManager2(PersonDayManager personDayManager, PersonShiftDayDao personShiftDayDao,
      PersonDayDao personDayDao, ShiftDao shiftDao,
      CompetenceUtility competenceUtility, CompetenceCodeDao competenceCodeDao,
      CompetenceDao competenceDao, IWrapperFactory wrapperFactory,
      PersonMonthRecapDao personMonthRecapDao, ShiftTypeMonthDao shiftTypeMonthDao) {

    this.personDayManager = personDayManager;
    this.personShiftDayDao = personShiftDayDao;
    this.personDayDao = personDayDao;
    this.shiftDao = shiftDao;
    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;
    this.wrapperFactory = wrapperFactory;
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
    final ShiftTimeTable timeTable = Verify.verifyNotNull(shiftType.shiftTimeTable);
    final LocalDate today = LocalDate.now();

    log.debug("Ricalcoli sul turno di {} in data {}", personShiftDay.personShift.person,
        personShiftDay.date);

    Optional<PersonDay> optionalPersonDay =
        personDayDao.getPersonDay(personShiftDay.personShift.person, personShiftDay.date);

    if (optionalPersonDay.isPresent()) {
      final PersonDay personDay = optionalPersonDay.get();
      // controlla se non sono nel futuro ed è un giorno valido
      IWrapperPersonDay wrPersonDay = wrapperFactory.create(personDay);

      // 1. Controlli sulle assenze
      if (personDayManager.isAllDayAbsences(personDay)) {
        setShiftTrouble(personShiftDay, ShiftTroubles.PERSON_IS_ABSENT);
      } else {
        fixShiftTrouble(personShiftDay, ShiftTroubles.PERSON_IS_ABSENT);
      }

      final LocalTime begin;
      final LocalTime end;
      // Nelle date passate posso effettuare i controlli sul tempo a lavoro
      if (personShiftDay.date.isBefore(today)) {

        switch (personShiftDay.shiftSlot) {
          case MORNING:
            begin = timeTable.startMorning;
            end = timeTable.endMorning;
            break;

          case AFTERNOON:
            begin = timeTable.startAfternoon;
            end = timeTable.endAfternoon;
            break;
          //TODO: case EVENING??
          default:
            begin = null;
            end = null;
        }
        
        /*
         * TODO: bisogna inserire i controlli sulle timbrature effettuate nel caso di pausa pranzo presente all'interno
         * del turno. Inoltre aggiungere il controllo sul maxToleranceAllowed, ovvero il caso relativo ai turni IIT (...)
         */
        

        // 2.Controlli sulle timbrature...
        List<PairStamping> validPairStampings = personDayManager
            .getValidPairStampings(personDay.stampings);

        // Le coppie che si sovrappongono anche solo per un minuto con la finestra del turno
        List<PairStamping> relatedPairs = validPairStampings.stream()
            .filter(pair -> Range.closed(begin, end).isConnected(
                Range.closed(pair.first.date.toLocalTime(), pair.second.date.toLocalTime())))
            .collect(Collectors.toList());

        // Se ci sono timbrature valide..
        if (begin != null && end != null && !relatedPairs.isEmpty()) {

          // Conteggio dei minuti di pausa fatti durante il turno
          Iterator<PairStamping> iterator = relatedPairs.iterator();
          PairStamping previousPair = iterator.next();
          PairStamping nextPair;
          int totalBreakMinutes = 0;

          while (iterator.hasNext()) {
            nextPair = iterator.next();
            totalBreakMinutes += DateUtility.toMinute(nextPair.first.date) - DateUtility
                .toMinute(previousPair.second.date);
            previousPair = nextPair;
          }

          // controlli sull'eventuale pausa in turno abilitata...
//          if (shiftType.breakInShiftEnabled) {
//            if (totalBreakMinutes > shiftType.breakInShift) {
//              setShiftTrouble(personShiftDay, ShiftTroubles.EXCEEDED_BREAKTIME);
//            } else {
//              fixShiftTrouble(personShiftDay, ShiftTroubles.EXCEEDED_BREAKTIME);
//            }
//          } else {
//            if (totalBreakMinutes > 0) {
//              setShiftTrouble(personShiftDay, ShiftTroubles.EXCEEDED_BREAKTIME);
//            } else {
//              fixShiftTrouble(personShiftDay, ShiftTroubles.EXCEEDED_BREAKTIME);
//            }
//          }

          LocalTime beginWithTolerance = begin.plusMinutes(shiftType.entranceTolerance);
          LocalTime endWithTolerance = end.minusMinutes(shiftType.exitTolerance);
//          final LocalTime hourToleranceThreshold = begin.plusMinutes(shiftType.hourTolerance);

          int timeInShift = personDayManager.workingMinutes(validPairStampings, begin, end);

          //verifico se la tolleranza oraria è presente...
//          if (shiftType.hourTolerance > 0) {
//
//            //la timbratura di ingresso è compresa tra la tolleranza e la tolleranza oraria
//            // sul turno e il tempo a lavoro è sufficiente per coprire il tempo minimo di turno
//            if (Range.closed(beginWithTolerance, hourToleranceThreshold)
//                .contains(relatedPairs.get(0).first.date.toLocalTime()) &&
//                Range.closed(timeTable.paidMinutes - shiftType.hourTolerance,
//                    timeTable.paidMinutes).contains(timeInShift)) {
//              setShiftTrouble(personShiftDay, ShiftTroubles.NOT_COMPLETED_SHIFT);
//            } else {
//              fixShiftTrouble(personShiftDay, ShiftTroubles.NOT_COMPLETED_SHIFT);
//            }
//
//            //l'ingresso è successivo alla soglia di tolleranza sull'ingresso in turno
//            if (relatedPairs.get(0).first.date.toLocalTime().isAfter(hourToleranceThreshold)) {
//              setShiftTrouble(personShiftDay, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
//            } else {
//              fixShiftTrouble(personShiftDay, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
//            }
//          } else {
//            // la prima timbratura è oltre la tolleranza in ingresso o il tempo a lavoro
//            // non è sufficiente
//            if (timeInShift < timeTable.paidMinutes) {
//              setShiftTrouble(personShiftDay, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
//            } else {
//              fixShiftTrouble(personShiftDay, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
//            }
//          }
//
//          //l'uscita è precedente alla soglia di tolleranza sull'uscita dal turno
//          if (relatedPairs.get(relatedPairs.size() - 1).second.date.toLocalTime()
//              .isBefore(endWithTolerance)
//              || shiftType.hourTolerance < shiftType.entranceTolerance 
//              && relatedPairs.get(0).first.date.toLocalTime().isAfter(beginWithTolerance)) {
//            setShiftTrouble(personShiftDay, ShiftTroubles.OUT_OF_STAMPING_TOLERANCE);
//          } else {
//            fixShiftTrouble(personShiftDay, ShiftTroubles.OUT_OF_STAMPING_TOLERANCE);
//          }
        }
      } else {
        // Non ci sono abbastanza timbrature o non è possibile identificare lo slot del turno
        setShiftTrouble(personShiftDay, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
      }

    } else {
      // E' una data passata senza nessuna informazione per quel giorno
      if (personShiftDay.date.isBefore(today)) {
        setShiftTrouble(personShiftDay, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
      } else {
        fixShiftTrouble(personShiftDay, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
      }
    }
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
    ImmutableList<ShiftTroubles> invalidatingTroubles = ImmutableList.of(
        ShiftTroubles.OUT_OF_STAMPING_TOLERANCE, ShiftTroubles.NOT_ENOUGH_WORKING_TIME,
        ShiftTroubles.EXCEEDED_BREAKTIME, ShiftTroubles.PERSON_IS_ABSENT);

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

    final LocalDate today = LocalDate.now();
    final LocalDate lastDay;

    if (to.isAfter(today)) {
      lastDay = today;
    } else {
      lastDay = to;
    }

    final Map<Person, Integer> shiftCompetences = new HashMap<>();

    involvedShiftWorkers(activity, from, lastDay).forEach(person -> {

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
   * specificato.
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
   * periodo (Dipendenti con i turni schedulati in quel periodo).
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
    final List<PersonShiftDay> shifts = personShiftDayDao
        .byTypeInPeriod(from, to, activity, Optional.of(person));

    // I conteggi funzionano nel caso lo stato dei turni sia aggiornato
    for (PersonShiftDay shift : shifts) {
      // Nessun errore sul turno
      if (shift.troubles.isEmpty()) {
        shiftCompetences += shift.shiftType.shiftTimeTable.paidMinutes;
      } else if (shift.troubles.size() == 1 && shift.hasError(ShiftTroubles.NOT_COMPLETED_SHIFT)) {
        // Il turno vale comunque ma con un'ora in meno
        shiftCompetences += shift.shiftType.shiftTimeTable.paidMinutes - SIXTY_MINUTES;
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
  public boolean allValidShifts(ShiftType activity, Person person, LocalDate from, LocalDate to) {

    final List<PersonShiftDay> shifts = personShiftDayDao
        .byTypeInPeriod(from, to, activity, Optional.of(person));

    return shifts.stream().allMatch(personShiftDay -> personShiftDay.troubles.isEmpty());
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
//   * @return la lista di coppie di timbrature di uscita/entrata appartenenti all'intervallo di turno
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

    final List<Person> involvedShiftPeople = involvedShiftWorkers(shiftTypeMonth.shiftType,
        monthBegin, monthEnd);

    Map<Person, Integer> totalPeopleCompetences = new HashMap<>();
    Map<Person, Integer> residualCompetences = new HashMap<>();

    // Recupero tutte le attività approvate in quel mese
    shiftTypeMonthDao.approvedInMonthRelatedWith(shiftTypeMonth.yearMonth, involvedShiftPeople)
        .forEach(monthStatus -> {
          // Per ogni attività calcolo le competenze di ogni persona coinvolta
          involvedShiftPeople.forEach(person -> {
            int activityCompetence = calculatePersonShiftCompetencesInPeriod(monthStatus.shiftType,
                person, monthBegin, monthEnd);
            // Somma algebrica delle competenze delle persone derivanti da ogni attività sulla
            // quale ha svolto i turni
            totalPeopleCompetences.merge(person, activityCompetence, (a, b) -> b + a);
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

}
