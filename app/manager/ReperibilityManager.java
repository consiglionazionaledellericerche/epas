/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import helpers.BadRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Competence;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.absences.Absence;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.ReperibilityPeriod;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import play.Logger;
import play.i18n.Messages;

/**
 * Gestore delle funzionalità relative alle reperibilità.
 *
 * @author Arianna Del Soldato
 * @author Dario Tagliaferri
 */
@Slf4j
public class ReperibilityManager {

  //codice dei turni feriali
  private static final String codFr = "207";
  //codice dei turni festivi
  private static final String codFs = "208";
  // Label della tabella delle inconsistenze delle reperibilità con le timbrature

  // nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
  public static String thNoStampings = Messages.get("PDFReport.thNoStampings");
  // nome della colonna per i giorni di assenza della tabella delle inconsistenze
  public static String thAbsences = Messages.get("PDFReport.thAbsences");
  private final AbsenceDao absenceDao;
  private final PersonDao personDao;
  private final PersonReperibilityDayDao personReperibilityDayDao;
  private final PersonDayDao personDayDao;
  private final PersonDayManager personDayManager;

  /**
   * Injection.
   *
   * @param absenceDao il dao sulle assenze
   * @param personDao il dao sulle persone
   * @param personReperibilityDayDao il dao sulle reperibilità
   * @param personDayManager il manager dei metodi dei personday
   * @param personDayDao il dao del personday
   * @param competenceCodeDao il dao sui codici di competenza
   * @param officeDao il dao sulle sedi
   */
  @Inject
  public ReperibilityManager(
      AbsenceDao absenceDao, PersonDao personDao,
      PersonReperibilityDayDao personReperibilityDayDao, PersonDayManager personDayManager,
      PersonDayDao personDayDao, CompetenceCodeDao competenceCodeDao, OfficeDao officeDao) {
    this.absenceDao = absenceDao;
    this.personDao = personDao;
    this.personReperibilityDayDao = personReperibilityDayDao;
    this.personDayDao = personDayDao;
    this.personDayManager = personDayManager;
  }

