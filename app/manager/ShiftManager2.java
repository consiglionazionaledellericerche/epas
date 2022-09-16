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

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import controllers.Calendar.ShiftPeriod;
import controllers.Security;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.GeneralSettingDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;
import dao.ShiftTypeMonthDao;
import dao.history.HistoricalDao;
import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.TimeInterval;
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
import models.GeneralSetting;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonDay;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftDayInTrouble;
import models.PersonShiftShiftType;
import models.Role;
import models.ShiftTimeTable;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.User;
import models.enumerate.CalculationType;
import models.enumerate.PaymentType;
import models.enumerate.ShiftSlot;
import models.enumerate.ShiftTroubles;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.YearMonth;
import play.i18n.Messages;


/**
 * Gestiore delle operazioni sui turni ePAS.
 *
 * @author Arianna Del Soldato
 */
@Slf4j
public class ShiftManager2 {

  private static final String codShiftNight = "T2";
  private static final String codShiftHolyday = "T3";
  private static final String codShift = "T1";
  private static final int SIXTY_MINUTES = 60;
  private static final long MAX_QUANTITY_IN_SLOT = 2;
  private static final long MAX_QUANTITY = 3;

  private final PersonDayManager personDayManager;
  private final PersonShiftDayDao personShiftDayDao;
  private final PersonDayDao personDayDao;
  private final ShiftDao shiftDao;
  private final CompetenceCodeDao competenceCodeDao;
  private final CompetenceDao competenceDao;
  private final ShiftTypeMonthDao shiftTypeMonthDao;
  private final GeneralSettingDao generalSettingDao;


  /**
   * Injector.
   *
   * @param personDayManager il personDayManager
   * @param personShiftDayDao il dao sul personShiftDay
   * @param personDayDao il dao sul personDay
   * @param shiftDao il dao sui turni
   * @param competenceUtility il manager per le competenze
   * @param competenceCodeDao il dao per i competenceCode
   * @param competenceDao il dao per le competenze
   * @param personMonthRecapDao il dao sui residui mensili
   * @param shiftTypeMonthDao il dao sulle attribuzioni mensili dei turni
   * @param generalSettingDao il dao sui parametri generali
   */
  @Inject
  public ShiftManager2(PersonDayManager personDayManager, PersonShiftDayDao personShiftDayDao,
      PersonDayDao personDayDao, ShiftDao shiftDao, CompetenceUtility competenceUtility,
      CompetenceCodeDao competenceCodeDao, CompetenceDao competenceDao,
      PersonMonthRecapDao personMonthRecapDao, ShiftTypeMonthDao shiftTypeMonthDao,
      GeneralSettingDao generalSettingDao) {

    this.personDayManager = personDayManager;
    this.personShiftDayDao = personShiftDayDao;
    this.personDayDao = personDayDao;
    this.shiftDao = shiftDao;
    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;
    this.shiftTypeMonthDao = shiftTypeMonthDao;
    this.generalSettingDao = generalSettingDao;
  }

