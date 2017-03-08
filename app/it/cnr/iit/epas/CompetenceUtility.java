package it.cnr.iit.epas;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.PersonDayDao;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.services.PairStamping;

import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.ShiftSlot;
import models.query.QCompetence;
import models.query.QCompetenceCode;
import models.query.QPerson;
import models.query.QPersonShiftShiftType;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.i18n.Messages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@Slf4j
public class CompetenceUtility {
  //codice dei turni feriali
  public static String codFr = "207";
  //codice dei turni festivi
  public static String codFs = "208";
  //codice dei turni
  public static String codShift = "T1";
  //nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
  public static String thNoStampings = Messages.get("PDFReport.thNoStampings");
  public static String thMissingTime = Messages.get("PDFReport.thMissingTime");
  //nome della colonna per i giorni di assenza della tabella delle inconsistenze
  public static String thAbsences = Messages.get("PDFReport.thAbsences");
  //nome della colonna per i giorni di assenza della tabella delle inconsistenze
  public static String thMissions = Messages.get("PDFReport.thMissions");
  //nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni
  public static String thBadStampings = Messages.get("PDFReport.thBadStampings");
  public static String thWarnStampings = Messages.get("PDFReport.thWarnStampings");
  //nome della colonna per i giorni di turno svolti mensilmente da una persona
  public static String thDays = Messages.get("PDFReport.thDays");
  //nome della colonna per le ore di turno svolte mensilmente da una persona
  public static String thReqHour = Messages.get("PDFReport.thReqHour");
  //nome della colonna per le ore di turno approvate mensilmente per una persona
  public static String thAppHour = Messages.get("PDFReport.thAppHour");
  public static String thLackTime = Messages.get("PDFReport.thLackTime");
  //Nome dela colonna contenente i minuti accumulati di turno da riportare nei mesi successivi
  public static String thExceededMin = Messages.get("PDFReport.thExceededMin");
  public static String thIncompleteTime = Messages.get("PDFReport.thIncompleteTime");

  private final JPQLQueryFactory queryFactory;
  private final PersonDayDao personDayDao;
  private PersonDayManager personDayManager;

  /**
   * Construttore predefinito che inizializza queryFactory e vari manager.
   */
  @Inject
  public CompetenceUtility(JPQLQueryFactory queryFactory,
      PersonDayDao personDayDao, PersonDayManager personDayManager) {

    this.queryFactory = queryFactory;
    this.personDayDao = personDayDao;
    this.personDayManager = personDayManager;
  }

  /**
   * Calcola le ore di turno dai giorni (days)
   * resto = (days%2 == 0) ? 0 : 0.5
   * ore = days*6 + (int)(days/2) + resto;
   *
   * @author arianna
   * TODO: parametrico rispetto ale pre del DB (ampo ore retribuite)
   */
  public BigDecimal calcShiftHoursFromDays(int days) {
    BigDecimal decDays = new BigDecimal(days);
    BigDecimal due = new BigDecimal("2");

    BigDecimal minutes = (days % 2 == 0) ? BigDecimal.ZERO : new BigDecimal("0.5");
    BigDecimal hours =
        decDays.multiply(
            new BigDecimal(6)).add(decDays.divide(due, RoundingMode.HALF_DOWN)).add(minutes);

    log.debug("La calcShiftHoursFromDays restituisce hours = {}", hours);

    return hours;
  }


  /**
   * Calcola le oer e i minuti dal numero totale dei minuti
   * lavorati.
   *
   * @author arianna
   */
  public BigDecimal calcDecimalShiftHoursFromMinutes(int minutes) {
    int hours;
    int mins;

    log.debug("Nella calcDecimalShiftHoursFromMinutes({})", minutes);

    if (minutes < 60) {
      hours = 0;
      mins = minutes;
    } else {
      hours = minutes / 60;
      mins = minutes % 60;
    }

    log.debug("hours = {} mins = {}", hours, mins);
    return new BigDecimal(Integer.toString(hours).concat(".").concat(Integer.toString(mins)));
  }


  /**
   * Calcola il LocalTime dal numero dei minuti
   * che compongono l'orario.
   *
   * @author arianna
   */
  public String calcStringShiftHoursFromMinutes(int minutes) {
    int hours;
    int mins;

    if (minutes < 60) {
      hours = 0;
      mins = minutes;
    } else {
      hours = minutes / 60;
      mins = minutes % 60;
    }

    //log.debug("hours = {} mins = {}", hours, mins);

    return Integer.toString(hours).concat(".").concat(Integer.toString(mins));
  }


  /**
   * Da chiamare per aggiornare la tabella competences inserendo i 30 minuti avanzati nella colonna
   * exceeded_min  nel caso in cui ci sia stato un arrotondamento per difetto delle ore approvate
   * rispetto a quelle richieste.
   */
  public void updateExceedeMinInCompetenceTable() {
    int year = 2015;
    int month = 3;  // Mese attuale del quale dobbiamo ancora fare il pdf

    int exceddedMin;

    CompetenceCode competenceCode =
        CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codShift)
            .first();
    List<Person> personList;