  /**
   * La lista dei periodi di reperibilità effettuati.
   *
   * @param reperibilityDays la lista dei giorni di reperibilità effettuati.
   * @param prt il tipo di reperibilità
   * @return la lista dei periodi di reperibilità effettuati.
   */
  public List<ReperibilityPeriod> getPersonReperibilityPeriods(
      List<PersonReperibilityDay> reperibilityDays, PersonReperibilityType prt) {

    List<ReperibilityPeriod> reperibilityPeriods = new ArrayList<>();
    ReperibilityPeriod reperibilityPeriod = null;

    for (PersonReperibilityDay prd : reperibilityDays) {
      //L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di
      // reperibilità non consecutivi.
      if (reperibilityPeriod == null
          || !reperibilityPeriod.person.equals(prd.getPersonReperibility().getPerson())
          || !reperibilityPeriod.end.plusDays(1).equals(prd.getDate())) {
        reperibilityPeriod =
            new ReperibilityPeriod(prd.getPersonReperibility().getPerson(), 
                prd.getDate(), prd.getDate(), prt);
        reperibilityPeriods.add(reperibilityPeriod);
        log.trace("Creato nuovo reperibilityPeriod, person={}, start={}, end={}",
            reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
      } else {
        reperibilityPeriod.end = prd.getDate();
        log.trace("Aggiornato reperibilityPeriod, person={}, start={}, end={}",
            reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
      }
    }
    return reperibilityPeriods;
  }


  /**
   * Ritorna la lista dei periodi di assenza in reperibilità.
   *
   * @param absencePersonReperibilityDays lista dei giorni di assenza in reperibilita'.
   * @param reperibilityType tipo di reperibilita'
   * @return lista di periodi di assenza in reperibilità.
   */
  public List<AbsenceReperibilityPeriod> getAbsentReperibilityPeriodsFromAbsentReperibilityDays(
      List<Absence> absencePersonReperibilityDays, PersonReperibilityType reperibilityType) {
    List<AbsenceReperibilityPeriod> absenceReperibilityPeriods = Lists.newArrayList();
    AbsenceReperibilityPeriod absenceReperibilityPeriod = null;

    for (Absence abs : absencePersonReperibilityDays) {
      //L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di
      // reperibilità non consecutivi.
      if (absenceReperibilityPeriod == null
          || !absenceReperibilityPeriod.person.equals(abs.getPersonDay().getPerson())
          || !absenceReperibilityPeriod.end.plusDays(1).equals(abs.getPersonDay().getDate())) {
        absenceReperibilityPeriod =
            new AbsenceReperibilityPeriod(
                abs.getPersonDay().getPerson(), abs.getPersonDay().getDate(), 
                abs.getPersonDay().getDate(), reperibilityType);
        absenceReperibilityPeriods.add(absenceReperibilityPeriod);
        log.trace("Creato nuovo absenceReperibilityPeriod, person={}, start={}, end={}",
            absenceReperibilityPeriod.person, absenceReperibilityPeriod.start,
            absenceReperibilityPeriod.end);
      } else {
        absenceReperibilityPeriod.end = abs.getPersonDay().getDate();
        log.trace("Aggiornato reperibilityPeriod, person={}, start={}, end={}",
            absenceReperibilityPeriod.person, absenceReperibilityPeriod.start,
            absenceReperibilityPeriod.end);
      }
    }

    return absenceReperibilityPeriods;
  }

  /**
   * L'insieme delle reperibilità salvate.
   *
   * @param reperibilityType la tipologia di servizio di reperibilità
   * @param year l'anno
   * @param month il mese
   * @param reperibilityPeriods i periodi di reperibilità
   * @return l'insieme delle reperibilità salvate.
   */
  public Set<Integer> savePersonReperibilityDaysFromReperibilityPeriods(
      PersonReperibilityType reperibilityType, Integer year, Integer month,
      List<ReperibilityPeriod> reperibilityPeriods) {

    //Il mese e l'anno ci servono per "azzerare" eventuale giorni di reperibilità rimasti vuoti
    LocalDate monthToManage = new LocalDate(year, month, 1);

    //Conterrà i giorni del mese che devono essere attribuiti a qualche reperibile
    Set<Integer> daysOfMonthToDelete = new HashSet<>();
    for (int i = 1; i <= monthToManage.dayOfMonth().withMaximumValue().getDayOfMonth(); i++) {
      daysOfMonthToDelete.add(i);
    }
    Logger.trace("Lista dei giorni del mese = %s", daysOfMonthToDelete);

    LocalDate day = null;

    for (ReperibilityPeriod reperibilityPeriod : reperibilityPeriods) {

      reperibilityPeriod.reperibilityType = reperibilityType;

      if (reperibilityPeriod.start.isAfter(reperibilityPeriod.end)) {
        throw new IllegalArgumentException(
            String.format(
                "ReperibilityPeriod person.id = %s has start date %s after end date %s",
                reperibilityPeriod.person.id, reperibilityPeriod.start, reperibilityPeriod.end));
      }

      day = reperibilityPeriod.start;
      while (day.isBefore(reperibilityPeriod.end.plusDays(1))) {

        //La persona deve essere tra i reperibili
        if (reperibilityPeriod.person.getReperibility() == null) {
          throw new IllegalArgumentException(
              String.format("Person %s is not a reperible person", reperibilityPeriod.person));
        }

        //Se la persona è assente in questo giorno non può essere reperibile
        if (absenceDao.getAbsencesInPeriod(
            Optional.fromNullable(reperibilityPeriod.person), day,
            Optional.absent(), false)
            .size() > 0) {
          String msg =
              String.format("La reperibilità di %s %s e' incompatibile con la sua assenza nel "
                  + "giorno %s",
                  reperibilityPeriod.person.getName(), reperibilityPeriod.person.getSurname(), day);
          BadRequest.badRequest(msg);
        }

        //Salvataggio del giorno di reperibilità
        //Se c'è un giorno di reperibilità già presente viene sostituito, altrimenti viene creato
        //un PersonReperibilityDay nuovo
        PersonReperibilityDay personReperibilityDay =
            personReperibilityDayDao
            .getPersonReperibilityDayByTypeAndDate(reperibilityPeriod.reperibilityType, day);

        if (personReperibilityDay == null) {
          personReperibilityDay = new PersonReperibilityDay();
          log.trace("Creo un nuovo personReperibilityDay per person = {}, day = {}, "
              + "reperibilityDay = {}",
              reperibilityPeriod.person, day, reperibilityPeriod.reperibilityType);
        } else {
          log.trace("Aggiorno il personReperibilityDay = {}", personReperibilityDay);
        }

        personReperibilityDay.setPersonReperibility(personReperibilityDayDao
            .getPersonReperibilityByPersonAndType(reperibilityPeriod.person, reperibilityType));
        personReperibilityDay.setDate(day);
        personReperibilityDay.setReperibilityType(reperibilityPeriod.reperibilityType);
        //XXX: manca ancora l'impostazione dell'eventuale holidayDay, ovvero se si tratta
        //di un giorno festivo

        personReperibilityDay.save();

        //Questo giorno è stato assegnato
        daysOfMonthToDelete.remove(day.getDayOfMonth());

        log.info("Inserito o aggiornata reperibilità di tipo {}, assegnata a {} per il giorno {}",
            personReperibilityDay.getReperibilityType(), 
            personReperibilityDay.getPersonReperibility().getPerson(),
            personReperibilityDay.getDate());

        day = day.plusDays(1);
      }
    }

    return daysOfMonthToDelete;
  }


  /**
   * Cancella dal DB i giorni di reperibilità di un certo tipo associati ad un determinato
   * mese ed anno.
   *
   * @param reperibilityType tipo delle reperibilità da cancellare
   * @param year anno di appartenenza dei giorni di reperibilità da cancellare
   * @param month mese di appartenenza dei giorni di reperibilità da cancellare
   * @param repDaysToRemove lista dei giorni da cancellare
   * @return numero di giorni di reperibilità cancellati
   */
  public int deleteReperibilityDaysFromMonth(
      PersonReperibilityType reperibilityType, int year, int month, Set<Integer> repDaysToRemove) {

    long cancelled = 0;

    for (int dayToRemove : repDaysToRemove) {
      LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
      log.trace("Eseguo la cancellazione del giorno {}", dateToRemove);

      cancelled =
          personReperibilityDayDao.deletePersonReperibilityDay(reperibilityType, dateToRemove);
      if (cancelled == 1) {
        log.info("Rimossa reperibilità di tipo {} del giorno {}",
            reperibilityType, dateToRemove);
      }
    }

    return (int) cancelled;
  }

  /**
   * Change the person between two reperibility periods of a certain type.
   *
   * @param reperibilityType tipo di reperibilità a cui appartengono i periodi
   * @param periods lista di due periodi di repribilità da scambiare
   * @return boolean che indica se il cambio è stato effettuato
   */
  public Boolean changeTwoReperibilityPeriods(
      PersonReperibilityType reperibilityType, List<ReperibilityPeriod> periods) {

    LocalDate reqStartDay = null;
    LocalDate subStartDay = null;
    LocalDate reqEndDay = null;
    LocalDate subEndDay = null;
    Person requestor = null;
    Person substitute = null;

    Boolean repChanged = true;

    for (ReperibilityPeriod reperibilityPeriod : periods) {

      reperibilityPeriod.reperibilityType = reperibilityType;

      if (reperibilityPeriod.start.isAfter(reperibilityPeriod.end)) {
        throw new IllegalArgumentException(
            String.format(
                "ReperibilityPeriod person.id = %s has start date %s after end date %s",
                reperibilityPeriod.person.id, reperibilityPeriod.start, reperibilityPeriod.end));
      }

      //La persona deve essere tra i reperibili
      if (reperibilityPeriod.person.getReperibility() == null) {
        throw new IllegalArgumentException(
            String.format("Person %s is not a reperible person", reperibilityPeriod.person));
      }

      // intervallo del richiedente
      if (repChanged) {
        reqStartDay = reperibilityPeriod.start;
        reqEndDay = reperibilityPeriod.end;
        requestor = reperibilityPeriod.person;

        log.debug("RICHIEDENTE: requestor={} inizio={}, fine={}",
            requestor, reqStartDay, reqEndDay);
        repChanged = !repChanged;
      } else {
        subStartDay = reperibilityPeriod.start;
        subEndDay = reperibilityPeriod.end;
        substitute = reperibilityPeriod.person;

        log.debug("SOSTITUTO: substitute={} inizio={}, fine={}",
            substitute, subStartDay, subEndDay);

        int day = 1000 * 60 * 60 * 24;

        // controlla che il numero dei giorni da scambiare coincida
        if (((reqEndDay.toDate().getTime() - reqStartDay.toDate().getTime()) / day)
            != ((subEndDay.toDate().getTime() - subStartDay.toDate().getTime()) / day)) {
          throw new IllegalArgumentException(
              String.format("Different number of days between two intervals!"));
        }

        log.debug("Aggiorno i giorni del richiedente");
        changePersonInReperibilityPeriod(
            reperibilityPeriod.reperibilityType, reqStartDay, reqEndDay, requestor, substitute);

        log.debug("Aggiorno i giorni del sostituto");
        changePersonInReperibilityPeriod(
            reperibilityPeriod.reperibilityType, subStartDay, subEndDay, substitute, requestor);

        repChanged = !repChanged;
      }
    }

    return repChanged;
  }


  /**
   * Cambia le persone in reperibilità.
   *
   * @param reperibilityType type of a reperibility.
   * @param startDay data di inizio del periodo di reperibilità
   * @param endDay data di fine del periodo di reperibilità
   * @param requestor persona che ha richiesto il cambio reperibilità
   * @param substitute persona che va a sostituire il richiedente nei giorni di reperibilità
   */
  public void changePersonInReperibilityPeriod(
      PersonReperibilityType reperibilityType, LocalDate startDay, LocalDate endDay,
      Person requestor, Person substitute) {

    LocalDate start = startDay;

    // Esegue il cambio sui giorni del richiedente

    while (start.isBefore(endDay.plusDays(1))) {

      //Se il sostituto è in ferie questo giorno non può essere reperibile
      if (absenceDao.getAbsencesInPeriod(
          Optional.fromNullable(substitute), start, Optional.absent(), false)
          .size() > 0) {
        throw new IllegalArgumentException(
            String.format(
                "ReperibilityPeriod substitute.id %s is not compatible with a Absence in the same "
                    + "day %s",
                    substitute, start));
      }

      // cambia le reperibilità mettendo quelle del sostituto al posto di quelle del richiedente
      PersonReperibilityDay personReperibilityDay =
          personReperibilityDayDao.getPersonReperibilityDayByTypeAndDate(reperibilityType, start);

      log.debug("trovato personReperibilityDay.personReperibility.person={} e reqstart={}",
          personReperibilityDay.getPersonReperibility().getPerson(), startDay);

      if (personReperibilityDay == null
          || (personReperibilityDay.getPersonReperibility().getPerson() != requestor)) {
        throw new IllegalArgumentException(
            String.format(
                "Impossible to offer the day %s because is not associated to the right "
                    + "requestor %s", start, requestor));
      } else {
        log.debug("Aggiorno il personReperibilityDay = {}", personReperibilityDay);
        PersonReperibility substituteRep =
            personReperibilityDayDao
            .getPersonReperibilityByPersonAndType(substitute, reperibilityType);
        personReperibilityDay.setPersonReperibility(substituteRep);

        log.info("scambio reperibilità del richiedente con "
            + "personReperibilityDay.personReperibility.person={} e reqstart={}",
            personReperibilityDay.getPersonReperibility().getPerson(), 
            personReperibilityDay.getDate());
      }

      personReperibilityDay.save();

      log.info("Aggiornato PersonReperibilityDay del richiedente= {}", personReperibilityDay);

      start = start.plusDays(1);
    }

  }


  /**
   * Ritorna la list di reperibili coinvolti nei giorni di reperibilità.
   *
   * @param personReperibilityDays lista di giorni di reperibilità effettuati dai reperibili.
   * @return lista dei reperibili coinvolti nei giorni di reperibilità passati come parametro.
   */
  public List<Person> getPersonsFromReperibilityDays(
      List<PersonReperibilityDay> personReperibilityDays) {
    List<Person> personList = new ArrayList<>();

    for (PersonReperibilityDay prd : personReperibilityDays) {
      if (!personList.contains(prd.getPersonReperibility().getPerson())) {
        log.trace("inserisco il reperibile {}", prd.getPersonReperibility().getPerson());
        personList.add(prd.getPersonReperibility().getPerson());
        log.trace("trovata person={}", prd.getPersonReperibility().getPerson());
      }
    }

    return personList;
  }


  /**
   * Costrisce il calendario delle reperibilità lavorate festive e feriali di un determinato anno.
   *
   * @param year anno di riferimento del calendario
   * @param reperibilityType tipo di reperibilità di cui costrire il calendario
   * @return Lista di tabelle (una per ogni mese dell'anno) contenenti per ogni persona e giorno del
   *        mese l'indicazione se la persona ha effettuato un turno di reperibilità festivo (FS) o 
   *        feriale (FR)
   */
  public List<Table<Person, Integer, String>> buildYearlyReperibilityCalendar(
      int year, PersonReperibilityType reperibilityType) {

    List<Table<Person, Integer, String>> reperibilityMonths =
        new ArrayList<>();

    // for each month of the year
    for (int i = 1; i <= 12; i++) {

      LocalDate firstOfMonth = new LocalDate(year, i, 1);

      List<PersonReperibilityDay> personReperibilityDays =
          personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(
              firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), reperibilityType,
              Optional.absent());

      // table associated to the current month of the year
      ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder();
      Table<Person, Integer, String> reperibilityMonth = null;

      for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
        Person person = personReperibilityDay.getPersonReperibility().getPerson();
        builder.put(
            person, personReperibilityDay.getDate().getDayOfMonth(),
            personDayManager.isHoliday(person, personReperibilityDay.getDate()) ? "FS" : "FR");
      }
      reperibilityMonth = builder.build();
      reperibilityMonths.add(reperibilityMonth);
    }

    return reperibilityMonths;
  }


