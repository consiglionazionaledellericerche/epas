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

package it.cnr.iit.epas;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import dao.PersonDayDao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
import manager.services.PairStamping;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.ShiftSlot;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import play.i18n.Messages;

/**
 * Classe di utilità per la gestione delle competenze.
 *
 */
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

  private final PersonDayDao personDayDao;
  private final PersonDayManager personDayManager;

  /**
   * Construttore predefinito che inizializza queryFactory e vari manager.
   */
  @Inject
  public CompetenceUtility(PersonDayDao personDayDao, PersonDayManager personDayManager) {

    this.personDayDao = personDayDao;
    this.personDayManager = personDayManager;
  }

  /**
   * Calcola le ore di turno dai giorni (days) resto = (days%2 == 0) ? 0 : 0.5 ore = days*6 +
   * (int)(days/2) + resto; TODO: parametrico rispetto ale pre del DB (ampo ore retribuite).
   *
   * @author Arianna Del Soldato 
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
   * Calcola le ore di turno dai giorni (days) resto = (days%2 == 0) ? 0 : 0.5 ore = days*6 +
   * (int)(days/2) + resto. TODO: parametrico rispetto ale pre del DB (ampo ore retribuite).
   *
   * @author Arianna Del Soldato
   */
  public BigDecimal calcShiftHoursFromDays2(int days) {
    BigDecimal decDays = new BigDecimal(days);

    // read the defined shift time associated to the shift type

    BigDecimal due = new BigDecimal("2");

    BigDecimal minutes = (days % 2 == 0) ? BigDecimal.ZERO : new BigDecimal("0.5");
    BigDecimal hours =
        decDays.multiply(
            new BigDecimal(6)).add(decDays.divide(due, RoundingMode.HALF_DOWN)).add(minutes);

    log.debug("La calcShiftHoursFromDays restituisce hours = {}", hours);

    return hours;
  }


  /**
   * Calcola le oer e i minuti dal numero totale dei minuti lavorati.
   *
   * @author Arianna Del Soldato
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
   * Calcola il LocalTime dal numero dei minuti che compongono l'orario.
   *
   * @author Arianna Del Soldato
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

    return Integer.toString(hours).concat(".").concat(Integer.toString(mins));
  }

  /**
   * Aggiorna la tabella totalPersonShiftSumDays per contenere, per ogni persona nella lista dei
   * turni personShiftDays, e per ogni tipo di turno trovato, il numero di giorni di turno
   * effettuati.
   *
   * @param personShiftDays lista di shiftDays
   * @param personShiftSumDaysForTypes tabella contenente il numero di giorni di turno effettuati
   *     per ogni persona e tipologia di turno. Questa tabella viene aggiornata contando i giorni di
   *     turno contenuti nella lista personShiftDays passata come parametro
   * @author Arianna Del Soldato
   */
  public void countPersonsShiftsDays(
      List<PersonShiftDay> personShiftDays,
      Table<Person, String, Integer> personShiftSumDaysForTypes) {

    // for each person and dy in the month contains worked shift (A/B)
    ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder();
    Table<Person, Integer, String> shiftMonth = null;

    // for each person contains the number of days of working shift divided by shift's type

    for (PersonShiftDay personShiftDay : personShiftDays) {
      Person person = personShiftDay.getPersonShift().getPerson();

      // registro il turno della persona per quel giorno
      //------------------------------------------------------
      builder.put(person, personShiftDay.getDate().getDayOfMonth(), 
          personShiftDay.getShiftType().getType());
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
   * crea una tabella con le eventuali inconsistenze tra le timbrature dei reperibili di un certo
   * tipo e i turni di reperibilit√† svolti in un determinato periodo di tempo ritorna una tabella
   * del tipo (Person, [thNoStamping, thAbsence], List<'gg/MMM '>).
   *
   * @author Arianna Del Soldato
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
      Person person = personReperibilityDay.getPersonReperibility().getPerson();

      //check for the absence inconsistencies
      //------------------------------------------

      Optional<PersonDay> personDay = personDayDao.getPersonDay(person, 
          personReperibilityDay.getDate());

      // if there are no events and it is not an holiday -> error
      if (!personDay.isPresent() & LocalDate.now().isAfter(personReperibilityDay.getDate())) {
        //if (!person.isHoliday(personReperibilityDay.date)) {
        if (!personDayManager.isHoliday(person, personReperibilityDay.getDate())) {
          log.debug("La reperibilità di {} {} è incompatibile con la sua mancata timbratura nel "
              + "giorno {}", person.getName(), person.getSurname(), 
              personReperibilityDay.getDate());

          noStampingDays =
              (inconsistentAbsenceTable.contains(person, thNoStampings))
                  ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
          noStampingDays.add(personReperibilityDay.getDate().toString("dd MMM"));
          inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
        }
      } else if (LocalDate.now().isAfter(personReperibilityDay.getDate())) {
        // check for the stampings in working days
        if (!personDayManager.isHoliday(person, personReperibilityDay.getDate())
            && personDay.get().getStampings().isEmpty()) {
          log.debug("La reperibilità di {} {} è incompatibile con la sua mancata timbratura nel "
              + "giorno {}", person.getName(), person.getSurname(), personDay.get().getDate());

          noStampingDays =
              (inconsistentAbsenceTable.contains(person, thNoStampings))
                  ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
          noStampingDays.add(personReperibilityDay.getDate().toString("dd MMM"));
          inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
        }

        // check for absences
        if (!personDay.get().getAbsences().isEmpty()) {
          for (Absence absence : personDay.get().getAbsences()) {
            if (absence.getJustifiedType().getName() == JustifiedTypeName.all_day) {
              log.debug("La reperibilita' di {} {} e' incompatibile con la sua assenza nel "
                  + "giorno {}", person.getName(), person.getSurname(), 
                  personReperibilityDay.getDate());

              absenceDays =
                  (inconsistentAbsenceTable.contains(person, thAbsences))
                      ? inconsistentAbsenceTable.get(person, thAbsences) : new ArrayList<String>();
              absenceDays.add(personReperibilityDay.getDate().toString("dd MMM"));
              inconsistentAbsenceTable.put(person, thAbsences, absenceDays);
            }
          }
        }
      }
    }

    return inconsistentAbsenceTable;
  }


  /**
   * Costruisce la tabella delle inconsistenza tra i giorni di turno dati e le timbrature ritorna
   * una tabella del tipo (Person, [thNoStamping, thAbsence, thBadStampings], [List<'gg MMM'>,
   * List<'gg MMM'>, 'dd MMM -> HH:mm-HH:mm']).
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
      Person person = personShiftDay.getPersonShift().getPerson();

      // legge l'orario di inizio e fine turno da rispettare (mattina o pomeriggio)
      LocalTime startShift =
          (personShiftDay.getOrganizationShiftSlot().getName().equals(ShiftSlot.MORNING.name()))
              ? personShiftDay.getShiftType().getShiftTimeTable().getStartMorning()
              : personShiftDay.getShiftType().getShiftTimeTable().getStartAfternoon();
      LocalTime endShift =
          (personShiftDay.getOrganizationShiftSlot().getName().equals(ShiftSlot.MORNING.name()))
              ? personShiftDay.getShiftType().getShiftTimeTable().getEndMorning()
              : personShiftDay.getShiftType().getShiftTimeTable().getEndAfternoon();

      // legge l'orario di inizio e fine pausa pranzo del turno
      LocalTime startLunchTime =
          (personShiftDay.getOrganizationShiftSlot().getName().equals(ShiftSlot.MORNING.name()))
              ? personShiftDay.getShiftType().getShiftTimeTable().getStartMorningLunchTime()
              : personShiftDay.getShiftType().getShiftTimeTable().getStartAfternoonLunchTime();
      LocalTime endLunchTime =
          (personShiftDay.getOrganizationShiftSlot().getName().equals(ShiftSlot.MORNING.name()))
              ? personShiftDay.getShiftType().getShiftTimeTable().getEndMorningLunchTime()
              : personShiftDay.getShiftType().getShiftTimeTable().getEndAfternoonLunchTime();

      //Logger.debug("Turno: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);

      // Add flexibility (15 min.) due to the new rules (PROT. N. 0008692 del 2/12/2014)
      LocalTime roundedStartShift = startShift.plusMinutes(15);

      log.debug("Turno: {}-{}  {}-{}", startShift, startLunchTime, endLunchTime, endShift);

      //check for the absence inconsistencies
      //------------------------------------------
      Optional<PersonDay> personDay = personDayDao.getPersonDay(person, personShiftDay.getDate());
      log.debug("Prelevo il personDay {} per la persona {}", 
          personShiftDay.getDate(), person.getSurname());

      // if there are no events and it is not an holiday -> error
      if (!personDay.isPresent()) {

        if (!personDayManager.isHoliday(person, personShiftDay.getDate())
            && personShiftDay.getDate().isBefore(LocalDate.now())) {
          log.debug("Il turno di {} {} e' incompatibile con la sua mancata timbratura nel giorno"
              + " {} (personDay == null)", 
              person.getName(), person.getSurname(), personShiftDay.getDate());

          noStampingDays =
              (inconsistentAbsenceTable.contains(person, thNoStampings))
                  ? inconsistentAbsenceTable.get(person, thNoStampings)
                  : new ArrayList<String>();
          noStampingDays.add(personShiftDay.getDate().toString("dd MMM"));
          inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);

          log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
              person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
        }
      } else {

        // check for the stampings in working days
        if (!personDayManager.isHoliday(person, personShiftDay.getDate())
            && LocalDate.now().isAfter(personShiftDay.getDate())) {

          // check no stampings
          //-----------------------------
          if (personDay.get().getStampings().isEmpty()) {
            log.debug("Il turno di {} {} e' incompatibile con la sue mancate timbrature nel giorno"
                + " {}", person.getName(), person.getSurname(), personDay.get().getDate());

            noStampingDays =
                (inconsistentAbsenceTable.contains(person, thNoStampings))
                    ? inconsistentAbsenceTable.get(person, thNoStampings)
                    : new ArrayList<String>();
            noStampingDays.add(personShiftDay.getDate().toString("dd MMM"));
            inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);

            log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
          } else {
            // check consistent stampings
            //-----------------------------
            // legge le coppie di timbrature valide
            List<PairStamping> pairStampings =
                personDayManager.getValidPairStampings(personDay.get().getStampings());

            // se c'e' una timbratura guardo se e' entro il turno
            if ((personDay.get().getStampings().size() == 1)
                && ((personDay.get().getStampings().get(0).isIn()
                && personDay.get().getStampings().get(0).getDate().toLocalTime()
                .isAfter(roundedStartShift))
                || (personDay.get().getStampings().get(0).isOut()
                && personDay.get().getStampings().get(0).getDate().toLocalTime()
                .isBefore(roundedStartShift)))) {

              String stamp =
                  (personDay.get().getStampings().get(0).isIn())
                      ? personDay.get().getStampings().get(0).getDate().toLocalTime()
                      .toString("HH:mm").concat("- **:**")
                      : "- **:**".concat(
                          personDay.get().getStampings().get(0).getDate().toLocalTime()
                          .toString("HH:mm"));

              badStampingDays =
                  (inconsistentAbsenceTable.contains(person, thBadStampings))
                      ? inconsistentAbsenceTable.get(person, thBadStampings)
                      : new ArrayList<String>();
              badStampingDays.add(
                  personShiftDay.getDate().toString("dd MMM").concat(" -> ").concat(stamp));
              inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);

              log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                  person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

              // se e' vuota => manca qualche timbratura
            } else if (pairStampings.isEmpty()) {

              log.debug("Il turno di {} {} e' incompatibile con la sue  timbrature disallineate nel"
                  + " giorno {}", person.getName(), person.getSurname(), personDay.get().getDate());

              badStampingDays =
                  (inconsistentAbsenceTable.contains(person, thBadStampings))
                      ? inconsistentAbsenceTable.get(person, thBadStampings)
                      : new ArrayList<String>();
              badStampingDays.add(
                  personShiftDay.getDate().toString("dd MMM")
                    .concat(" -> timbrature disaccoppiate"));
              inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);

              log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                  person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

              // controlla che le coppie di timbrature coprano
              // gli intervalli di prima e dopo pranzo
            } else {

              boolean okBeforeLunch = false;    // intervallo prima di pranzo coperto
              boolean okAfterLunch = false;        // intervallo dopo pranzo coperto

              String strStamp = "";

              // per ogni coppia di timbrature
              for (PairStamping pairStamping : pairStampings) {

                strStamp =
                    strStamp.concat(pairStamping.first.getDate().toString("HH:mm")).concat(" - ")
                        .concat(pairStamping.second.getDate().toString("HH:mm")).concat("  ");
                log.debug("Controllo la coppia {}", strStamp);

                // controlla se la coppia di timbrature interseca l'intervallo prima e dopo
                //pranzo del turno
                if (!pairStamping.second.getDate().toLocalTime().isBefore(startLunchTime)
                    && !pairStamping.first.getDate().toLocalTime().isAfter(startShift)) {
                  okBeforeLunch = true;
                }
                if (!pairStamping.second.getDate().toLocalTime().isBefore(endShift)
                    && !pairStamping.first.getDate().toLocalTime().isAfter(endLunchTime)) {
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

                log.debug("Il turno di {} nel giorno {} non e' stato completato o c'e' stata una "
                        + "uscita fuori pausa pranzo - orario {}",
                    person, personDay.get().getDate(), strStamp);
                log.debug("Esamino le coppie di timbrature");

                // per ogni coppia di timbrature
                for (PairStamping pairStamping : pairStampings) {

                  // l'intervallo di tempo lavorato interseca la parte del turno prima di pranzo
                  if ((pairStamping.first.getDate().toLocalTime().isBefore(startShift)
                      && pairStamping.second.getDate().toLocalTime().isAfter(startShift))
                      || (pairStamping.first.getDate().toLocalTime().isAfter(startShift)
                      && pairStamping.first.getDate().toLocalTime().isBefore(startLunchTime))) {

                    // conta le ore lavorate in turno prima di pranzo
                    lowLimit =
                        (pairStamping.first.getDate().toLocalTime().isBefore(startShift))
                            ? startShift : pairStamping.first.getDate().toLocalTime();
                    upLimit =
                        (pairStamping.second.getDate().toLocalTime().isBefore(startLunchTime))
                            ? pairStamping.second.getDate().toLocalTime() : startLunchTime;
                    workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
                    log.debug("N.1 - ss={} -- slt={} lowLimit={} upLimit={} workingMinutes={}",
                        startShift, startLunchTime, lowLimit, upLimit, workingMinutes);

                    // calcola gli scostamenti dalla prima fascia del turno tenendo conto dei
                    // 15 min di comporto se il turnista è entrato prima
                    if (pairStamping.first.getDate().toLocalTime().isBefore(startShift)) {
                      newLimit =
                          (pairStamping.first.getDate().toLocalTime()
                              .isBefore(startShift.minusMinutes(15)))
                              ? startShift.minusMinutes(15) 
                                  : pairStamping.first.getDate().toLocalTime();
                      if (pairStamping.first.getDate().toLocalTime()
                          .isBefore(startShift.minusMinutes(15))) {
                        inTolleranceLimit = false;
                      }
                    } else {
                      // è entrato dopo
                      newLimit =
                          (pairStamping.first.getDate().toLocalTime()
                              .isAfter(startShift.plusMinutes(15)))
                              ? startShift.plusMinutes(15) 
                                  : pairStamping.first.getDate().toLocalTime();
                      if (pairStamping.first.getDate().toLocalTime()
                          .isAfter(startShift.plusMinutes(15))) {
                        inTolleranceLimit = false;
                      }
                    }
                    diffStartShift =
                        DateUtility.getDifferenceBetweenLocalTime(newLimit, startShift);
                    log.debug("diffStartShift={}", diffStartShift);

                    // calcola gli scostamenti dell'ingresso in pausa pranzo tenendo conto dei
                    // 15 min di comporto se il turnista è andato a  pranzo prima
                    if (pairStamping.second.getDate().toLocalTime().isBefore(startLunchTime)) {
                      log.trace("vedo uscita per pranzo prima");
                      newLimit =
                          (startLunchTime.minusMinutes(15)
                              .isAfter(pairStamping.second.getDate().toLocalTime()))
                              ? startLunchTime.minusMinutes(15)
                              : pairStamping.second.getDate().toLocalTime();
                      diffStartLunchTime =
                          DateUtility.getDifferenceBetweenLocalTime(newLimit, startLunchTime);
                      if (startLunchTime.minusMinutes(15)
                          .isAfter(pairStamping.second.getDate().toLocalTime())) {
                        inTolleranceLimit = false;
                      }
                    } else if (pairStamping.second.getDate().toLocalTime().isBefore(endLunchTime)) {
                      // è andato a pranzo dopo
                      log.debug("vedo uscita per pranzo dopo");
                      newLimit =
                          (startLunchTime.plusMinutes(15)
                              .isAfter(pairStamping.second.getDate().toLocalTime()))
                              ? pairStamping.second.getDate().toLocalTime()
                              : startLunchTime.plusMinutes(15);
                      if (startLunchTime.plusMinutes(15)
                          .isBefore(pairStamping.second.getDate().toLocalTime())) {
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
                  if ((pairStamping.first.getDate().toLocalTime().isBefore(endLunchTime)
                      && pairStamping.second.getDate().toLocalTime().isAfter(endLunchTime))
                      || (pairStamping.first.getDate().toLocalTime().isAfter(endLunchTime)
                      && pairStamping.first.getDate().toLocalTime().isBefore(endShift))) {

                    // conta le ore lavorate in turno dopo pranzo
                    lowLimit =
                        (pairStamping.first.getDate().toLocalTime().isBefore(endLunchTime))
                            ? endLunchTime : pairStamping.first.getDate().toLocalTime();
                    upLimit =
                        (pairStamping.second.getDate().toLocalTime().isBefore(endShift))
                            ? pairStamping.second.getDate().toLocalTime() : endShift;
                    workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
                    log.debug("N.2 - elt={} --- es={}  slowLimit={} upLimit={} workingMinutes={}",
                        endLunchTime, endShift, lowLimit, upLimit, workingMinutes);

                    // calcola gli scostamenti dalla seconda fascia del turno tenendo conto dei
                    // 15 min di comporto se il turnista è rientrato prima dalla pausa pranzo
                    if (pairStamping.first.getDate().toLocalTime().isBefore(endLunchTime)
                        && pairStamping.first.getDate().toLocalTime().isAfter(startLunchTime)) {
                      log.trace("vedo rientro da pranzo prima");
                      newLimit =
                          (endLunchTime.minusMinutes(15)
                              .isAfter(pairStamping.first.getDate().toLocalTime()))
                              ? endLunchTime.minusMinutes(15)
                              : pairStamping.first.getDate().toLocalTime();
                      diffEndLunchTime =
                          DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
                      log.debug("diffEndLunchTime=getDifferenceBetweenLocalTime({}, {})={}",
                          newLimit, endLunchTime, diffEndLunchTime);
                    } else if (pairStamping.first.getDate().toLocalTime().isBefore(endShift)
                        && pairStamping.first.getDate().toLocalTime().isAfter(endLunchTime)) {
                      // è rientrato dopo
                      log.trace("vedo rientro da pranzo dopo");
                      newLimit =
                          (pairStamping.first.getDate().toLocalTime()
                              .isAfter(endLunchTime.plusMinutes(15)))
                              ? endLunchTime.plusMinutes(15)
                              : pairStamping.first.getDate().toLocalTime();
                      if (pairStamping.first.getDate().toLocalTime()
                          .isAfter(endLunchTime.plusMinutes(15))) {
                        inTolleranceLimit = false;
                      }
                      diffEndLunchTime =
                          DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
                      log.debug("diffEndLunchTime=getDifferenceBetweenLocalTime({}, {})={}",
                          endLunchTime, newLimit, diffEndLunchTime);
                    }

                    // se il turnista è uscito prima del turno
                    if (pairStamping.second.getDate().toLocalTime().isBefore(endShift)) {
                      log.debug("vedo uscita prima della fine turno");
                      newLimit =
                          (endShift.minusMinutes(15)
                              .isAfter(pairStamping.second.getDate().toLocalTime()))
                              ? endShift.minusMinutes(15) 
                                  : pairStamping.second.getDate().toLocalTime();
                      if (endShift.minusMinutes(15)
                          .isAfter(pairStamping.second.getDate().toLocalTime())) {
                        inTolleranceLimit = false;
                      }
                    } else {
                      log.trace("vedo uscita dopo la fine turno");
                      // il turnista è uscito dopo la fine del turno
                      newLimit =
                          (pairStamping.second.getDate().toLocalTime()
                              .isAfter(endShift.plusMinutes(15)))
                              ? endShift.plusMinutes(15) 
                                  : pairStamping.second.getDate().toLocalTime();
                    }
                    diffEndShift = DateUtility.getDifferenceBetweenLocalTime(endShift, newLimit);
                    log.debug("diffEndShift={}", diffEndShift);
                  }

                  // write the pair stamping
                  stampings =
                      stampings.concat(pairStamping.first.getDate().toString("HH:mm")).concat("-")
                          .concat(pairStamping.second.getDate().toString("HH:mm")).concat("  ");
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

                  log.debug("Il turno di {} {} nel giorno {} non e' stato completato - "
                          + "timbrature: {}",
                      person.getName(), person.getSurname(), personDay.get().getDate(), stampings);

                  badStampingDays = (inconsistentAbsenceTable.contains(person, thMissingTime))
                      ? inconsistentAbsenceTable.get(person, thMissingTime)
                      : Lists.<String>newArrayList();
                  badStampingDays.add(
                      personShiftDay.getDate().toString("dd MMM").concat(" -> ").concat(stampings)
                          .concat("(").concat(workedTime).concat(" ore lavorate)"));
                  inconsistentAbsenceTable.put(person, thMissingTime, badStampingDays);

                  log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}",
                      person, thMissingTime, inconsistentAbsenceTable.get(person, thMissingTime));
                } else if (lackOfMinutes != 0) {
                  label = (inTolleranceLimit) ? thIncompleteTime : thWarnStampings;

                  log.debug("Il turno di {} {} nel giorno {} non e'stato completato per meno di "
                          + "2 ore ({} minuti ({})) - CONTROLLARE PERMESSO timbrature: {}",
                      person.getName(), person.getSurname(), personDay.get().getDate(), 
                      lackOfMinutes, lackOfTime, stampings);
                  log.debug("Timbrature nella tolleranza dei 15 min. = {}", inTolleranceLimit);

                  badStampingDays = (inconsistentAbsenceTable.contains(person, label))
                      ? inconsistentAbsenceTable.get(person, label) : new ArrayList<String>();
                  badStampingDays.add(
                      personShiftDay.getDate().toString("dd MMM").concat(" -> ").concat(stampings)
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
        if (!personDay.get().getAbsences().isEmpty()) {
          log.debug("E assente!!!! Esamino le assenze({})", personDay.get().getAbsences().size());
          for (Absence absence : personDay.get().getAbsences()) {
            if (absence.getJustifiedType().getName() == JustifiedTypeName.all_day) {

              if (absence.getAbsenceType().getCode().equals("92")) {
                log.debug("Il turno di {} {} e' coincidente con una missione il giorno {}",
                    person.getName(), person.getSurname(), personShiftDay.getDate());

                absenceDays =
                    (inconsistentAbsenceTable.contains(person, thMissions))
                        ? inconsistentAbsenceTable.get(person, thMissions)
                        : new ArrayList<String>();
                absenceDays.add(personShiftDay.getDate().toString("dd MMM"));
                inconsistentAbsenceTable.put(person, thMissions, absenceDays);

                absenceDays =
                    (inconsistentAbsenceTable.contains(person, thAbsences))
                        ? inconsistentAbsenceTable.get(person, thAbsences) :
                        new ArrayList<String>();
                absenceDays.add(personShiftDay.getDate().toString("dd MMM"));
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

