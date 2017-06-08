package manager;


import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.PersonShiftDayDao;
import dao.PersonShiftDayInTroubleDao;
import dao.RoleDao;
import dao.ShiftDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;
import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.DateUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.services.PairStamping;
import models.CompetenceCode;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonDay;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftDayInTrouble;
import models.PersonShiftShiftType;
import models.Role;
import models.ShiftCategories;
import models.ShiftTimeTable;
import models.ShiftType;
import models.Stamping;
import models.UsersRolesOffices;
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
  private final CompetenceUtility competenceUtility;
  private final CompetenceCodeDao competenceCodeDao;
  private final CompetenceDao competenceDao;
  private final IWrapperFactory wrapperFactory;
  private final UsersRolesOfficesDao uroDao;
  private final RoleDao roleDao;
  private final PersonShiftDayInTroubleDao troubleDao;
  private final PersonMonthRecapDao personMonthRecapDao;

  @Inject
  public ShiftManager2(PersonDayManager personDayManager, PersonShiftDayDao personShiftDayDao,
      PersonDayDao personDayDao, ShiftDao shiftDao,
      CompetenceUtility competenceUtility, CompetenceCodeDao competenceCodeDao,
      CompetenceDao competenceDao, IWrapperFactory wrapperFactory, UsersRolesOfficesDao uroDao,
      RoleDao roleDao, PersonShiftDayInTroubleDao troubleDao,
      PersonMonthRecapDao personMonthRecapDao) {
    this.personDayManager = personDayManager;
    this.personShiftDayDao = personShiftDayDao;
    this.personDayDao = personDayDao;
    this.shiftDao = shiftDao;
    this.competenceUtility = competenceUtility;
    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;
    this.wrapperFactory = wrapperFactory;
    this.uroDao = uroDao;
    this.roleDao = roleDao;
    this.troubleDao = troubleDao;
    this.personMonthRecapDao = personMonthRecapDao;
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

  //  /**
  //   * Controlla se il PersonShiftDay è compatibile con la presenza in Istituto
  //   * in un determinato giorno:
  //   * - assenza o missione
  //   * - mancata timbratura
  //   * - timbrature disaccoppiate
  //   * - tempo di lavoro insufficiente
  //   *
  //   * @return String:
  //   */
  //  public String checkShiftDayCompatibilityWithPresence(PersonShiftDay shift) {
  //    String errCode = "";
  //    LocalTime startShift =
  //        (shift.shiftSlot.equals(ShiftSlot.MORNING)) ? shift.shiftType.shiftTimeTable.startMorning
  //            : shift.shiftType.shiftTimeTable.startAfternoon;
  //    Optional<PersonDay> personDay = personDayDao.getPersonDay(shift.personShift.person, shift.date);
  //    if (!personDay.isPresent()) {
  ////      errCode = ShiftTroubles.FUTURE_DAY.toString();
  ////      PersonShiftDayInTrouble trouble =
  ////          new PersonShiftDayInTrouble(shift, ShiftTroubles.FUTURE_DAY);
  ////      trouble.save();
  //      return errCode;
  //    }
  //
  //    // controlla che il nuovo turno non coincida con un giorno di assenza del turnista
  //    if (personDayManager.isAllDayAbsences(personDay.get())) {
  //      errCode = ShiftTroubles.PERSON_IS_ABSENT.toString();
  //    } else if (!LocalDate.now().isBefore(shift.date)) {
  //
  //      // non sono nel futuro controllo le timbrature
  //      // controlla se non è una giornata valida di lavoro
  //      IWrapperPersonDay wrPersonDay = wrapperFactory.create(personDay.get());
  //      if (!personDayManager.isValidDay(personDay.get(), wrPersonDay)) {
  //        log.debug("NON è un giorno valido!");
  //
  //        // check no stampings
  //        if (personDay.get().hasError(Troubles.NO_ABS_NO_STAMP)) {
  //
  //          log.info("Il turno di {} e' incompatibile con la sue mancate timbrature nel "
  //              + "giorno {}", shift.personShift.person.getFullname(), personDay.get().date);
  //          return Troubles.NO_ABS_NO_STAMP.toString();
  //
  //        } else if ((personDay.get().stampings.size() == 1)
  //            && ((personDay.get().stampings.get(0).isIn() && personDay.get().stampings.get(0).date
  //            .toLocalTime().isBefore(startShift.plusMinutes(shift.shiftType.entranceTolerance)))
  //            || (personDay.get().stampings.get(0).isOut() && personDay.get().stampings.get(0).date
  //            .toLocalTime().isBefore(startShift.minusMinutes(shift.shiftType.exitTolerance))))) {
  //          // (?)DA RIVEDERE CONTROLLI
  //          log.info("Il turno di {} e' incompatibile con la sola timbratura nel giorno {}"
  //              + "giorno {}", shift.personShift.person.getFullname(), personDay.get().date);
  //
  //        } else if (personDay.get().hasError(Troubles.UNCOUPLED_WORKING)) {
  //          // there are no stampings
  //          log.info("Il turno di {} e' incompatibile con le timbraure disaccoppiate nel "
  //              + "giorno {}", shift.personShift.person.getFullname(), personDay.get().date);
  //          return Troubles.UNCOUPLED_WORKING.toString();
  //        } else {
  //          log.info("La giornata lavorativa di {} per il giorno {} non è valida",
  //              shift.personShift.person.getFullname(),
  //              personDay.get().date);
  //          return Troubles.NOT_ENOUGH_WORKTIME.toString();
  //        }
  //      }
  //    }
  //
  //    return errCode;
  //  }

  //  /**
  //   * Salva le ore di turno da retribuire di un certo mese nelle competenze.
  //   * Per ogni persona riceve i giorni di turno effettuati nel mese e le eventuali ore non lavorate.
  //   * Calcola le ore da retribuire sulla base dei giorni di turno sottraendo le eventuali ore non
  //   * lavorate e aggiungendo i minuti eventualemnte avanzati nel mese precedente. Le ore retribuite
  //   * sono la parte intera delle ore calcolate.
  //   * I minuti eccedenti sono memorizzati nella competenza per i mesi successivi.
  //   *
  //   * @param personsShiftHours contiene per ogni persona il numero dei giorni in turno lavorati
  //   * (thDays) e gli eventuali minuti non lavorati (thLackTime)
  //   * @param year anno di riferimento dei turni
  //   * @param month mese di riferimento dei turni
  //   * @return la lista delle competenze corrispondenti ai turni lavorati
  //   * @author arianna
  //   */
  //  public List<Competence> updateDbShiftCompetences(
  //      Table<Person, String, Integer> personsShiftHours, int year, int month) {
  //
  //    List<Competence> savedCompetences = new ArrayList<Competence>();
  //    int[] apprHoursAndExcMins;
  //
  //    String thDays = Messages.get("PDFReport.thDays");
  //    String thLackTime = Messages.get("PDFReport.thLackTime");
  //
  //    // get the Competence code for the ordinary shift
  //    CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode(codShift);
  //
  //    // for each person
  //    for (Person person : personsShiftHours.rowKeySet()) {
  //
  //      log.debug("Registro dati di {} {}", person.surname, person.name);
  //
  //      BigDecimal sessanta = new BigDecimal("60");
  //
  //      log.debug("Calcolo le ore di turno teoriche dai giorni = {}",
  //          personsShiftHours.get(person, thDays));
  //      BigDecimal numOfHours =
  //          competenceUtility.calcShiftHoursFromDays(personsShiftHours.get(person, thDays));
  //
  //      // compute the worked time in minutes of the present month
  //      int workedMins = (personsShiftHours.contains(person, thLackTime))
  //          ? numOfHours.multiply(sessanta).subtract(
  //          new BigDecimal(personsShiftHours.get(person, thLackTime))).intValue()
  //          : numOfHours.multiply(sessanta).intValue();
  //
  //      log.debug("Minuti lavorati = thReqHour * 60 - thLackTime = {} * 60 - {}",
  //          numOfHours, personsShiftHours.get(person, thLackTime));
  //
  //      // compute the hours appproved and the exceede minutes on the basis of
  //      // the current worked minutes and the exceeded mins of the previous month
  //      apprHoursAndExcMins = calcShiftValueApproved(person, year, month, workedMins);
  //
  //      // compute the value requested
  //      BigDecimal reqHours = competenceUtility.calcDecimalShiftHoursFromMinutes(workedMins);
  //
  //      // save the FS reperibility competences in the DB
  //      Optional<Competence> shiftCompetence =
  //          competenceDao.getCompetence(person, year, month, competenceCode);
  //
  //      // update the requested hours
  //      if (shiftCompetence.isPresent()) {
  //
  //        // check if the competence has been processed to be sent to Rome
  //        // and and this case we don't change the valueApproved
  //        CertificatedData certData = personMonthRecapDao
  //            .getPersonCertificatedData(person, month, year);
  //
  //        int apprHours = (certData != null && certData.isOk && (certData.competencesSent != null))
  //            ? shiftCompetence.get().valueApproved : apprHoursAndExcMins[0];
  //        int exceededMins =
  //            (certData != null && certData.isOk && (certData.competencesSent != null))
  //                ? shiftCompetence.get().exceededMins
  //                : apprHoursAndExcMins[1];
  //
  //        shiftCompetence.get().setValueApproved(apprHours);
  //        shiftCompetence.get().setValueRequested(reqHours);
  //        shiftCompetence.get().setExceededMin(exceededMins);
  //        shiftCompetence.get().save();
  //
  //        log.debug("Aggiornata competenza di {} {}: valueRequested={}, valueApproved={}, "
  //                + "exceddMins={}", shiftCompetence.get().person.surname,
  //            shiftCompetence.get().person.name, shiftCompetence.get().valueRequested,
  //            shiftCompetence.get().valueApproved, shiftCompetence.get().exceededMins);
  //
  //        savedCompetences.add(shiftCompetence.get());
  //      } else {
  //        // insert a new competence with the requested hours an reason
  //        Competence competence = new Competence(person, competenceCode, year, month);
  //        competence.setValueApproved(apprHoursAndExcMins[0]);
  //        competence.setExceededMin(apprHoursAndExcMins[1]);
  //        competence.setValueRequested(reqHours);
  //        competence.save();
  //
  //        savedCompetences.add(competence);
  //
  //        log.debug("Salvata competenza {}", shiftCompetence);
  //      }
  //    }
  //
  //    // return the number of saved competences
  //    return savedCompetences;
  //  }

  //  /**
  //   * Calcola le ore di turno da approvare date quelle richieste.
  //   * Poiché le ore approvate devono essere un numero intero e quelle calcolate direttamente dai
  //   * giorni di turno possono essere decimali, le ore approvate devono essere arrotondate per
  //   * eccesso o per difetto a seconda dell'ultimo arrotondamento effettuato in modo che questi
  //   * vengano alternati.
  //   *
  //   * @author arianna
  //   */
  //  public int[] calcShiftValueApproved(Person person, int year, int month, int requestedMins) {
  //    int hoursApproved = 0;
  //    int exceedMins = 0;
  //
  //    log.debug("Nella calcShiftValueApproved person ={}, year={}, month={}, requestedMins={})",
  //        person, year, month, requestedMins);
  //
  //    String workedTime = competenceUtility.calcStringShiftHoursFromMinutes(requestedMins);
  //    int hoursOfWorkedTime = Integer.parseInt(workedTime.split("\\.")[0]);
  //    int minsOfWorkedTime = Integer.parseInt(workedTime.split("\\.")[1]);
  //
  //    log.debug("hoursOfWorkedTime = {} minsOfWorkedTime = {}", hoursOfWorkedTime, minsOfWorkedTime);
  //
  //    // get the Competence code for the ordinary shift
  //    CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode(codShift);
  //
  //    log.debug("month={}", month);
  //
  //    Competence myCompetence =
  //        competenceDao.getLastPersonCompetenceInYear(person, year, month, competenceCode);
  //
  //    int oldExceedMins = 0;
  //
  //    // get the old exceede mins in the DB
  //    oldExceedMins =
  //        ((myCompetence == null)
  //            || ((myCompetence != null) && myCompetence.getExceededMin() == null))
  //            ? 0 : myCompetence.getExceededMin();
  //
  //    log.debug("oldExceedMins in the DB={}", oldExceedMins);
  //
  //    // if there are no exceeded mins, the approved hours
  //    // match with the worked hours
  //    if (minsOfWorkedTime == 0) {
  //      hoursApproved = hoursOfWorkedTime;
  //      exceedMins = oldExceedMins;
  //
  //    } else {
  //      // check if the exceeded mins of this month plus those
  //      // worked in the previous months make up an hour
  //      exceedMins = oldExceedMins + minsOfWorkedTime;
  //      if (exceedMins >= 60) {
  //        hoursApproved = hoursOfWorkedTime + 1;
  //        exceedMins -= 60;
  //      } else {
  //        hoursApproved = hoursOfWorkedTime;
  //      }
  //
  //    }
  //
  //    log.debug("hoursApproved={} exceedMins={}", hoursApproved, exceedMins);
  //
  //    int[] result = {hoursApproved, exceedMins};
  //
  //    log.debug("La calcShiftValueApproved restituisce {}", result);
  //
  //    return result;
  //  }


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

  /***********************************************************************************************/
  /**Sezione di metodi utilizzati al bootstrap per sistemare le situazioni sui turni             */
  /***********************************************************************************************/

  public void linkSupervisorToRole() {
    List<ShiftCategories> list = ShiftCategories.findAll();
    Role role = roleDao.getRoleByName(Role.SHIFT_SUPERVISOR);
    list.forEach(item -> {
      Optional<UsersRolesOffices> optional = uroDao
          .getUsersRolesOffices(item.supervisor.user, role, item.office);
      if (!optional.isPresent()) {
        UsersRolesOffices uro = new UsersRolesOffices();
        uro.office = item.office;
        uro.role = role;
        uro.user = item.supervisor.user;
        uro.save();
        log.info("aggiunto ruolo di supervisore turni per {} della sede {}",
            item.supervisor.fullName(), item.office);
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
    // TODO: 06/06/17 Verificare se questo vincolo va bene o deve esistere solo per 2 turni sulla stessa attività
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
        .getPersonShiftDayByTypeAndPeriod(personShiftDay.date, personShiftDay.date,
            personShiftDay.shiftType);

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

    PersonShiftDayInTrouble trouble = new PersonShiftDayInTrouble(shift, cause);

    if (!shift.troubles.contains(trouble)) {
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
    shift.troubles.removeIf(trouble -> {
      if (trouble.cause == cause) {
        trouble.delete();
        log.info("Rimosso personShiftDayInTrouble {} - {} - {}",
            shift.personShift.person.getFullname(), shift.date, cause);
        return true;
      }
      return false;
    });
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
    ShiftTimeTable timeTable = personShiftDay.shiftType.shiftTimeTable;
    final LocalTime begin;
    final LocalTime end;
    Optional<PersonDay> personDay =
        personDayDao.getPersonDay(personShiftDay.personShift.person, personShiftDay.date);
    if (personDay.isPresent()) {
      List<Stamping> stampings = personDay.get().getStampings();
      // controlla se non sono nel futuro ed è un giorno valido
      IWrapperPersonDay wrPersonDay = wrapperFactory.create(personDay.get());
      if (!LocalDate.now().isBefore(personShiftDay.date) && personDayManager
          .isValidDay(personDay.get(), wrPersonDay)) {

        switch (personShiftDay.shiftSlot) {
          case MORNING:
            begin = timeTable.startMorning;
            end = timeTable.endMorning;
            getShiftSituation(personShiftDay, begin, end, timeTable, stampings);
            break;

          case AFTERNOON:
            begin = timeTable.startAfternoon;
            end = timeTable.endAfternoon;
            getShiftSituation(personShiftDay, begin, end, timeTable, stampings);
            break;
            //TODO: case EVENING??
          default:
            break;
        }
      } else {
        //TODO ???
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
  public void checkShifDayValid(LocalDate date, ShiftType activity) {
    List<PersonShiftDay> dayList = shiftDao.getShiftDaysByPeriodAndType(date, date, activity);
    for (PersonShiftDay pd : dayList) {
      checkShiftValid(pd);
      if (troubleDao.getPersonShiftDayInTroubleByType(pd, ShiftTroubles.PROBLEMS_ON_OTHER_SLOT)
          .isPresent()) {

      }
    }

  }

  /**
   * Effettua il calcolo dei minuti di turno maturati nel mese su un'attività per ogni persona in
   * turno
   *
   * @param activity attività sulla quale effettuare i calcoli
   * @param yearMonth Mese sul quale effettuare i calcoli
   * @return Restituisce una mappa con i minuti di turno maturati per ogni persona.
   */
  public Map<Person, Integer> calculateActivityShiftCompetences(ShiftType activity,
      YearMonth yearMonth) {

    // FIXME effettuare il calcolo una persona alla volta con il metodo calculatePersonShiftCompetences()
    // ed effettuare il loop utilizzando quello
    final LocalDate monthBegin = yearMonth.toLocalDate(1);
    final LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    final Map<Person, Integer> shiftCompetences = new HashMap<>();

    // Vengono filtrati perchè sui giorni futuri non si possono fare calcoli
    personShiftDayDao.getPersonShiftDayByTypeAndPeriod(monthBegin, monthEnd, activity).stream()
    .filter(personShiftDay -> !personShiftDay.date.isAfter(LocalDate.now()))
    .forEach(shift -> {
      final Person person = shift.personShift.person;
      Integer totalMinutes = shiftCompetences.get(person);
      Integer shiftMinutes;

      // Nessun errore sul turno (mi aspetto che lo stato del turno sia aggiornato)
      if (shift.troubles.isEmpty()) {
        shiftMinutes = shift.shiftType.shiftTimeTable.getSlotDuration(shift.shiftSlot);
      } else if (shift.troubles.size() == 1 && shift
          .hasError(ShiftTroubles.NOT_COMPLETE_SHIFT)) {
        // Il turno vale comunque ma con un'ora in meno
        shiftMinutes =
            shift.shiftType.shiftTimeTable.getSlotDuration(shift.shiftSlot) - SIXTY_MINUTES;
      } else {
        shiftMinutes = 0;
      }

      if (totalMinutes == null) {
        totalMinutes = shiftMinutes;
      } else {
        totalMinutes += shiftMinutes;
      }
      shiftCompetences.put(shift.personShift.person, totalMinutes);
    });

    return shiftCompetences;
  }

  //
  //  public void calculatePersonShiftCompetences(ShiftType activity,
  //      Person person, YearMonth yearMonth) {
  //
  //  }


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


  /**
   * @param shift il personShiftDay di cui si vuole verificare la validità
   * @param begin l'ora di inizio del turno
   * @param end l'ora di fine del turno
   * @param timeTable la timetable contenente i dati su cui basare i calcoli
   * @param stampings la lista di timbrature da vagliare nel periodo begin-end
   * @return l'eventuale codice di errore derivante dai controlli effettuati sulle timbrature e il
   *     tempo in turno.
   */
  private void getShiftSituation(PersonShiftDay shift, LocalTime begin,
      LocalTime end, ShiftTimeTable timeTable, List<Stamping> stampings) {

    LocalTime beginWithTolerance;
    LocalTime endWithTolerance;
    if (shift.shiftType.entranceTolerance != 0) {
      beginWithTolerance = begin.plusMinutes(shift.shiftType.entranceTolerance);
    } else {
      beginWithTolerance = begin;
    }
    if (shift.shiftType.exitTolerance != 0) {
      endWithTolerance = end.minusMinutes(shift.shiftType.exitTolerance);
    } else {
      endWithTolerance = end;
    }
    //Controlli sulle timbrature...
    List<PairStamping> pairStampings = personDayManager
        .getValidPairStampings(stampings);

    int timeInShift = personDayManager.workingMinutes(pairStampings, begin, end);

    //verifico se la tolleranza oraria è presente...
    if (shift.shiftType.hourTolerance != 0) {
      //l'ingresso è successivo alla soglia di tolleranza sull'ingresso in turno
      if (pairStampings.get(0).first.date.toLocalTime()
          .isAfter(begin.plusMinutes(shift.shiftType.hourTolerance))) {
        setShiftTrouble(shift, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
      } else {
        fixShiftTrouble(shift, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
      }
      //la timbratura di ingresso è compresa tra la tolleranza e la tolleranza oraria sul turno
      if (Range.open(beginWithTolerance, begin.plusMinutes(shift.shiftType.hourTolerance))
          .contains(pairStampings.get(0).first.date.toLocalTime())) {
        setShiftTrouble(shift, ShiftTroubles.NOT_COMPLETE_SHIFT);
      } else {
        fixShiftTrouble(shift, ShiftTroubles.NOT_COMPLETE_SHIFT);
      }
      // il tempo in turno è compreso tra il tempo pagato per turno e la tolleranza oraria
      if (Range.open(timeTable.paidMinutes, timeTable.paidMinutes - shift.shiftType.hourTolerance)
          .contains(timeInShift)) {
        setShiftTrouble(shift, ShiftTroubles.NOT_COMPLETE_SHIFT);
      } else {
        fixShiftTrouble(shift, ShiftTroubles.NOT_COMPLETE_SHIFT);
      }

    } else {
      if (pairStampings.get(0).first.date.toLocalTime().isAfter(beginWithTolerance)) {
        setShiftTrouble(shift, ShiftTroubles.NOT_COMPLETE_SHIFT);
      } else {
        fixShiftTrouble(shift, ShiftTroubles.NOT_COMPLETE_SHIFT);
      }
    }
    
    //l'uscita è precedente alla soglia di tolleranza sull'uscita dal turno
    if (pairStampings.get(pairStampings.size() - 1).second.date.toLocalTime()
        .isBefore(endWithTolerance)) {
      setShiftTrouble(shift, ShiftTroubles.OUT_OF_STAMPING_TOLERANCE);
    } else {
      fixShiftTrouble(shift, ShiftTroubles.OUT_OF_STAMPING_TOLERANCE);
    }
    
    
    //Controlli sul tempo a lavoro in turno...
    if (timeInShift < timeTable.paidMinutes - shift.shiftType.hourTolerance) {
      setShiftTrouble(shift, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
    } else {
      fixShiftTrouble(shift, ShiftTroubles.NOT_ENOUGH_WORKING_TIME);
    }


    // controlli sull'eventuale pausa in turno abilitata...
    if (shift.shiftType.breakInShiftEnabled) {
      List<PairStamping> gapBreakInShift =
          getBreakPairStampings(pairStampings, beginWithTolerance, endWithTolerance);
      int maxTime = 0;
      for (PairStamping pair : gapBreakInShift) {
        maxTime = pair.timeInPair;
      }
      if (maxTime > shift.shiftType.breakInShift) {
        setShiftTrouble(shift, ShiftTroubles.EXCEEDED_BREAKTIME);
      } else {
        fixShiftTrouble(shift, ShiftTroubles.EXCEEDED_BREAKTIME);
      }
    }

  }

  /**
   * @param pairStampings la lista di coppie valide di entrata/uscita
   * @param begin l'ora di inizio del turno
   * @param end l'ora di fine del turno
   * @return la lista di coppie di timbrature di uscita/entrata appartenenti all'intervallo di turno
   *     che vanno considerate per controllare se il tempo trascorso in pausa eccede quello previsto
   *     dalla configurazione di turno.
   */
  private List<PairStamping> getBreakPairStampings(List<PairStamping> pairStampings,
      LocalTime begin, LocalTime end) {
    List<PairStamping> allGapPairs = Lists.newArrayList();
    PairStamping previous = null;
    for (PairStamping validPair : pairStampings) {
      if (previous != null) {
        if ((previous.second.stampType == null
            || previous.second.stampType.isGapLunchPairs())
            && (validPair.first.stampType == null
            || validPair.first.stampType.isGapLunchPairs())) {

          allGapPairs.add(new PairStamping(previous.second, validPair.first));
        }
      }
      previous = validPair;
    }
    List<PairStamping> gapPairs = Lists.newArrayList();
    for (PairStamping gapPair : allGapPairs) {
      LocalTime first = gapPair.first.date.toLocalTime();
      LocalTime second = gapPair.second.date.toLocalTime();

      boolean isInIntoBreakTime = !first.isBefore(begin) && !first.isAfter(end);
      boolean isOutIntoBreakTime = !second.isBefore(begin) && !second.isAfter(end);

      if (!isInIntoBreakTime && !isOutIntoBreakTime) {
        if (second.isBefore(begin) || first.isAfter(end)) {
          continue;
        }
      }

      LocalTime inForCompute = gapPair.first.date.toLocalTime();
      LocalTime outForCompute = gapPair.second.date.toLocalTime();
      if (!isInIntoBreakTime) {
        inForCompute = begin;
      }
      if (!isOutIntoBreakTime) {
        outForCompute = end;
      }
      int timeInPair = 0;
      timeInPair -= DateUtility.toMinute(inForCompute);
      timeInPair += DateUtility.toMinute(outForCompute);
      gapPair.timeInPair = timeInPair;
      gapPairs.add(gapPair);
    }
    return gapPairs;
  }
}