  /**
   * Costruisce il resoconto del numero dei giorni lavorati in reperibilità festiva e feriale
   * sulla base di un calendario annuale passato come parametro.
   *
   * @param reperibilityMonths contiene una tabella per ogni mese di un anno che contiene per ogni
   *        giorno e persona coinvolta se ha lavorato una reperibilità festiva o feriale
   * @return tabella che contiene per ogni
   */
  public Table<Person, String, Integer> buildYearlyReperibilityReport(
      List<Table<Person, Integer, String>> reperibilityMonths) {

    int i = 0;
    Table<Person, String, Integer> reperibilitySumDays =
        HashBasedTable.create();

    // fro each month of the calendar
    for (Table<Person, Integer, String> reperibilityMonth : reperibilityMonths) {
      i++;
      // for each person counts the worked days in reperibility
      // divided by holiday or not
      for (Person person : reperibilityMonth.rowKeySet()) {
        for (Integer dayOfMonth : reperibilityMonth.columnKeySet()) {
          if (reperibilityMonth.contains(person, dayOfMonth)) {
            String col =
                String.format("%s%dS",
                    reperibilityMonth.get(person, dayOfMonth).toUpperCase(), (i <= 6 ? 1 : 2));

            int n =
                reperibilitySumDays.contains(person, col)
                ? reperibilitySumDays.get(person, col) + 1 : 1;
            reperibilitySumDays.put(person, col, Integer.valueOf(n));
          } else {
            //TODO qualcosa.....
          }
        }
      }
    }

    return reperibilitySumDays;
  }