  /**
   * Metodo che ritorna la lista delle attività associate all'utente che ne fa richiesta.
   *
   * @return la lista delle attività associate all'utente che ne fa richiesta.
   */
  public List<ShiftType> getUserActivities() {
    List<ShiftType> activities = Lists.newArrayList();
    User currentUser = Security.getUser().get();
    Person person = currentUser.person;
    if (person != null) {
      if (!person.shiftCategories.isEmpty()) {
        activities.addAll(person.shiftCategories.stream().filter(st -> !st.disabled)
            .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
            .sorted(Comparator.comparing(o -> o.type))
            .collect(Collectors.toList()));

      }
      if (!person.categories.isEmpty()) {
        activities.addAll(person.categories.stream().filter(st -> !st.disabled)
            .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
            .sorted(Comparator.comparing(o -> o.type))
            .collect(Collectors.toList()));
      }
      if (!person.personShifts.isEmpty()) {
        activities.addAll(person.personShifts.stream().filter(ps -> !ps.disabled)
            .filter(ps -> !ps.beginDate.isAfter(LocalDate.now()) 
                && (ps.endDate == null || !ps.endDate.isBefore(LocalDate.now())))
            .flatMap(ps -> ps.personShiftShiftTypes.stream())
            .filter(psst -> psst.endDate == null || psst.endDate.isAfter(LocalDate.now()))
            .map(psst -> psst.shiftType).filter(sc -> !sc.shiftCategories.disabled)
            .sorted(Comparator.comparing(o -> o.type))
            .collect(Collectors.toList()));
      }
      if (currentUser.hasRoles(Role.PERSONNEL_ADMIN)) {
        activities.addAll(currentUser.usersRolesOffices.stream()
            .flatMap(uro -> uro.office.shiftCategories.stream().filter(st -> !st.disabled)
                .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
                .sorted(Comparator.comparing(o -> o.type)))
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
   * un determinato giorno: assenza o missione.
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
   * Popola la tabella PersonShift andando a cercare nel db tutte le persone che son già
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
      if (personShiftDayDao.getPersonShiftByPerson(item.person, LocalDate.now()) == null) {
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
   * Verifica se un turno puo' essere inserito senza violare le regole dei turni.
   *
   * @param personShiftDay il personShiftDay da inserire
   * @return l'eventuale stringa contenente l'errore evidenziato in fase di inserimento del turno.
   */
  public Optional<String> shiftPermitted(PersonShiftDay personShiftDay) {

    /*
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
    // Condizionato per INAF con parametro generalSetting
    GeneralSetting setting = generalSettingDao.generalSetting();
    if (setting != null && setting.enableUniqueDailyShift) {
      final Optional<PersonShiftDay> personShift = personShiftDayDao
          .byPersonAndDate(personShiftDay.personShift.person, personShiftDay.date);

      if (personShift.isPresent()) {
        return Optional.of(Messages.get("shift.alreadyInShift", personShift.get().shiftType));
      }
    }
    
    // Controllo se sono assente il giorno di turno
    final Optional<PersonDay> personDay = personDayDao
        .getPersonDay(personShiftDay.personShift.person, personShiftDay.date);

    if (personDay.isPresent() && personDayManager.isAllDayAbsences(personDay.get())) {
      return Optional.of(Messages.get("shift.absenceInDay"));
    }

    //Controllo che sia abilitato il turno festivo
    CompetenceCode holidayCode = competenceCodeDao.getCompetenceCodeByCode(codShiftHolyday);

    final boolean isHolidayShiftEnabled = 
        personShiftDay.personShift.person.personCompetenceCodes
        .stream().anyMatch(pcc -> pcc.competenceCode.equals(holidayCode) 
            && !pcc.beginDate.isAfter(personShiftDay.date));

    
    if (setting != null) {
      if (personDayManager.isHoliday(personShiftDay.personShift.person, personShiftDay.date, 
          setting.saturdayHolidayShift) && !isHolidayShiftEnabled) {          
        return Optional.of(Messages.get("shift.holidayShiftNotEnabled"));
      } 

    } else {
      log.warn("Non sono correttamente configurati i parametri generali. "
          + "Controllo sul sabato festivo/feriale non praticabile");
    }


    List<PersonShiftDay> list = personShiftDayDao
        .byTypeInPeriod(personShiftDay.date, personShiftDay.date,
            personShiftDay.shiftType, Optional.absent());

    //Controllo se è abilitata la disparità di slot nell'attività di turno
    if (!personShiftDay.shiftType.allowUnpairSlots) {
      for (PersonShiftDay registeredDay : list) {
        if (personShiftDay.shiftType.organizaionShiftTimeTable != null) {
          //controlla che il turno in quello slot sia già stato assegnato ad un'altra persona
          if (registeredDay.organizationShiftSlot.equals(personShiftDay.organizationShiftSlot)) {
            return Optional.of(Messages
                .get("shift.slotAlreadyAssigned", registeredDay.personShift.person.fullName()));
          }
        } else {
          if (registeredDay.shiftSlot.equals(personShiftDay.shiftSlot)) {
            return Optional.of(Messages
                .get("shift.slotAlreadyAssigned", registeredDay.personShift.person.fullName()));
          }
        }

      }
    } else {
      long count = 1;
      long sum = 0;
      if (personShiftDay.shiftType.organizaionShiftTimeTable != null) {
        sum = list.stream()
            .filter(psd -> psd.organizationShiftSlot
                .equals(personShiftDay.organizationShiftSlot)).count();
      } else {
        sum = list.stream().filter(psd -> psd.shiftSlot.equals(personShiftDay.shiftSlot)).count();
      }

      if (sum + count > MAX_QUANTITY_IN_SLOT) {
        return Optional.of(Messages.get("shift.maxQuantityInSlot", personShiftDay.shiftType.type));
      }
      long total = list.stream().count();
      if (total + count > MAX_QUANTITY) {
        return Optional.of(Messages.get("shift.maxQuantity", personShiftDay.shiftType.type));
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

      if (personShiftDay.date.isBefore(today)) {
        fixShiftTrouble(personShiftDay, ShiftTroubles.FUTURE_DAY);        
      }

      // Nelle date passate posso effettuare i controlli sul tempo a lavoro
      if (personShiftDay.date.isBefore(today)) {

        final LocalTime slotBegin;
        final LocalTime slotEnd;

        if (personShiftDay.organizationShiftSlot != null) {
          slotBegin = personShiftDay.organizationShiftSlot.beginSlot;
          slotEnd = personShiftDay.organizationShiftSlot.endSlot;

        } else {
          slotBegin = personShiftDay.slotBegin();
          slotEnd = personShiftDay.slotEnd();
        }
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
          final LocalTime exitMaxThreshold = slotEnd.minusMinutes(shiftType.exitMaxTolerance);
          final LocalTime exitThreshold = slotEnd.minusMinutes(shiftType.exitTolerance);

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
          final LocalTime lunchBreakStart;
          final LocalTime lunchBreakEnd;
          if (personShiftDay.organizationShiftSlot != null) {
            lunchBreakStart = personShiftDay.organizationShiftSlot.beginMealSlot;
            lunchBreakEnd = personShiftDay.organizationShiftSlot.endMealSlot;
          } else {
            lunchBreakStart = personShiftDay.lunchTimeBegin();
            lunchBreakEnd = personShiftDay.lunchTimeEnd();
          }

          if (lunchBreakStart != null && lunchBreakEnd != null) {
            if (Range.open(slotBegin, slotEnd)
                .encloses(Range.closed(lunchBreakStart, lunchBreakEnd))) {
              rangeSet.add(Range.closed(lunchBreakStart, lunchBreakEnd));
            }
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

    // 1. Controllo che siano coperti tutti gli slot solo se stiamo sulla configurazione CNR 
    // o in una configurazione non CNR ma in cui gli slot devono essere tutti coperti
    if (activity.shiftTimeTable != null || (activity.organizaionShiftTimeTable != null 
        && activity.organizaionShiftTimeTable.considerEverySlot)) {
      long slotNumber = 0;
      if (activity.organizaionShiftTimeTable != null) {
        slotNumber = activity.organizaionShiftTimeTable.slotCount();
        if (slotNumber > shifts.size()) {
          shifts.forEach(shift -> setShiftTrouble(shift, ShiftTroubles.SHIFT_INCOMPLETED));
        } else {
          shifts.forEach(shift -> fixShiftTrouble(shift, ShiftTroubles.SHIFT_INCOMPLETED));
        }

      } else {
        slotNumber = activity.shiftTimeTable.slotCount();
        if (slotNumber > shifts.size()) {
          shifts.forEach(shift -> setShiftTrouble(shift, ShiftTroubles.SHIFT_INCOMPLETED));
        } else {
          shifts.forEach(shift -> fixShiftTrouble(shift, ShiftTroubles.SHIFT_INCOMPLETED));
        }
      }  
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
   * turno.
   *
   * @param activity attività sulla quale effettuare i calcoli
   * @param from data di inizio da cui calcolare
   * @param to data di fine
   * @return Restituisce una mappa con i minuti di turno maturati per ogni persona.
   */
  public Map<Person, Integer> calculateActivityShiftCompetences(ShiftType activity,
      LocalDate from, LocalDate to, ShiftPeriod type) {

    final Map<Person, Integer> shiftCompetences = new HashMap<>();

    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if (to.isAfter(today)) {
      lastDay = today;
    } else {
      lastDay = to;
    }
    involvedShiftWorkers(activity, from, to).forEach(person -> {
      int competences = 0;
      competences = 
          calculatePersonShiftCompetencesInPeriod(activity, person, from, lastDay, type);

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
   * Metodo che ritorna la lista delle persone coinvolte nei turni in un determinato periodo.
   *
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
   * Metodo che calcola i minuti di turno maturati in base ai turni effettuati nel periodo.
   *
   * @param activity attività di turno
   * @param person Persona sulla quale effettuare i calcoli
   * @param from data iniziale
   * @param to data finale
   * @return il numero di minuti di competenza maturati in base ai turni effettuati nel periodo
   *     selezionato (di norma serve calcolarli su un intero mese al massimo).
   */
  public int calculatePersonShiftCompetencesInPeriod(ShiftType activity, Person person,
      LocalDate from, LocalDate to, ShiftPeriod type) {

    int shiftCompetences = 0;

    int paidMinutes = 0;
    if (activity.shiftTimeTable != null) {
      paidMinutes = activity.shiftTimeTable.paidMinutes;
    }

    final List<PersonShiftDay> shifts = personShiftDayDao
        .byTypeInPeriod(from, to, activity, Optional.of(person));
    List<PersonShiftDay> list = Lists.newArrayList();
    //Cerco gli intervalli orari per stabilire a quale competenza assegnare la quantità 
    //di ore di turno
    Optional<TimeInterval> timeInterval = null;
    Optional<TimeInterval> timeInterval2 = null;
    TimeInterval daily = null;
    TimeInterval night = null;
    TimeInterval beforeDawn = null;
    GeneralSetting setting = generalSettingDao.generalSetting();
    if (setting != null) {
      //Costruisco l'intervallo temporale per il turno diurno e il turno notturno
      daily = new TimeInterval(convertFromString(setting.startDailyShift), 
          convertFromString(setting.endDailyShift));

      night = new TimeInterval(convertFromString(setting.startNightlyShift), 
          new LocalTime(23, 59));
      beforeDawn = new TimeInterval(
          new LocalTime(0, 0), convertFromString(setting.endNightlyShift));

    } else {
      log.warn("Manca il general setting relativo all'ente. Occore definirlo!!!");
      return 0;
    }
    //Proviamo a filtrarli...
    switch (type) {
      case daily:
        timeInterval = Optional.fromNullable(daily);
        timeInterval2 = Optional.<TimeInterval>absent();
        list = shifts.stream().filter(day -> { 
          return !personDayManager.isHoliday(day.personShift.person, day.date, 
              setting.saturdayHolidayShift);
        }).collect(Collectors.toList());

        break;
      case nightly:
        timeInterval = Optional.fromNullable(night);
        timeInterval2 = Optional.fromNullable(beforeDawn);
        list = shifts.stream().filter(day -> { 
          return !personDayManager.isHoliday(day.personShift.person, day.date, 
              setting.saturdayHolidayShift);
        }).collect(Collectors.toList());

        break;
      case holiday:
        if (setting.holidayShiftInNightToo) {
          timeInterval = Optional.of(new TimeInterval(new LocalTime(0, 0), new LocalTime(23, 59)));
          timeInterval2 = Optional.<TimeInterval>absent();
        } else {
          timeInterval = Optional.fromNullable(daily);
          timeInterval2 = Optional.<TimeInterval>absent();
        }
        
        list = shifts.stream().filter(day -> { 
          return personDayManager.isHoliday(day.personShift.person, day.date, 
              setting.saturdayHolidayShift);
        }).collect(Collectors.toList());

        break;
      default:
        break;
    }
    List<ShiftTroubles> troubles = Lists.newArrayList();
    if (activity.organizaionShiftTimeTable != null
        && !activity.organizaionShiftTimeTable.considerEverySlot) {
      troubles.addAll(ShiftTroubles.warningTroubles());
    } else {
      troubles.addAll(ShiftTroubles.invalidatingTroubles());
    }
    // I conteggi funzionano nel caso lo stato dei turni sia aggiornato
    for (PersonShiftDay shift : list) {
      // Nessun errore sul turno
      if (!shift.hasOneOfErrors(troubles)) {
        PersonDay pd = personDayManager
            .getOrCreateAndPersistPersonDay(shift.personShift.person, shift.date);
        if (shift.organizationShiftSlot != null) {

          if (shift.organizationShiftSlot.shiftTimeTable.calculationType
              .equals(CalculationType.percentage)) {
            //FIXME: che succede se siamo nel festivo e ci sono timbrature notturne?
            int quantity = isIntervalTotallyInSlot(pd, shift, timeInterval)
                - (shift.exceededThresholds * SIXTY_MINUTES);
            if (quantity < 0) {
              quantity = 0;
            }
            shiftCompetences = shiftCompetences + quantity;
            if (timeInterval2.isPresent()) {
              shiftCompetences += isIntervalTotallyInSlot(pd, shift, timeInterval2);
            }

          } else {    
            if (shift.organizationShiftSlot.paymentType == PaymentType.SPLIT_CALCULATION) {
              shiftCompetences += quantityCountForShift(shift, pd, timeInterval);
            } else {
              shiftCompetences += shift.organizationShiftSlot.minutesPaid 
                  - (shift.exceededThresholds * SIXTY_MINUTES);
            }
          }
        } else {
          shiftCompetences += paidMinutes - (shift.exceededThresholds * SIXTY_MINUTES);
        }
        log.info("Competenza calcolata sul turno di {}-{}: {}", 
            person.fullName(), shift.date, shiftCompetences 
            - (shift.exceededThresholds * SIXTY_MINUTES));
      }
    }

    if (setting.roundingShiftQuantity) {
      shiftCompetences = roundingShift(shiftCompetences);
    } 
    return shiftCompetences;
  }

  /**
   * Metodo di utilità che arrotonda il quantitativo di ore di turno all'ora superiore
   * o inferiore a seconda che la divisione % 60 del quantitativo sia maggiore o minore di mezz'ora.
   *
   * @param shiftCompetence la quantità di ore di turno (in minuti)
   * @return l'arrotondamento all'ora superiore o inferiore della quantità in minuti 
   *     di ore di turno.
   */
  private int roundingShift(int shiftCompetence) {
    if (shiftCompetence == 0) {
      return 0;
    }
    if (shiftCompetence % 60 > DateTimeConstants.MINUTES_PER_HOUR / 2) {
      shiftCompetence = shiftCompetence 
          + (DateTimeConstants.MINUTES_PER_HOUR - shiftCompetence % 60);
    } else {
      shiftCompetence = shiftCompetence - shiftCompetence % 60;
    }
    return shiftCompetence;
  }

  /**
   * Metodo che ritorna la quantità di minuti lavorata all'interno della fascia di turno.
   *
   * @param psd il personShiftDday del giorno
   * @param pd il personDay del giorno
   * @param timeInterval (optional) l'intervallo in cui cercare il turno di un certo tipo
   * @return la quantità in minuti lavorata all'interno della fascia oraria di turno.
   */
  private int quantityCountForShift(PersonShiftDay psd, PersonDay pd, 
      Optional<TimeInterval> timeInterval) {
    int timeIntersection = 0;
    TimeInterval interval = null;
    List<PairStamping> pairList = personDayManager.getValidPairStampings(pd.stampings);
    for (PairStamping pair : pairList) {
      interval = DateUtility.intervalIntersection(
          new TimeInterval(psd.organizationShiftSlot.beginSlot, psd.organizationShiftSlot.endSlot), 
          new TimeInterval(pair.first.date.toLocalTime(), 
              pair.second.date.toLocalTime()));
      if (interval != null) {
        if (timeInterval.isPresent()) {
          TimeInterval intersection = 
              DateUtility.intervalIntersection(timeInterval.get(), interval);
          if (intersection == null) {
            return 0;
          }
          timeIntersection += intersection.minutesInInterval();
        } else {
          timeIntersection += interval.minutesInInterval();
        }        
      }
    }    

    return timeIntersection;
  }

  /**
   * Metodo che ritorna la quantità di minuti da pagare in un certo slot (diurno/notturno).
   *
   * @param pd il personday di un certo giorno
   * @param psd il personshiftday di un certo giorno
   * @param interval l'eventuale intervallo di validità dello slot (diurno/notturno)
   * @return la quantità in minuti da pagare nello specifico slot di turno.
   */
  private int isIntervalTotallyInSlot(PersonDay pd, PersonShiftDay psd, 
      Optional<TimeInterval> interval) {
    if (interval.isPresent()) {
      int quantity = quantityCountForShift(psd, pd, interval);
      if (quantity < 0) {
        return 0;
      }
      if (quantity < psd.organizationShiftSlot.minutesPaid) {
        return quantity;
      }
    }
    return psd.organizationShiftSlot.minutesPaid;
  }

  /**
   * Metodo che converte un'orario in formato stringa in un orario in formato LocalTime.
   *
   * @param str la stringa da convertire
   * @return il LocalTime generato a partire dalla stringa passata come parametro.
   */
  private LocalTime convertFromString(String str) {
    final String splitter = ":";
    String[] s = str.split(splitter);
    LocalTime time = new LocalTime(Integer.valueOf(s[0]), Integer.valueOf(s[1]));
    return time;
  }

  /**
   * Metodo che ritorna la lista degli ShiftTroubles appartenenti a una persona sull'attività
   * di turno in un certo periodo.
   *
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
   * Metodo che restituisce il residuo della competenza turno, se ne è avanzato, 
   * dal mese precedente.
   *
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
   * Metodo che ritorna la lista di tutte le persone abilitate sull'attività nell'intervallo
   * di tempo specificato.
   *
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
    Map<Person, Integer> totalHolidayPeopleCompetences = new HashMap<>();
    Map<Person, Integer> totalNightlyPeopleCompetences = new HashMap<>();

    // Recupero tutte le attività approvate in quel mese
    shiftTypeMonthDao.approvedInMonthRelatedWith(shiftTypeMonth.yearMonth, involvedShiftPeople)
        .forEach(monthStatus -> {
          // Per ogni attività calcolo le competenze di ogni persona coinvolta
          involvedShiftPeople.forEach(person -> {
            int activityCompetence = 0;
            //Cerco le competenze di turno diurno...
            activityCompetence = calculatePersonShiftCompetencesInPeriod(monthStatus.shiftType,
                person, monthBegin, lastDay, ShiftPeriod.daily);
            // Somma algebrica delle competenze delle persone derivanti da ogni attività sulla
            // quale ha svolto i turni
            totalPeopleCompetences.merge(person, activityCompetence, 
                (previousValue, newValue) -> newValue + previousValue);
            //Cerco le competenze di turno festivo...
            activityCompetence = calculatePersonShiftCompetencesInPeriod(monthStatus.shiftType,
                person, monthBegin, lastDay, ShiftPeriod.holiday);
            totalHolidayPeopleCompetences.merge(person, activityCompetence, 
                (previousValue, newValue) -> newValue + previousValue);
            //Cerco le competenze di turno notturno...
            activityCompetence = calculatePersonShiftCompetencesInPeriod(monthStatus.shiftType, 
                person, monthBegin, lastDay, ShiftPeriod.nightly);
            totalNightlyPeopleCompetences.merge(person, activityCompetence, 
                (previousValue, newValue) -> newValue + previousValue);
          });
        });
    //Assegno i codici di competenza per andare ad assegnare le competenze corrette
    CompetenceCode shiftCode = competenceCodeDao.getCompetenceCodeByCode(codShift);
    CompetenceCode nightCode = competenceCodeDao.getCompetenceCodeByCode(codShiftNight);
    CompetenceCode holidayCode = competenceCodeDao.getCompetenceCodeByCode(codShiftHolyday);

    involvedShiftPeople.forEach(person -> {
      Integer calculatedCompetences = null;
      calculatedCompetences = totalPeopleCompetences.get(person);
      saveCompetence(person, shiftTypeMonth, shiftCode, calculatedCompetences);
      // Verifico che per le person coinvolte ci siano o no eventuali residui dai mesi precedenti

      if (person.personCompetenceCodes.stream()
          .anyMatch(pcc -> pcc.competenceCode.equals(holidayCode))) {
        calculatedCompetences = totalHolidayPeopleCompetences.get(person);
        saveCompetence(person, shiftTypeMonth, holidayCode, calculatedCompetences);
      }
      if (person.personCompetenceCodes.stream()
          .anyMatch(pcc -> pcc.competenceCode.equals(nightCode))) {
        calculatedCompetences = totalNightlyPeopleCompetences.get(person);
        saveCompetence(person, shiftTypeMonth, nightCode, calculatedCompetences);
      }


    });

  }

  /**
   * Metodo che salva la quantità di ore di turno.
   *
   * @param person il dipendente per cui si vuole salvare la competenza
   * @param shiftTypeMonth il pregresso se presente con l'informazione se è già stato approvato 
   *     o meno
   * @param shiftCode il codice di competenza
   * @param calculatedCompetences la quantità di competenza calcolata
   */
  private void saveCompetence(Person person, ShiftTypeMonth shiftTypeMonth, 
      CompetenceCode shiftCode, Integer calculatedCompetences) {
    int lastShiftCompetence = getPersonResidualShiftCompetence(person, shiftTypeMonth.yearMonth);
    //

    // TODO: 12/06/17 sicuramente andranno differenziate tra T1 e T2
    int totalShiftMinutes;
    if (calculatedCompetences != null) {
      totalShiftMinutes = calculatedCompetences + lastShiftCompetence;
    } else {
      totalShiftMinutes = lastShiftCompetence;
    }

    Optional<Competence> shiftCompetence = competenceDao
        .getCompetence(person, shiftTypeMonth.yearMonth.getYear(), 
            shiftTypeMonth.yearMonth.getMonthOfYear(), shiftCode);

    Competence newCompetence = 
        shiftCompetence.or(new Competence(person, shiftCode, shiftTypeMonth.yearMonth.getYear(), 
            shiftTypeMonth.yearMonth.getMonthOfYear()));
    newCompetence.valueApproved = totalShiftMinutes / 60;
    newCompetence.exceededMins = totalShiftMinutes % 60;
    // newCompetence.valueRequested = ; e qui cosa ci va?

    newCompetence.save();
    log.info("Salvata {}", newCompetence);
  }

  /**
   * salva il personShiftDay ed effettua i ricalcoli.
   *
   * @param personShiftDay il personshiftDay da salvare
   */
  public void save(PersonShiftDay personShiftDay) {

    Verify.verifyNotNull(personShiftDay).save();
    recalculate(personShiftDay);
  }

  /**
   * cancella il personShiftDay.
   *
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

  /**
   * Metodo che ritorna il giusto numero di slot associati all'attività.
   *
   * @param timeTable la timetable relativa all'attività
   * @return la lista degli slot di turno attivi sulla timetable passata come parametro.
   */
  public List<ShiftSlot> getSlotsFromTimeTable(ShiftTimeTable timeTable) {
    List<ShiftSlot> list = Lists.newArrayList();
    if (timeTable.startMorning != null && timeTable.endMorning != null) {
      list.add(ShiftSlot.MORNING);
    }
    if (timeTable.startAfternoon != null && timeTable.endAfternoon != null) {
      list.add(ShiftSlot.AFTERNOON);
    }
    if (timeTable.startEvening != null && timeTable.endEvening != null) {
      list.add(ShiftSlot.EVENING);
    }
    return list;
  }

}