    QCompetence com = QCompetence.competence;
    QPerson person = QPerson.person;
    QPersonShiftShiftType personShiftShiftType = QPersonShiftShiftType.personShiftShiftType;

    personList = queryFactory.from(person)
        .join(person.personShift.personShiftShiftTypes, personShiftShiftType)
        .where(
            personShiftShiftType.shiftType.type.in(ImmutableSet.of("A", "B"))
        ).list(person);

    for (Person p : personList) {

      final JPQLQuery query = queryFactory.query();

      // leggo l'ultima competenza con il numero delle ore approvate
      // diverso da quello richieste
      final Competence myCompetence = query
          .from(com)
          .where(
              com.person.eq(p)
                  .and(com.year.eq(year))
                  .and(com.month.lt(month))
                  .and(com.competenceCode.eq(competenceCode))
                  .and(com.valueApproved.ne(0))
                  .and(com.valueRequested.ne(BigDecimal.ZERO))
                  .and(com.valueRequested.intValue().ne(com.valueApproved)
                      .or(com.valueRequested.floor().ne(com.valueRequested)))
          )
          .orderBy(com.year.desc(), com.month.desc())
          .limit(1)
          .uniqueResult(com);

      // calcolo i minuti in eccesso non ancora remunerati
      if (myCompetence == null) {
        // we are at the first case, so the person has its fist 0.5 hour to accumulate
        log.debug("myCompetence is null");
        exceddedMin = 0;
      } else if (myCompetence.valueRequested.setScale(0, RoundingMode.UP).intValue()
          <= myCompetence.valueApproved) {
        log.debug("La query sulle competenze ha trovato {} e "
                + "myCompetence.valueRequested.ROUND_CEILING={} "
                + "<= myCompetence.valueApproved=%d",
            myCompetence.toString(), myCompetence.valueRequested.ROUND_CEILING,
            myCompetence.valueApproved);
        // Last rounding was on ceiling, so we round to floor
        //valueApproved = requestedHours.setScale(0, RoundingMode.DOWN).intValue();
        exceddedMin = 0;
      } else {
        log.debug("La query sulle competenze ha trovato {}", myCompetence.toString());
        // we round to ceiling
        //valueApproved = requestedHours.setScale(0, RoundingMode.UP).intValue();
        exceddedMin = 30;
      }


      Competence lastCompetence = getLastCompetence(p, year, month, competenceCode);
      // aggiorno la competenza con i minuti in eccesso calcolati
      lastCompetence.setExceededMin(exceddedMin);
      lastCompetence.save();
    }

  }


  private Competence getLastCompetence(
      Person person, int year, int month, CompetenceCode competenceCode) {
    QCompetence com2 = QCompetence.competence;
    QCompetenceCode comCode = QCompetenceCode.competenceCode;
    // prendo la competenza del mese precedente
    return queryFactory
        .from(com2)
        .join(com2.competenceCode, comCode)
        .where(
            com2.person.eq(person)
                .and(com2.year.eq(year))
                .and(com2.month.lt(month))
                .and(comCode.eq(competenceCode))
        )
        .orderBy(com2.year.desc(), com2.month.desc())
        .limit(1)
        .uniqueResult(com2);
  }

  /**
   * Aggiorna la tabella totalPersonShiftSumDays per contenere, per ogni persona nella lista dei
   * turni personShiftDays, e per ogni tipo di turno trovato, il numero di giorni di turno
   * effettuati.
   *
   * @param personShiftDays            lista di shiftDays
   * @param personShiftSumDaysForTypes tabella contenente il numero di giorni di turno effettuati
   *                                   per ogni persona e tipologia di turno. Questa tabella viene
   *                                   aggiornata contando i giorni di turno contenuti nella lista
   *                                   personShiftDays passata come parametro
   * @author arianna
   */
  public void countPersonsShiftsDays(
      List<PersonShiftDay> personShiftDays,
      Table<Person, String, Integer> personShiftSumDaysForTypes) {

    // for each person and dy in the month contains worked shift (A/B)
    ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder();
    Table<Person, Integer, String> shiftMonth = null;

    // for each person contains the number of days of working shift divided by shift's type

    for (PersonShiftDay personShiftDay : personShiftDays) {
      Person person = personShiftDay.personShift.person;

      // registro il turno della persona per quel giorno
      //------------------------------------------------------
      builder.put(person, personShiftDay.date.getDayOfMonth(), personShiftDay.shiftType.type);
    }
    shiftMonth = builder.build();

    // for each person and shift type counts the total shift days
    for (Person person : shiftMonth.rowKeySet()) {

      log.debug("conto i turni di {}", person);

      // number of competence
      int shiftNum = 0;

      for (int day : shiftMonth.columnKeySet()) {

        if (shiftMonth.contains(person, day)) {
          // get the shift type
          String shift = shiftMonth.get(person, day);
          shiftNum = (personShiftSumDaysForTypes.contains(person, shift))
              ? personShiftSumDaysForTypes.get(person, shift) : 0;
          shiftNum++;
          personShiftSumDaysForTypes.put(person, shift, shiftNum);
        }
      }

    }

    log.debug("la countPersonsShiftCompetences ritorna  totalPersonShiftSumDays.size() = {}",
        personShiftSumDaysForTypes.size());

  }


  /**
   * crea una tabella con le eventuali inconsistenze tra le timbrature dei
   * reperibili di un certo tipo e i turni di reperibilit√† svolti in un determinato periodo di
   * tempo ritorna una tabella del tipo (Person, [thNoStamping, thAbsence], List<'gg/MMM '>).
   *
   * @author arianna
   */
  public Table<Person, String, List<String>> getReperibilityInconsistenceAbsenceTable(
      List<PersonReperibilityDay> personReperibilityDays,
      LocalDate startDate, LocalDate endDate) {
    // for each person contains days with absences and no-stamping  matching the reperibility days
    final Table<Person, String, List<String>> inconsistentAbsenceTable =
        HashBasedTable.<Person, String, List<String>>create();

    // lista dei giorni di assenza e mancata timbratura
    List<String> noStampingDays = new ArrayList<String>();
    List<String> absenceDays = new ArrayList<String>();

    for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
      Person person = personReperibilityDay.personReperibility.person;


      //check for the absence inconsistencies
      //------------------------------------------

      Optional<PersonDay> personDay = personDayDao.getPersonDay(person, personReperibilityDay.date);

      // if there are no events and it is not an holiday -> error
      if (!personDay.isPresent() & LocalDate.now().isAfter(personReperibilityDay.date)) {
        //if (!person.isHoliday(personReperibilityDay.date)) {
        if (!personDayManager.isHoliday(person, personReperibilityDay.date)) {
          log.info("La reperibilità di {} {} è incompatibile con la sua mancata timbratura nel "
              + "giorno {}", person.name, person.surname, personReperibilityDay.date);


          noStampingDays =
              (inconsistentAbsenceTable.contains(person, thNoStampings))
                  ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
          noStampingDays.add(personReperibilityDay.date.toString("dd MMM"));
          inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
        }
      } else if (LocalDate.now().isAfter(personReperibilityDay.date)) {
        // check for the stampings in working days
        if (!personDayManager.isHoliday(person, personReperibilityDay.date)
            && personDay.get().stampings.isEmpty()) {
          log.info("La reperibilità di {} {} è incompatibile con la sua mancata timbratura nel "
              + "giorno {}", person.name, person.surname, personDay.get().date);


          noStampingDays =
              (inconsistentAbsenceTable.contains(person, thNoStampings))
                  ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
          noStampingDays.add(personReperibilityDay.date.toString("dd MMM"));
          inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
        }

        // check for absences
        if (!personDay.get().absences.isEmpty()) {
          for (Absence absence : personDay.get().absences) {
            if (absence.justifiedType.name == JustifiedTypeName.all_day) {
              log.info("La reperibilita' di {} {} e' incompatibile con la sua assenza nel "
                  + "giorno {}", person.name, person.surname, personReperibilityDay.date);

              absenceDays =
                  (inconsistentAbsenceTable.contains(person, thAbsences))
                      ? inconsistentAbsenceTable.get(person, thAbsences) : new ArrayList<String>();
              absenceDays.add(personReperibilityDay.date.toString("dd MMM"));
              inconsistentAbsenceTable.put(person, thAbsences, absenceDays);
            }
          }
        }
      }
    }

    return inconsistentAbsenceTable;
  }


  /**
   * Costruisce la tabella delle inconsistenza tra i giorni di turno dati e le timbrature
   * ritorna una tabella del tipo (Person, [thNoStamping, thAbsence, thBadStampings],
   * [List<'gg MMM'>, List<'gg MMM'>, 'dd MMM -> HH:mm-HH:mm']).
   */
  public void getShiftInconsistencyTimestampTable(
      List<PersonShiftDay> personShiftDays,
      Table<Person, String, List<String>> inconsistentAbsenceTable) {

    log.debug("---------thBadStampings = {}-----", thBadStampings);
    // lista dei giorni di assenza nel mese, mancata timbratura e timbratura inconsistente
    List<String> noStampingDays = new ArrayList<String>();        // mancata timbratura
    List<String> badStampingDays = new ArrayList<String>();        // timbrature errate
    List<String> absenceDays = new ArrayList<String>();            // giorni di assenza
    List<String> lackOfTimes = new ArrayList<String>();            // tempo mancante


    for (PersonShiftDay personShiftDay : personShiftDays) {
      Person person = personShiftDay.personShift.person;

      // legge l'orario di inizio e fine turno da rispettare (mattina o pomeriggio)
      LocalTime startShift =
          (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING))
              ? personShiftDay.shiftType.shiftTimeTable.startMorning
              : personShiftDay.shiftType.shiftTimeTable.startAfternoon;
      LocalTime endShift =
          (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING))
              ? personShiftDay.shiftType.shiftTimeTable.endMorning
              : personShiftDay.shiftType.shiftTimeTable.endAfternoon;

      // legge l'orario di inizio e fine pausa pranzo del turno
      LocalTime startLunchTime =
          (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING))
              ? personShiftDay.shiftType.shiftTimeTable.startMorningLunchTime
              : personShiftDay.shiftType.shiftTimeTable.startAfternoonLunchTime;
      LocalTime endLunchTime =
          (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING))
              ? personShiftDay.shiftType.shiftTimeTable.endMorningLunchTime
              : personShiftDay.shiftType.shiftTimeTable.endAfternoonLunchTime;

      //Logger.debug("Turno: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);

      // Add flexibility (15 min.) due to the new rules (PROT. N. 0008692 del 2/12/2014)
      LocalTime roundedStartShift = startShift.plusMinutes(15);

      log.debug("Turno: {}-{}  {}-{}", startShift, startLunchTime, endLunchTime, endShift);

      //check for the absence inconsistencies
      //------------------------------------------
      Optional<PersonDay> personDay = personDayDao.getPersonDay(person, personShiftDay.date);
      log.debug("Prelevo il personDay {} per la persona {}", personShiftDay.date, person.surname);

      // if there are no events and it is not an holiday -> error
      if (!personDay.isPresent()) {

        if (!personDayManager.isHoliday(person, personShiftDay.date)
            && personShiftDay.date.isBefore(LocalDate.now())) {
          log.info("Il turno di {} {} e' incompatibile con la sua mancata timbratura nel giorno"
              + " {} (personDay == null)", person.name, person.surname, personShiftDay.date);

          noStampingDays =
              (inconsistentAbsenceTable.contains(person, thNoStampings))
                  ? inconsistentAbsenceTable.get(person, thNoStampings)
                  : new ArrayList<String>();
          noStampingDays.add(personShiftDay.date.toString("dd MMM"));
          inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);

          log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
              person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
        }
      } else {

        // check for the stampings in working days
        if (!personDayManager.isHoliday(person, personShiftDay.date)
            && LocalDate.now().isAfter(personShiftDay.date)) {

          // check no stampings
          //-----------------------------
          if (personDay.get().stampings.isEmpty()) {
            log.info("Il turno di {} {} e' incompatibile con la sue mancate timbrature nel giorno"
                + " {}", person.name, person.surname, personDay.get().date);


            noStampingDays =
                (inconsistentAbsenceTable.contains(person, thNoStampings))
                    ? inconsistentAbsenceTable.get(person, thNoStampings)
                    : new ArrayList<String>();
            noStampingDays.add(personShiftDay.date.toString("dd MMM"));
            inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);

            log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
          } else {
            // check consistent stampings
            //-----------------------------
            // legge le coppie di timbrature valide
            //FIXME injettare il PersonDayManager
            List<PairStamping> pairStampings =
                personDayManager.getValidPairStampings(personDay.get().stampings);

            //Logger.debug("Dimensione di pairStampings =%s", pairStampings.size());

            // se c'e' una timbratura guardo se e' entro il turno
            if ((personDay.get().stampings.size() == 1)
                && ((personDay.get().stampings.get(0).isIn()
                && personDay.get().stampings.get(0).date.toLocalTime()
                .isAfter(roundedStartShift))
                || (personDay.get().stampings.get(0).isOut()
                && personDay.get().stampings.get(0).date.toLocalTime()
                .isBefore(roundedStartShift)))) {


              String stamp =
                  (personDay.get().stampings.get(0).isIn())
                      ? personDay.get().stampings.get(0).date.toLocalTime()
                      .toString("HH:mm").concat("- **:**")
                      : "- **:**".concat(personDay.get().stampings.get(0).date.toLocalTime()
                      .toString("HH:mm"));

              badStampingDays =
                  (inconsistentAbsenceTable.contains(person, thBadStampings))
                      ? inconsistentAbsenceTable.get(person, thBadStampings)
                      : new ArrayList<String>();
              badStampingDays.add(
                  personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stamp));
              inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);

              log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                  person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

              // se e' vuota => manca qualche timbratura
            } else if (pairStampings.isEmpty()) {

              log.info("Il turno di {} {} e' incompatibile con la sue  timbrature disallineate nel"
                  + " giorno {}", person.name, person.surname, personDay.get().date);

              badStampingDays =
                  (inconsistentAbsenceTable.contains(person, thBadStampings))
                      ? inconsistentAbsenceTable.get(person, thBadStampings)
                      : new ArrayList<String>();
              badStampingDays.add(
                  personShiftDay.date.toString("dd MMM").concat(" -> timbrature disaccoppiate"));
              inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);

              log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                  person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

              // controlla che le coppie di timbrature coprano
              // gli intervalli di prima e dopo pranzo
            } else {

              //Logger.debug("Controlla le timbrature");
              boolean okBeforeLunch = false;    // intervallo prima di pranzo coperto
              boolean okAfterLunch = false;        // intervallo dopo pranzo coperto

              String strStamp = "";

              // per ogni coppia di timbrature
              for (PairStamping pairStamping : pairStampings) {

                strStamp =
                    strStamp.concat(pairStamping.first.date.toString("HH:mm")).concat(" - ")
                        .concat(pairStamping.second.date.toString("HH:mm")).concat("  ");
                log.debug("Controllo la coppia {}", strStamp);

                // controlla se la coppia di timbrature interseca l'intervallo prima e dopo
                //pranzo del turno
                if (!pairStamping.second.date.toLocalTime().isBefore(startLunchTime)
                    && !pairStamping.first.date.toLocalTime().isAfter(startShift)) {
                  okBeforeLunch = true;
                }
                if (!pairStamping.second.date.toLocalTime().isBefore(endShift)
                    && !pairStamping.first.date.toLocalTime().isAfter(endLunchTime)) {
                  okAfterLunch = true;
                }
              }

              // se non ha coperto interamente i due intervalli, controlla se il tempo mancante al
              // completamento del turno sia <= 2 ore
              if (!okBeforeLunch || !okAfterLunch) {

                int workingMinutes = 0;
                LocalTime lowLimit;
                LocalTime upLimit;
                LocalTime newLimit;

                // scostamenti delle timbrature dalle fasce del turno
                int diffStartShift = 0;
                int diffStartLunchTime = 0;
                int diffEndLunchTime = 0;
                int diffEndShift = 0;

                // ingressi  euscite nella tolleranza dei 15 min
                boolean inTolleranceLimit = true;

                String stampings = "";

                log.info("Il turno di {} nel giorno {} non e' stato completato o c'e' stata una "
                        + "uscita fuori pausa pranzo - orario {}",
                    person, personDay.get().date, strStamp);
                log.debug("Esamino le coppie di timbrature");

                // per ogni coppia di timbrature
                for (PairStamping pairStamping : pairStampings) {

                  // l'intervallo di tempo lavorato interseca la parte del turno prima di pranzo
                  if ((pairStamping.first.date.toLocalTime().isBefore(startShift)
                      && pairStamping.second.date.toLocalTime().isAfter(startShift))
                      || (pairStamping.first.date.toLocalTime().isAfter(startShift)
                      && pairStamping.first.date.toLocalTime().isBefore(startLunchTime))) {

                    // conta le ore lavorate in turno prima di pranzo
                    lowLimit =
                        (pairStamping.first.date.toLocalTime().isBefore(startShift))
                            ? startShift : pairStamping.first.date.toLocalTime();
                    upLimit =
                        (pairStamping.second.date.toLocalTime().isBefore(startLunchTime))
                            ? pairStamping.second.date.toLocalTime() : startLunchTime;
                    workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
                    log.debug("N.1 - ss={} -- slt={} lowLimit={} upLimit={} workingMinutes={}",
                        startShift, startLunchTime, lowLimit, upLimit, workingMinutes);

                    // calcola gli scostamenti dalla prima fascia del turno tenendo conto dei
                    // 15 min di comporto se il turnista è entrato prima
                    if (pairStamping.first.date.toLocalTime().isBefore(startShift)) {
                      newLimit =
                          (pairStamping.first.date.toLocalTime().isBefore(startShift.minusMinutes(15)))
                              ? startShift.minusMinutes(15) : pairStamping.first.date.toLocalTime();
                      if (pairStamping.first.date.toLocalTime()
                          .isBefore(startShift.minusMinutes(15))) {
                        inTolleranceLimit = false;
                      }
                    } else {
                      // è entrato dopo
                      newLimit =
                          (pairStamping.first.date.toLocalTime().isAfter(startShift.plusMinutes(15)))
                              ? startShift.plusMinutes(15) : pairStamping.first.date.toLocalTime();
                      if (pairStamping.first.date.toLocalTime().isAfter(startShift.plusMinutes(15))) {
                        inTolleranceLimit = false;
                      }
                    }
                    diffStartShift =
                        DateUtility.getDifferenceBetweenLocalTime(newLimit, startShift);
                    log.debug("diffStartShift={}", diffStartShift);

                    // calcola gli scostamenti dell'ingresso in pausa pranzo tenendo conto dei
                    // 15 min di comporto se il turnista è andato a  pranzo prima
                    if (pairStamping.second.date.toLocalTime().isBefore(startLunchTime)) {
                      log.trace("vedo uscita per pranzo prima");
                      newLimit =
                          (startLunchTime.minusMinutes(15)
                              .isAfter(pairStamping.second.date.toLocalTime()))
                              ? startLunchTime.minusMinutes(15) : pairStamping.second.date.toLocalTime();
                      diffStartLunchTime =
                          DateUtility.getDifferenceBetweenLocalTime(newLimit, startLunchTime);
                      if (startLunchTime.minusMinutes(15)
                          .isAfter(pairStamping.second.date.toLocalTime())) {
                        inTolleranceLimit = false;
                      }
                    } else if (pairStamping.second.date.toLocalTime().isBefore(endLunchTime)) {
                      // è andato a pranzo dopo
                      log.debug("vedo uscita per pranzo dopo");
                      newLimit =
                          (startLunchTime.plusMinutes(15)
                              .isAfter(pairStamping.second.date.toLocalTime()))
                              ? pairStamping.second.date.toLocalTime() : startLunchTime.plusMinutes(15);
                      if (startLunchTime.plusMinutes(15)
                          .isBefore(pairStamping.second.date.toLocalTime())) {
                        inTolleranceLimit = false;
                      }
                      diffStartLunchTime =
                          /* ? */
                          DateUtility.getDifferenceBetweenLocalTime(startLunchTime, newLimit);
                    }

                    log.debug("diffStartLunchTime=getDifferenceBetweenLocalTime({}, {})={}",
                        startLunchTime, newLimit, diffStartLunchTime);
                  }

                  // l'intervallo di tempo lavorato interseca la parte del turno dopo pranzo
                  if ((pairStamping.first.date.toLocalTime().isBefore(endLunchTime)
                      && pairStamping.second.date.toLocalTime().isAfter(endLunchTime))
                      || (pairStamping.first.date.toLocalTime().isAfter(endLunchTime)
                      && pairStamping.first.date.toLocalTime().isBefore(endShift))) {

                    // conta le ore lavorate in turno dopo pranzo
                    lowLimit =
                        (pairStamping.first.date.toLocalTime().isBefore(endLunchTime))
                            ? endLunchTime : pairStamping.first.date.toLocalTime();
                    upLimit =
                        (pairStamping.second.date.toLocalTime().isBefore(endShift))
                            ? pairStamping.second.date.toLocalTime() : endShift;
                    workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
                    log.debug("N.2 - elt={} --- es={}  slowLimit={} upLimit={} workingMinutes={}",
                        endLunchTime, endShift, lowLimit, upLimit, workingMinutes);

                    // calcola gli scostamenti dalla seconda fascia del turno tenendo conto dei
                    // 15 min di comporto se il turnista è rientrato prima dalla pausa pranzo
                    if (pairStamping.first.date.toLocalTime().isBefore(endLunchTime)
                        && pairStamping.first.date.toLocalTime().isAfter(startLunchTime)) {
                      log.trace("vedo rientro da pranzo prima");
                      newLimit =
                          (endLunchTime.minusMinutes(15)
                              .isAfter(pairStamping.first.date.toLocalTime()))
                              ? endLunchTime.minusMinutes(15) : pairStamping.first.date.toLocalTime();
                      diffEndLunchTime =
                          DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
                      log.debug("diffEndLunchTime=getDifferenceBetweenLocalTime({}, {})={}",
                          newLimit, endLunchTime, diffEndLunchTime);
                    } else if (pairStamping.first.date.toLocalTime().isBefore(endShift)
                        && pairStamping.first.date.toLocalTime().isAfter(endLunchTime)) {
                      // è rientrato dopo
                      log.trace("vedo rientro da pranzo dopo");
                      newLimit =
                          (pairStamping.first.date.toLocalTime().isAfter(endLunchTime.plusMinutes(15)))
                              ? endLunchTime.plusMinutes(15) : pairStamping.first.date.toLocalTime();
                      if (pairStamping.first.date.toLocalTime()
                          .isAfter(endLunchTime.plusMinutes(15))) {
                        inTolleranceLimit = false;
                      }
                      diffEndLunchTime =
                          DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
                      log.debug("diffEndLunchTime=getDifferenceBetweenLocalTime({}, {})={}",
                          endLunchTime, newLimit, diffEndLunchTime);
                    }


                    // se il turnista è uscito prima del turno
                    if (pairStamping.second.date.toLocalTime().isBefore(endShift)) {
                      log.debug("vedo uscita prima della fine turno");
                      newLimit =
                          (endShift.minusMinutes(15).isAfter(pairStamping.second.date.toLocalTime()))
                              ? endShift.minusMinutes(15) : pairStamping.second.date.toLocalTime();
                      if (endShift.minusMinutes(15).isAfter(pairStamping.second.date.toLocalTime())) {
                        inTolleranceLimit = false;
                      }
                    } else {
                      log.trace("vedo uscita dopo la fine turno");
                      // il turnista è uscito dopo la fine del turno
                      newLimit =
                          (pairStamping.second.date.toLocalTime().isAfter(endShift.plusMinutes(15)))
                              ? endShift.plusMinutes(15) : pairStamping.second.date.toLocalTime();
                    }
                    diffEndShift = DateUtility.getDifferenceBetweenLocalTime(endShift, newLimit);
                    log.debug("diffEndShift={}", diffEndShift);
                  }

                  // write the pair stamping
                  stampings =
                      stampings.concat(pairStamping.first.date.toString("HH:mm")).concat("-")
                          .concat(pairStamping.second.date.toString("HH:mm")).concat("  ");
                }

                stampings.concat("<br />");

                // controllo eventuali compensazioni di minuti in  ingresso e uscita
                //--------------------------------------------------------------------
                int restoredMin = 0;


                // controlla pausa pranzo:
                // - se è uscito prima dell'inizio PP (è andato a pranzo prima)
                if (diffStartLunchTime < 0) {
                  log.debug("sono entrata in pausa pranzo prima! diffStartLunchTime={}",
                      diffStartLunchTime);
                  // controlla se è anche rientrato prima dalla PP e compensa
                  if (diffEndLunchTime > 0) {
                    log.debug("E rientrato prima dalla pusa pranzo! diffEndLunchTime={}",
                        diffEndLunchTime);
                    restoredMin +=
                        Math.min(Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));
                    log.debug("restoredMin={} Math.abs(diffStartLunchTime)={} "
                            + "Math.abs(diffEndLunchTime)={}",
                        restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));

                    diffStartLunchTime =
                        ((diffStartLunchTime + diffEndLunchTime) > 0)
                            ? 0 : diffStartLunchTime + diffEndLunchTime;
                    diffEndLunchTime =
                        ((diffStartLunchTime + diffEndLunchTime) > 0)
                            ? diffStartLunchTime + diffEndLunchTime : 0;

                  }
                  // se necessario e se è entrato prima, compensa con l'ingresso
                  if ((diffStartLunchTime < 0) && (diffStartShift > 0)) {
                    log.debug("E entrato anche prima! diffStartShift={}", diffStartShift);
                    // cerca di compensare con l'ingresso
                    restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffStartShift));
                    log.debug("restoredMin={} Math.abs(diffStartLunchTime)={} "
                            + "Math.abs(diffStartShift)={}",
                        restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffStartShift));

                    diffStartLunchTime =
                        ((diffStartLunchTime + diffStartShift) > 0)
                            ? 0 : diffStartLunchTime + diffStartShift;
                    diffStartShift =
                        ((diffStartLunchTime + diffStartShift) > 0)
                            ? diffStartLunchTime + diffStartShift : 0;
                  }
                }

                // - se è entrato dopo la fine della pausa pranzo
                if (diffEndLunchTime < 0) {
                  log.debug("E entrato in ritardo dalla pausa pranzo! diffEndLunchTime={}",
                      diffEndLunchTime);
                  // controlla che sia entrata dopo in pausa pranzo
                  if (diffStartLunchTime > 0) {
                    log.debug("e andata anche dopo in pausa pranzo! diffStartLunchTime={}",
                        diffStartLunchTime);
                    restoredMin
                        += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));
                    log.debug("restoredMin={} Math.abs(diffStartLunchTime)={} "
                            + "Math.abs(diffEndLunchTime)={}",
                        restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));

                    diffEndLunchTime =
                        ((diffEndLunchTime + diffStartLunchTime) > 0)
                            ? 0 : diffEndLunchTime + diffStartLunchTime;
                    diffStartLunchTime =
                        ((diffEndLunchTime + diffStartLunchTime) > 0)
                            ? diffEndLunchTime + diffStartLunchTime : 0;

                  }
                  // se necessario e se è uscito dopo, compensa con l'uscita
                  if ((diffEndLunchTime < 0) && (diffEndShift > 0)) {
                    log.debug("e' uscito dopo! diffEndShift={}", diffEndShift);
                    // cerca di conpensare con l'uscita (è uscito anche dopo)
                    restoredMin += Math.min(Math.abs(diffEndLunchTime), Math.abs(diffEndShift));
                    log.debug("restoredMin={} Math.abs(diffEndLunchTime)={} "
                            + "Math.abs(diffEndShift)={}",
                        restoredMin, Math.abs(diffEndLunchTime), Math.abs(diffEndShift));

                    diffEndLunchTime =
                        ((diffEndLunchTime + diffEndShift) > 0)
                            ? 0 : diffEndLunchTime + diffEndShift;
                    diffEndShift =
                        ((diffEndLunchTime + diffEndShift) > 0)
                            ? diffEndLunchTime + diffEndShift : 0;
                  }
                }

                // controlla eventuali compensazioni di ingresso e uscita
                // controlla se è uscito dopo
                if ((diffStartShift < 0) && (diffEndShift > 0)) {
                  log.debug("e entrato dopo ed è uscito dopo! diffStartShift={} diffEndShift={}",
                      diffStartShift, diffEndShift);
                  restoredMin += Math.min(Math.abs(diffEndShift), Math.abs(diffStartShift));
                  log.debug("restoredMin={} Math.abs(diffEndShift)={} "
                          + "Math.abs(diffStartShift)={}",
                      restoredMin, Math.abs(diffEndShift), Math.abs(diffStartShift));

                  diffStartShift = ((diffEndShift + diffStartShift) > 0)
                      ? 0 : diffEndShift + diffStartShift;
                  diffEndShift = ((diffEndShift + diffStartShift) > 0)
                      ? diffEndShift + diffStartShift : 0;

                } else if ((diffEndShift < 0) && (diffStartShift > 0)) {
                  log.debug("e uscito prima ed è entrato dopo! diffStartShift={} diffEndShift={}",
                      diffStartShift, diffEndShift);
                  restoredMin += Math.min(Math.abs(diffEndShift), Math.abs(diffStartShift));
                  log.debug("restoredMin={} Math.abs(diffEndShift)={} "
                          + "Math.abs(diffStartShift)={}",
                      restoredMin, Math.abs(diffEndShift), Math.abs(diffStartShift));

                  diffEndShift = ((diffEndShift + diffStartShift) > 0)
                      ? 0 : diffEndShift + diffStartShift;
                  diffStartShift = ((diffEndShift + diffStartShift) > 0)
                      ? diffEndShift + diffStartShift : 0;

                }

                log.debug("Minuti recuperati: {}", restoredMin);

                // check if the difference between the worked hours in the shift periods are less
                // than 2 hours (new rules for shift)
                int teoreticShiftMinutes =
                    DateUtility.getDifferenceBetweenLocalTime(startShift, startLunchTime)
                        + DateUtility.getDifferenceBetweenLocalTime(endLunchTime, endShift);
                int lackOfMinutes = teoreticShiftMinutes - workingMinutes;

                log.debug("teoreticShiftMinutes = {} workingMinutes = {} lackOfMinutes = {}",
                    teoreticShiftMinutes, workingMinutes, lackOfMinutes);
                lackOfMinutes -= restoredMin;
                workingMinutes += restoredMin;

                log.debug("Minuti mancanti con recupero: {} - Minuti lavorati con recupero: {}",
                    lackOfMinutes, workingMinutes);

                String lackOfTime = calcStringShiftHoursFromMinutes(lackOfMinutes);
                String workedTime = calcStringShiftHoursFromMinutes(workingMinutes);
                String label;

                int twoHoursinMinutes = 2 * 60;

                if (lackOfMinutes > twoHoursinMinutes) {

                  log.info("Il turno di {} {} nel giorno {} non e' stato completato - "
                          + "timbrature: {}",
                      person.name, person.surname, personDay.get().date, stampings);

                  badStampingDays = (inconsistentAbsenceTable.contains(person, thMissingTime))
                      ? inconsistentAbsenceTable.get(person, thMissingTime)
                      : Lists.<String>newArrayList();
                  badStampingDays.add(
                      personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings)
                          .concat("(").concat(workedTime).concat(" ore lavorate)"));
                  inconsistentAbsenceTable.put(person, thMissingTime, badStampingDays);

                  log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                      person, thMissingTime, inconsistentAbsenceTable.get(person, thMissingTime));
                } else if (lackOfMinutes != 0) {
                  label = (inTolleranceLimit) ? thIncompleteTime : thWarnStampings;

                  log.info("Il turno di {} {} nel giorno {} non e'stato completato per meno di "
                          + "2 ore ({} minuti ({})) - CONTROLLARE PERMESSO timbrature: {}",
                      person.name, person.surname, personDay.get().date, lackOfMinutes,
                      lackOfTime, stampings);
                  log.info("Timbrature nella tolleranza dei 15 min. = {}", inTolleranceLimit);

                  badStampingDays = (inconsistentAbsenceTable.contains(person, label))
                      ? inconsistentAbsenceTable.get(person, label) : new ArrayList<String>();
                  badStampingDays.add(
                      personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings)
                          .concat("(").concat(lackOfTime).concat(" ore mancanti)"));

                  lackOfTimes =
                      (inconsistentAbsenceTable.contains(person, thLackTime))
                          ? inconsistentAbsenceTable.get(person, thLackTime)
                          : Lists.<String>newArrayList();
                  lackOfTimes.add(Integer.toString(lackOfMinutes));
                  inconsistentAbsenceTable.put(person, label, badStampingDays);
                  inconsistentAbsenceTable.put(person, thLackTime, lackOfTimes);

                  log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                      person, thLackTime, inconsistentAbsenceTable.get(person, thLackTime));
                }
              }
            } // fine controllo coppie timbrature
          } // fine if esistenza timbrature
        } // fine se non √® giorno festivo

        // check for absences
        if (!personDay.get().absences.isEmpty()) {
          log.debug("E assente!!!! Esamino le assenze({})", personDay.get().absences.size());
          for (Absence absence : personDay.get().absences) {
            if (absence.justifiedType.name == JustifiedTypeName.all_day) {

              if (absence.absenceType.code.equals("92")) {
                log.info("Il turno di {} {} e' coincidente con una missione il giorno {}",
                    person.name, person.surname, personShiftDay.date);

                absenceDays =
                    (inconsistentAbsenceTable.contains(person, thMissions))
                        ? inconsistentAbsenceTable.get(person, thMissions) : new ArrayList<String>();
                absenceDays.add(personShiftDay.date.toString("dd MMM"));
                inconsistentAbsenceTable.put(person, thMissions, absenceDays);


                absenceDays =
                    (inconsistentAbsenceTable.contains(person, thAbsences))
                        ? inconsistentAbsenceTable.get(person, thAbsences) : new ArrayList<String>();
                absenceDays.add(personShiftDay.date.toString("dd MMM"));
                inconsistentAbsenceTable.put(person, thAbsences, absenceDays);
              }
            }
          }
        }
      } // fine personDay != null
    }
    //return inconsistentAbsenceTable;
  }
}