  /**
   * Salva il riepilogo dei giorni di reperibilit√† e le reason di un certo mesenel database.
   * La reason contiene la descrizione dei periodi di reperibilit√† effettuati
   * in quel mese.
   *
   * @param personReperibilityDays list of repribility days worked by persons
   * @param year anno di riferimento dei giorni di reperibilità passati come parametro
   * @param month mese di riferimento dei giorni di  reperibilità passati come parametro
   * @return numero di competenze salvate nel DB
   * @author arianna
   */
  public int updateDbReperibilityCompetences(
      List<PersonReperibilityDay> personReperibilityDays, int year, int month) {

    /**
     * single person reperibility period in a month.
     */
    class Prp {

      int inizio;
      int fine;
      String mese;
      String tipo;

      public Prp(int inizio, int fine, String mese, String tipo) {
        this.inizio = inizio;
        this.fine = fine;
        this.mese = mese;
        this.tipo = tipo;
      }

      @Override
      public String toString() {
        return (this.inizio != this.fine)
            ? String.format("%d-%d/%s", inizio, fine, mese) : String.format("%d/%s", inizio, mese);
      }
    }

    /**
     * single person reperibility day.
     */
    class Prd {

      int giorno;
      String tipo;

      public Prd(int giorno, String tipo) {
        this.giorno = giorno;
        this.tipo = tipo;
      }
    }

    // get the Competence code for the reperibility working or non-working days
    // CompetenceCode competenceCodeFs = competenceCodeDao.getCompetenceCodeByCode(codFs);
    // CompetenceCode competenceCodeFr = competenceCodeDao.getCompetenceCodeByCode(codFr);

    // read the first day of the month and the short month description
    LocalDate firstOfMonth = new LocalDate(year, month, 1);
    String shortMonth = firstOfMonth.monthOfYear().getAsShortText();


    // for each person contains the reperibility days (fs/fr) in the month
    ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder();
    Table<Person, Integer, String> reperibilityMonth = null;

    // build the reperibility calendar with the reperibility days
    for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
      Person person = personReperibilityDay.getPersonReperibility().getPerson();

      // record the reperibility day
      builder.put(
          person, personReperibilityDay.getDate().getDayOfMonth(),
          personDayManager.isHoliday(person, personReperibilityDay.getDate()) ? codFs : codFr);

    }
    reperibilityMonth = builder.build();

    int numSavedCompetences = 0;

    // for each person in the reperibilitymonth conunts the reperibility day
    // divided by fs and fr and build a string description of the rep periods
    for (Person person : reperibilityMonth.rowKeySet()) {

      // lista dei periodi di reperibilit√† ferieali e festivi
      List<Prp> fsPeriods = new ArrayList<>();
      List<Prp> frPeriods = new ArrayList<>();

      Prd previousPersonReperibilityDay = null;
      Prp currentPersonReperibilityPeriod = null;

      // number of working and non-working days
      int numOfFsDays = 0;
      int numOfFrDays = 0;

      // for each day of month
      for (Integer dayOfMonth : reperibilityMonth.columnKeySet()) {

        // counts the reperibility days fs and fr
        if (reperibilityMonth.contains(person, dayOfMonth)) {
          if (reperibilityMonth.get(person, dayOfMonth).equals(codFr)) {
            numOfFrDays++;
          } else {
            numOfFsDays++;
          }
        }

        // create the reperibility periods strings divided by fs and fr
        if (reperibilityMonth.contains(person, dayOfMonth)) {
          if ((previousPersonReperibilityDay == null)
              || (!reperibilityMonth.get(person, dayOfMonth)
                  .equals(previousPersonReperibilityDay.tipo))
              || ((dayOfMonth - 1) != previousPersonReperibilityDay.giorno)) {
            currentPersonReperibilityPeriod =
                new Prp(
                    dayOfMonth, dayOfMonth, shortMonth,
                    reperibilityMonth.get(person, dayOfMonth));

            if (currentPersonReperibilityPeriod.tipo == codFs) {
              fsPeriods.add(currentPersonReperibilityPeriod);
            } else {
              frPeriods.add(currentPersonReperibilityPeriod);
            }
          } else {
            currentPersonReperibilityPeriod.fine = dayOfMonth;
          }
          previousPersonReperibilityDay =
              new Prd(dayOfMonth, reperibilityMonth.get(person, dayOfMonth));
        }
      }

      log.debug("NumOfFsDays={} fsPeriods={} - NumOfFrDays={} frPeriods={} ",
          numOfFsDays, fsPeriods, numOfFrDays, frPeriods);

      // build the Fs and Fr reasons
      String fsReason = "";
      String frReason = "";
      for (Prp prp : fsPeriods) {
        fsReason = fsReason.concat(prp.toString().concat(" "));
      }
      for (Prp prp : frPeriods) {
        frReason = frReason.concat(prp.toString().concat(" "));
      }
      log.debug("ReasonFS={} ReasonFR={}", fsReason, frReason);


    }

    // return the number of saved competences
    return numSavedCompetences;
  }


  /**
   * Aggiorna la reperibilità a partire dalle competenze.
   *
   * @param competenceList lista di competenze.
   * @param personsApprovedCompetence tabella contnente per ogni persona, coinvolta nelle competenze
   *        passate come parametro, e per ogni tipo di competenza, il valore approvato per quella
   *        competenza.
   */
  public void updateReperibilityDaysReportFromCompetences(
      Table<Person, String, Integer> personsApprovedCompetence, List<Competence> competenceList) {
    for (Competence competence : competenceList) {
      log.debug("-- Metto nella tabella competence = {}", competence.toString());
      log.debug("La tabella contiene {} ? {}",
          competence.getPerson(), personsApprovedCompetence.containsRow(competence.getPerson()));
      personsApprovedCompetence.put(
          competence.getPerson(), competence.getCompetenceCode().getCodeToPresence(), 
          competence.getValueApproved());
    }
  }


  /**
   * Aggiorna le date del report della reperibilità a partire dalle competenze.
   *
   * @param competenceList lista di competenze.
   * @param reperibilityDateDays tabella contnente per ogni persona, coinvolta nelle competenze
   *        passate come eparametro, e per ogni tipo di competenza, il valore della reason
   */
  public void updateReperibilityDatesReportFromCompetences(
      Table<Person, String, List<String>> reperibilityDateDays, List<Competence> competenceList) {
    for (Competence competence : competenceList) {
      log.debug("Metto nella tabella competence = {}", competence.toString());
      List<String> str = (competence.getReason() != null)
          ? Arrays.asList(competence.getReason().split(" ")) : Arrays.asList(" ");
      reperibilityDateDays.put(competence.getPerson(), 
          competence.getCompetenceCode().getCodeToPresence(), str);
    }
  }


  /**
   * Crea una tabella con le eventuali inconsistenze tra le timbrature dei
   * reperibili di un certo tipo e i turni di reperibilit√† svolti in un determinato periodo di
   * tempo ritorna una tabella del tipo (Person, [thNoStamping, thAbsence], List['gg/MMM']).
   *
   * @author Arianna Del Soldato
   */
  public Table<Person, String, List<String>> getReperibilityInconsistenceAbsenceTable(
      List<PersonReperibilityDay> personReperibilityDays,
      LocalDate startDate, LocalDate endDate) {
    // for each person contains days with absences and no-stamping  matching the reperibility days
    Table<Person, String, List<String>> inconsistentAbsenceTable =
        HashBasedTable.create();

    // lista dei giorni di assenza e mancata timbratura
    List<String> noStampingDays = new ArrayList<>();
    List<String> absenceDays = new ArrayList<>();

    for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
      Person person = personReperibilityDay.getPersonReperibility().getPerson();

      //check for the absence inconsistencies
      //------------------------------------------

      Optional<PersonDay> personDay = personDayDao.getPersonDay(person, 
          personReperibilityDay.getDate());

      // if there are no events and it is not an holiday -> error
      if (!personDay.isPresent() || (!personDay.get().isHoliday() 
          && personDay.get().getStampings().isEmpty()
          && personDay.get().getAbsences().isEmpty())) {
        if (!personDayManager.isHoliday(person, personReperibilityDay.getDate())) {
          log.info("La reperibilità di {} {} è incompatibile con la sua mancata timbratura nel "
              + "giorno {}", person.getName(), person.getSurname(), 
              personReperibilityDay.getDate());

          noStampingDays =
              (inconsistentAbsenceTable.contains(person, thNoStampings))
              ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<>();
          noStampingDays.add(personReperibilityDay.getDate().toString("dd MMM"));
          inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
        }
      } else if (!personDay.get().isHoliday() && !personDay.get().getAbsences().isEmpty()) {
        // check for absences
        for (Absence absence : personDay.get().getAbsences()) {
          if (!absence.getAbsenceType().isReperibilityCompatible()) {
            log.info("La reperibilità di {} è incompatibile con la sua assenza nel "
                + "giorno {}", person.getFullname(), personReperibilityDay.getDate());

            absenceDays =
                (inconsistentAbsenceTable.contains(person, thAbsences))
                ? inconsistentAbsenceTable.get(person, thAbsences) : new ArrayList<>();
            absenceDays.add(personReperibilityDay.getDate().toString("dd MMM"));
            inconsistentAbsenceTable.put(person, thAbsences, absenceDays);
          }
        }
      }
    }

    return inconsistentAbsenceTable;
  }


  /**
   * Export the reperibility calendar in iCal for the person with id = personId with reperibility
   * of type 'type' for the 'year' year.
   * If the personId=0, it exports the calendar for all  the reperibility persons of type 'type'
   */
  public Optional<Calendar> createCalendar(
      Long type, Optional<Long> personId, int year) {
    log.debug("Crea iCal per l'anno {} della person con id = {}, reperibility type {}",
        year, personId, type);

    // check for the parameter
    //---------------------------

    Optional<PersonReperibility> personReperibility = Optional.absent();
    if (personId.isPresent()) {
      // read the reperibility person
      personReperibility =
          Optional.fromNullable(
              personReperibilityDayDao.getPersonReperibilityByPersonAndType(
                  personDao.getPersonById(personId.get()),
                  personReperibilityDayDao.getPersonReperibilityTypeById(type)));
      if (!personReperibility.isPresent()) {
        log.info("Person id = {} is not associated to a reperibility of type = {}",
            personId.get(), type);
        return Optional.absent();
      }
    }

    log.debug("chiama la createicsReperibilityCalendar({}, {}, {})",
        year, type, personReperibility);
    Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
    icsCalendar = createicsReperibilityCalendar(year, type, personReperibility);

    log.debug("la createicsReperibilityCalendar ha trovato {} periodi di reperibilità.",
        icsCalendar.getComponents().size());
    log.debug("Crea iCal per l'anno {} della person con id = {}, reperibility type {}",
        year, personId, type);

    return Optional.of(icsCalendar);
  }

  /**
   * Ritorna un calendario con le ics nei giorni di reperibilità.
   *
   * @param year l'anno
   * @param type il tipo di reperibilità
   * @param personReperibility la personReperibility
   * @return un Calendar che inserisce le ics in reperibilità.
   */
  public Calendar createicsReperibilityCalendar(
      int year, Long type, Optional<PersonReperibility> personReperibility) {

    String eventLabel;
    List<PersonReperibility> personsInTheCalList = new ArrayList<>();

    // Create a calendar
    //---------------------------
    Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
    icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
    icsCalendar.getProperties().add(CalScale.GREGORIAN);
    icsCalendar.getProperties().add(Version.VERSION_2_0);

    // read the person(0) reperibility days for the year
    //-------------------------------------------------
    LocalDate from = new LocalDate(year, 1, 1);
    LocalDate to = new LocalDate(year, 12, 31);

    if (!personReperibility.isPresent()) {
      // read the reperibility person
      personsInTheCalList =
          personReperibilityDayDao.getPersonReperibilityByType(
              personReperibilityDayDao.getPersonReperibilityTypeById(type));
    } else {
      personsInTheCalList.add(personReperibility.get());
    }

    for (PersonReperibility personRep : personsInTheCalList) {

      eventLabel = (personsInTheCalList.size() == 1)
          ? "Reperibilità Registro" : "Reperibilità ".concat(personRep.getPerson().getSurname());
      List<PersonReperibilityDay> reperibilityDays =
          personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(
              from, to, personRep.getPersonReperibilityType(), Optional.of(personRep));

      log.debug("Reperibility find called from {} to {}, found {} reperibility days for person"
          + " id = {}", from, to, reperibilityDays.size(), personRep.getPerson().id);

      log.debug("Calcola i periodi di reperibilità");

      Date startDate = null;
      Date endDate = null;
      int sequence = 1;

      for (PersonReperibilityDay prd : reperibilityDays) {

        Date date = new Date(prd.getDate()
            .toDateTimeAtStartOfDay(DateTimeZone.UTC).toDate().getTime());

        if (startDate == null) {
          log.debug("Nessun periodo, nuovo periodo: startDate={}", date);

          startDate = endDate = date;
          sequence = 1;
          continue;
        }

        if (date.getTime() - endDate.getTime() > 86400 * 1000) {
          log.debug("Memorizza periodo: startDate={}, sequence={}", startDate, sequence);
          icsCalendar.getComponents().add(createIcalEvent(startDate, sequence, eventLabel));
          startDate = endDate = date;
          sequence = 1;
          log.trace("Nuovo periodo: startDate={}", date);
        } else {
          sequence++;
          endDate = date;
          log.debug("Allungamento periodo: startDate={}, endDate={}, sequence.new={}",
              startDate, endDate, sequence);
        }

      }

      log.debug("Memorizzo l'ultimo periodo: startDate={}, sequence={}", startDate, sequence);
      icsCalendar.getComponents().add(createIcalEvent(startDate, sequence, eventLabel));
    }

    return icsCalendar;
  }


  private VEvent createIcalEvent(Date startDate, int sequence, String eventLabel) {
    VEvent reperibilityPeriod = new VEvent(startDate, Duration.ofDays(sequence), eventLabel);
    reperibilityPeriod.getProperties().add(new Uid(UUID.randomUUID().toString()));
    return reperibilityPeriod;
  }

}