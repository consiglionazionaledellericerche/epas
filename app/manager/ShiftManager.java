package manager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;
import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.DateUtility;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.services.PairStamping;
import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonDay;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftCategories;
import models.ShiftType;
import models.absences.Absence;
import models.enumerate.ShiftSlot;
import models.enumerate.Troubles;
import models.exports.AbsenceShiftPeriod;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import play.i18n.Messages;


/**
 * Gestiore delle operazioni sui turni.
 *
 * @author arianna
 */
@Slf4j
public class ShiftManager {

  private class WorkedParameters {

    private boolean stampingOk;     // se ci sono problemi sulle timbrature
    private int workedTime;      // minuti lavorati in turno
    private int lackOfTime;      // minuti mancanti al completamento del turno

  }

  private static final String codShiftNight = "T2";
  private static final String codShiftHolyday = "T3";
  private static final String codShift = "T1";            // codice dei turni

  //nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
  public static String thNoStampings = Messages.get("PDFReport.thNoStampings");

  // nome della colonna per la giornata lavorativa non valida (non è festa & non ci sono 
  // assenze & tempo di lavoro è insufficiente)
  public static String thBadWorkindDay = Messages.get("PDFReport.thBadWorkindDay");

  //nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni
  public static String thBadStampings = Messages.get("PDFReport.thBadStampings");
  public static String thMissingTime = Messages.get("PDFReport.thMissingTime");
  public static String thIncompleteTime = Messages.get("PDFReport.thIncompleteTime");

  //nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni ma
  public static String thWarnStampings = Messages.get("PDFReport.thWarnStampings");

  // con le ore lavorate che discostano meno di 2 ore
  public static String thLackTime = Messages.get("PDFReport.thLackTime");

  //nome della colonna per i giorni di missione della tabella delle inconsistenze
  public static String thMissions = Messages.get("PDFReport.thMissions");

  //nome della colonna per i giorni di assenza della tabella delle inconsistenze
  public static String thAbsences = Messages.get("PDFReport.thAbsences");

  @Inject
  private PersonDayManager personDayManager;
  @Inject
  private PersonShiftDayDao personShiftDayDao;
  @Inject
  private PersonDayDao personDayDao;
  @Inject
  private AbsenceDao absenceDao;
  @Inject
  private ShiftDao shiftDao;
  @Inject
  private CompetenceUtility competenceUtility;
  @Inject
  private CompetenceCodeDao competenceCodeDao;
  @Inject
  private CompetenceDao competenceDao;
  @Inject
  private PersonMonthRecapDao personMonthRecapDao;
  @Inject
  private IWrapperFactory wrapperFactory;


  /**
   * Costruisce la tabella delle inconsistenza tra i giorni di turno dati e le timbrature.
   * <p>
   * Ritorna una tabella del tipo (Person, [thNoStamping, thAbsence, thBadStampings],
   * [List['gg MMM'], List['gg MMM'], 'dd MMM -> HH:mm-HH:mm'])
   * </p>
   *
   * @param personShiftDays lista di giorni di turno (PersonShiftDay)
   * @param inconsistentAbsenceTable tabella di tipo Person, String, List di String che contiene le
   *        eventuali inconsistenze rilevate per ogni persona che ha effettuato almeno un turno
   *        nella lista: - String: è una label della tabella tra: - List di String
   */

  public void getShiftInconsistencyTimestampTable(
      List<PersonShiftDay> personShiftDays,
      Table<Person, String, List<String>> inconsistentAbsenceTable, ShiftType shiftType) {

    // legge gli orari di inizio e fine turno da rispettare (mattina o pomeriggio)
    LocalTime morningStartShift = shiftType.shiftTimeTable.startMorning;
    LocalTime morningEndShift = shiftType.shiftTimeTable.endMorning;
    LocalTime afternoonStartShift = shiftType.shiftTimeTable.startAfternoon;
    LocalTime afternoonEndShift = shiftType.shiftTimeTable.endAfternoon;

    // legge gli orari di inizio e fine pausa pranzo del turno   
    LocalTime startMorningLunch = shiftType.shiftTimeTable.startMorningLunchTime;
    LocalTime endMorningLunch = shiftType.shiftTimeTable.endMorningLunchTime;
    LocalTime startAfternoonLunch = shiftType.shiftTimeTable.startAfternoonLunchTime;
    LocalTime endAfternoonLunch = shiftType.shiftTimeTable.endAfternoonLunchTime;

    // Add flexibility (15 min.) due to the new rules (PROT. N. 0008692 del 2/12/2014)
    //LocalTime roundedStartShift = startShift.plusMinutes(shiftType.tolerance);

    LocalTime startShift = null;
    LocalTime endShift = null;
    LocalTime startLunchTime = null;
    LocalTime endLunchTime = null;

    for (PersonShiftDay personShiftDay : personShiftDays) {
      Person person = personShiftDay.personShift.person;

      // legge gli orari del turno a seconda dello slot  associato
      // in quel giorno (mattina, pomeriggio)
      if (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) {
        startShift = morningStartShift;
        endShift = morningEndShift;
        startLunchTime = startMorningLunch;
        endLunchTime = endMorningLunch;
      } else {
        startShift = afternoonStartShift;
        endShift = afternoonEndShift;
        startLunchTime = startAfternoonLunch;
        endLunchTime = endAfternoonLunch;
      }

      //log.debug("Turno: {}-{}  {}-{}", startShift, startLunchTime, endLunchTime, endShift);

      //check for the absence inconsistencies
      //------------------------------------------
      Optional<PersonDay> personDay = personDayDao.getPersonDay(person, personShiftDay.date);
      log.debug("Prelevo il personDay {} per la persona {}", personShiftDay.date, person.surname);

      // if I am not in the future
      if (personDay.isPresent()) {
        // se non è una giornata valida di lavoro
        IWrapperPersonDay wrPersonDay = wrapperFactory.create(personDay.get());
        if (!personDayManager.isValidDay(personDay.get(), wrPersonDay)) {

          log.debug("NON è un giorno valido!");
          // check for absences
          if (personDayManager.isAllDayAbsences(personDay.get())) {

            if (personDayManager.isOnMission(personDay.get())) {
              // check for missions
              log.info("Il turno di {} {} e' coincidente con una missione il giorno {}",
                  person.name, person.surname, personShiftDay.date);
              updateCellOfTableOfInconsistency(inconsistentAbsenceTable, person, thMissions,
                  personDay.get().date.toString("dd MMM"));
            } else {
              log.info("Il turno di {} {} e' incompatibile con la sua assenza nel giorno {}",
                  person.name, person.surname, personShiftDay.date);
              updateCellOfTableOfInconsistency(inconsistentAbsenceTable, person, thAbsences,
                  personDay.get().date.toString("dd MMM"));
            }

          } else if (personDay.get().hasError(
              Troubles.NO_ABS_NO_STAMP)) {
            // check no stampings
            //-----------------------------
            log.info("Il turno di {} {} e' incompatibile con la sue mancate timbrature nel "
                + "giorno {}", person.name, person.surname, personDay.get().date);

            updateCellOfTableOfInconsistency(inconsistentAbsenceTable, person, thNoStampings,
                personShiftDay.date.toString("dd MMM"));
            //log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}", person, thNoStampings, 
            //      inconsistentAbsenceTable.get(person, thNoStampings));
          } else if ((personDay.get().stampings.size() == 1)
              && ((personDay.get().stampings.get(0).isIn() && personDay.get().stampings.get(0).date
              .toLocalTime().isAfter(startShift.plusMinutes(shiftType.entranceTolerance)))
              || (personDay.get().stampings.get(0).isOut() && personDay.get().stampings.get(0).date
              .toLocalTime().isBefore(startShift.plusMinutes(shiftType.entranceTolerance))))) {

            String stamp =
                (personDay.get().stampings.get(0).isIn()) ? personDay.get().stampings.get(0).date
                    .toLocalTime().toString("HH:mm").concat("- **:**")
                    : "- **:**".concat(
                        personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm"));

            updateCellOfTableOfInconsistency(inconsistentAbsenceTable, person, thBadStampings,
                personDay.get().date.toString("dd MMM").concat(" -> ").concat(stamp));
            log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}", personDay.get().person,
                thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

          } else if (personDayManager.getValidPairStampings(personDay.get().stampings).isEmpty()) {
            // there are no stampings
            log.info("Il turno di {} {} e' incompatibile con la sue  timbrature disallineate nel"
                + " giorno {}", person.name, person.surname, personDay.get().date);

            updateCellOfTableOfInconsistency(inconsistentAbsenceTable, person, thBadStampings,
                personDay.get().date.toString("dd MMM").concat(" -> timbrature disaccoppiate"));
            log.debug("Nuovo inconsistentAbsenceTable({}, {}) = {}", person, thBadStampings,
                inconsistentAbsenceTable.get(person, thBadStampings));

          } else {
            log.info("La giornata lavorativa di {} {} per il giorno {} non è valida", person.name,
                person.surname, personDay.get().date);
            updateCellOfTableOfInconsistency(inconsistentAbsenceTable, person, thBadWorkindDay,
                personShiftDay.date.toString("dd MMM"));
          }

        } else {
          log.debug("E' un giorno valido!");
          // check consistent stampings
          //----------------------------

          // get the working time parameters in the shift period (worked and missed time 
          // during the shift period )

          WorkedParameters wp = checkShiftWorkedMins(personDay, shiftType, startShift,
              startLunchTime, endLunchTime, endShift);

          if (!wp.stampingOk) {
            String lackOfTime = competenceUtility.calcStringShiftHoursFromMinutes(wp.lackOfTime);
            String workedTime = competenceUtility.calcStringShiftHoursFromMinutes(wp.workedTime);

            log.debug("lackOfTime = {} workedTime = {}", lackOfTime, workedTime);

          } // fine if esistenza timbrature    
        } // fine check of working days
      } // fine personDay is present
    } // fine ciclo sui personShiftDays  
  }


  /*
   * aggiorna il contenuto della cella di una tabella del tipo <Person, String, List<String>>
   * tipicamente utilizzata per contenere le inconsistenze tra i turni e le timbrature
   * 
   * @param table tabella da aggiornare
   * @param th stringa identificativa della colonna
   * @param tr persona identificativa della riga
   * @param element stringa da aggiungere alla cella
   */
  private static void updateCellOfTableOfInconsistency(Table<Person, String, List<String>> table,
      Person tr, String th, String element) {

    List<String> cell = new ArrayList<String>();

    // prende l'elemento della tabella, se esiste, altrimenti lo crea
    cell = (table.contains(tr, th)) ? table.get(tr, th) : new ArrayList<String>();

    cell.add(element);
    table.put(tr, th, cell);
  }


  /*
   * 
   */

  WorkedParameters checkShiftWorkedMins(Optional<PersonDay> personDay,
      ShiftType shiftType, LocalTime startShift, LocalTime startLunchTime,
      LocalTime endLunchTime, LocalTime endShift) {

    WorkedParameters wp = new WorkedParameters();
    wp.stampingOk = true;

    int restoredMin = 0;
    int workingMinutes = 0;

    // ingressi  e uscite nella tolleranza dei 15 min
    boolean inTolleranceLimit = true;
    String stampings = "";
    Person person = personDay.get().person;

    // legge le coppie di timbrature valide
    List<PairStamping> pairStampings = personDayManager
        .getValidPairStampings(personDay.get().stampings);

    boolean okBeforeLunch = false;    // intervallo prima di pranzo coperto
    boolean okAfterLunch = false;        // intervallo dopo pranzo coperto

    String strStamp = "";

    // per ogni coppia di timbrature
    for (PairStamping pairStamping : pairStampings) {

      strStamp = strStamp.concat(pairStamping.first.date.toString("HH:mm")).concat(" - ")
          .concat(pairStamping.second.date.toString("HH:mm")).concat("  ");
      //log.debug("Controllo la coppia {}", strStamp);

      // controlla se la coppia di timbrature interseca l'intervallo prima e dopo
      // pranzo del turno
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

      wp.stampingOk = false;

      LocalTime lowLimit;
      LocalTime upLimit;
      LocalTime newLimit;

      // scostamenti delle timbrature dalle fasce del turno
      int diffStartShift = 0;
      int diffStartLunchTime = 0;
      int diffEndLunchTime = 0;
      int diffEndShift = 0;

      log.info(
          "Il turno di {} nel giorno {} non e' stato completato o c'e' stata una uscita "
          + "fuori pausa pranzo - orario {}",
          person, personDay.get().date, strStamp);

      // per ogni coppia di timbrature
      for (PairStamping pairStamping : pairStampings) {

        // l'intervallo di tempo lavorato interseca la parte del turno prima di pranzo
        //---------------------------------------------------------------------------
        if ((pairStamping.first.date.toLocalTime().isBefore(startShift)
            && pairStamping.second.date.toLocalTime().isAfter(startShift))
            || (pairStamping.first.date.toLocalTime().isAfter(startShift)
            && pairStamping.first.date.toLocalTime().isBefore(startLunchTime))) {

          // prende l'intervallo di intersezione tra le timbrature e 
          // e la parte di turno prima di pranzo
          lowLimit = (pairStamping.first.date.toLocalTime().isBefore(startShift)) ? startShift
              : pairStamping.first.date.toLocalTime();
          upLimit = (pairStamping.second.date.toLocalTime().isBefore(startLunchTime))
              ? pairStamping.second.date.toLocalTime() : startLunchTime;

          // conta le ore lavorate in turno prima di pranzo   
          workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
          log.debug("N.1 - ss={} -- slt={} lowLimit={} upLimit={} workingMinutes={}", startShift,
              startLunchTime, lowLimit, upLimit, workingMinutes);

          // calcola gli scostamenti dall'ingresso tenendo conto della tolleranza
          //--------------------------------------------------------------------------------------
          // min di comporto se il turnista è entrato prima
          // FIXME: anche qui, occorrerebbe testare prima che tipo di tolleranza è attribuita 
          // all'attività e, nel caso si tratti di tolleranza sull'entrata, applicarla.
          if (pairStamping.first.date.toLocalTime().isBefore(startShift)) {
            if (pairStamping.first.date.toLocalTime()
                .isBefore(startShift.minusMinutes(shiftType.entranceTolerance))) {
              newLimit = startShift.minusMinutes(shiftType.entranceTolerance);
              inTolleranceLimit = false;
            } else {
              newLimit = pairStamping.first.date.toLocalTime();
            }
          } else {
            // è entrato dopo
            if (pairStamping.first.date.toLocalTime()
                .isAfter(startShift.plusMinutes(shiftType.entranceTolerance))) {
              newLimit = startShift.plusMinutes(shiftType.entranceTolerance);
              inTolleranceLimit = false;
            } else {
              newLimit = pairStamping.first.date.toLocalTime();
            }
          }

          diffStartShift = DateUtility.getDifferenceBetweenLocalTime(newLimit, startShift);
          //log.debug("diffStartShift={}", diffStartShift);

          // calcola gli scostamenti dell'ingresso in pausa pranzo tenendo conto della tolleranza
          //--------------------------------------------------------------------------------------
          // se il turnista è andato a  pranzo prima
          // FIXME: anche qui occorre controllare che tipo di tolleranza è applicata all'attività 
          // e, se si tratta di tolleranza in entrata, applicarla
          if (pairStamping.second.date.toLocalTime().isBefore(startLunchTime)) {
            //log.debug("vedo uscita per pranzo prima");

            if (startLunchTime.minusMinutes(shiftType.entranceTolerance)
                .isAfter(pairStamping.second.date.toLocalTime())) {
              newLimit = startLunchTime.minusMinutes(shiftType.entranceTolerance);
              inTolleranceLimit = false;
            } else {
              newLimit = pairStamping.second.date.toLocalTime();
            }

            diffStartLunchTime = DateUtility
                .getDifferenceBetweenLocalTime(newLimit, startLunchTime);

          } else if (pairStamping.second.date.toLocalTime().isBefore(endLunchTime)) {
            // è andato a pranzo dopo
            //log.debug("vedo uscita per pranzo dopo");
            if (startLunchTime.plusMinutes(shiftType.entranceTolerance)
                .isAfter(pairStamping.second.date.toLocalTime())) {
              newLimit = pairStamping.second.date.toLocalTime();
            } else {
              newLimit = startLunchTime.plusMinutes(shiftType.entranceTolerance);
            }

            if (startLunchTime.plusMinutes(shiftType.entranceTolerance)
                .isBefore(pairStamping.second.date.toLocalTime())) {
              inTolleranceLimit = false;
            }

            diffStartLunchTime = DateUtility
                .getDifferenceBetweenLocalTime(startLunchTime, newLimit);
          }

          //log.debug("diffStartLunchTime=getDifferenceBetweenLocalTime({}, {})={}",
          //  startLunchTime, newLimit, diffStartLunchTime);
        }

        // l'intervallo di tempo lavorato interseca la parte del turno dopo pranzo
        if ((pairStamping.first.date.toLocalTime().isBefore(endLunchTime)
            && pairStamping.second.date.toLocalTime().isAfter(endLunchTime))
            || (pairStamping.first.date.toLocalTime().isAfter(endLunchTime)
            && pairStamping.first.date.toLocalTime().isBefore(endShift))) {

          // conta le ore lavorate in turno dopo pranzo
          lowLimit = (pairStamping.first.date.toLocalTime().isBefore(endLunchTime)) ? endLunchTime
              : pairStamping.first.date.toLocalTime();
          upLimit =
              (pairStamping.second.date.toLocalTime().isBefore(endShift)) ? pairStamping.second.date
                  .toLocalTime() : endShift;

          workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
          log.debug("N.2 - elt={} --- es={}  slowLimit={} upLimit={} workingMinutes={}",
              endLunchTime, endShift, lowLimit, upLimit, workingMinutes);

          // calcola gli scostamenti dalla seconda fascia del turno tenendo conto della tolleranza
          // --------------------------------------------------------------------------
          // 15 min di comporto se il turnista è rientrato prima dalla pausa pranzo
          if (pairStamping.first.date.toLocalTime().isBefore(endLunchTime)
              && pairStamping.first.date.toLocalTime().isAfter(startLunchTime)) {
            //log.debug("vedo rientro da pranzo prima");

            newLimit = (endLunchTime.minusMinutes(shiftType.entranceTolerance)
                .isAfter(pairStamping.first.date.toLocalTime())) ? endLunchTime
                .minusMinutes(shiftType.entranceTolerance) : pairStamping.first.date.toLocalTime();

            diffEndLunchTime = DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
            //log.debug("diffEndLunchTime=getDifferenceBetweenLocalTime({}, {})={}", 
            //    newLimit, endLunchTime, diffEndLunchTime);
          } else if (pairStamping.first.date.toLocalTime().isBefore(endShift)
              && pairStamping.first.date.toLocalTime().isAfter(endLunchTime)) {
            // è rientrato dopo
            //log.debug("vedo rientro da pranzo dopo");

            if (pairStamping.first.date.toLocalTime()
                .isAfter(endLunchTime.plusMinutes(shiftType.entranceTolerance))) {
              newLimit = endLunchTime.plusMinutes(shiftType.entranceTolerance);
              inTolleranceLimit = false;
            } else {
              newLimit = pairStamping.first.date.toLocalTime();
            }

            diffEndLunchTime = DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
            //log.debug("diffEndLunchTime=getDifferenceBetweenLocalTime({}, {})={}",
            //    endLunchTime, newLimit, diffEndLunchTime);
          }

          // FIXME: anche in questo caso occorre verificare quale sia la tolleranza applicata e, 
          // se si tratta di tolleranza in uscita, conteggiarla nei calcoli che vengono effettuati.
          // se il turnista è uscito prima del turno
          if (pairStamping.second.date.toLocalTime().isBefore(endShift)) {
            //log.debug("vedo uscita prima della fine turno")
            //TODO: verificare che qui l'exitTolerance sia valorizzato e, nel caso, controllare
            // quello altrimenti non va fatto il controllo di tolleranza sull'uscita
            if (endShift.minusMinutes(shiftType.exitTolerance)
                .isAfter(pairStamping.second.date.toLocalTime())) {
              newLimit = endShift.minusMinutes(shiftType.exitTolerance);
              inTolleranceLimit = false;
            } else {
              newLimit = pairStamping.second.date.toLocalTime();
            }
          } else {
            //log.debug("vedo uscita dopo la fine turno");
            // il turnista è uscito dopo la fine del turno
            newLimit = (pairStamping.second.date.toLocalTime()
                .isAfter(endShift.plusMinutes(shiftType.exitTolerance))) ? endShift
                .plusMinutes(shiftType.exitTolerance) : pairStamping.second.date.toLocalTime();
          }
          diffEndShift = DateUtility.getDifferenceBetweenLocalTime(endShift, newLimit);
          //log.debug("diffEndShift={}", diffEndShift);
        }

        // write the pair stamping
        stampings = stampings.concat(pairStamping.first.date.toString("HH:mm")).concat("-")
            .concat(pairStamping.second.date.toString("HH:mm")).concat("  ");
      }

      stampings.concat("<br />");

      // controllo eventuali compensazioni di minuti in  ingresso e uscita
      //--------------------------------------------------------------------

      // controlla pausa pranzo:

      // - se è andato a pranzo prima
      if (diffStartLunchTime < 0) {
        //log.debug("sono entrata in pausa pranzo prima! diffStartLunchTime={}", 
        //    diffStartLunchTime);

        // ed è anche rientrato prima dalla PP => compensa
        if (diffEndLunchTime > 0) {
          restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));

          if ((diffStartLunchTime + diffEndLunchTime) > 0) {
            diffStartLunchTime = 0;
            diffEndLunchTime = diffStartLunchTime + diffEndLunchTime;
          } else {
            diffStartLunchTime = diffStartLunchTime + diffEndLunchTime;
            diffEndLunchTime = 0;
          }
        }

        // se necessario e se è entrato prima, compensa con l'ingresso
        if ((diffStartLunchTime < 0) && (diffStartShift > 0)) {
          log.debug("E entrato anche prima! diffStartShift={}", diffStartShift);

          // cerca di compensare con l'ingresso
          restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffStartShift));
          log.debug("restoredMin={} Math.abs(diffStartLunchTime)={} Math.abs(diffStartShift)={}",
              restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffStartShift));

          if ((diffStartLunchTime + diffStartShift) > 0) {
            diffStartLunchTime = 0;
            diffStartShift = diffStartLunchTime + diffStartShift;
          } else {
            diffStartLunchTime = diffStartLunchTime + diffStartShift;
            diffStartShift = 0;
          }

        }
      }

      // - se è entrato dopo la fine della pausa pranzo
      if (diffEndLunchTime < 0) {
        log.debug("E entrato in ritardo dalla pausa pranzo! diffEndLunchTime={}", diffEndLunchTime);

        // controlla che sia entrata dopo in pausa pranzo
        if (diffStartLunchTime > 0) {
          log.debug("e andata anche dopo in pausa pranzo! diffStartLunchTime={}",
              diffStartLunchTime);

          restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));
          log.debug("restoredMin={} Math.abs(diffStartLunchTime)={} Math.abs(diffEndLunchTime)={}",
              restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));

          if ((diffEndLunchTime + diffStartLunchTime) > 0) {
            diffEndLunchTime = 0;
            diffStartLunchTime = diffEndLunchTime + diffStartLunchTime;
          } else {
            diffEndLunchTime = diffEndLunchTime + diffStartLunchTime;
            diffStartLunchTime = 0;
          }

        }

        // se necessario e se è uscito dopo, compensa con l'uscita
        if ((diffEndLunchTime < 0) && (diffEndShift > 0)) {
          //log.debug("e' uscito dopo! diffEndShift={}", diffEndShift);
          // cerca di conpensare con l'uscita (è uscito anche dopo)
          restoredMin += Math.min(Math.abs(diffEndLunchTime), Math.abs(diffEndShift));

          if ((diffEndLunchTime + diffEndShift) > 0) {
            diffEndLunchTime = 0;
            diffEndShift = diffEndLunchTime + diffEndShift;
          } else {
            diffEndLunchTime = diffEndLunchTime + diffEndShift;
            diffEndShift = 0;
          }
        }
      }

      // controlla eventuali compensazioni di ingresso e uscita
      // controlla se è uscito dopo
      if ((diffStartShift < 0) && (diffEndShift > 0)) {

        restoredMin += Math.min(Math.abs(diffEndShift), Math.abs(diffStartShift));

        log.debug("restoredMin={} Math.abs(diffEndShift)={} Math.abs(diffStartShift)={}",
            restoredMin, Math.abs(diffEndShift), Math.abs(diffStartShift));
        if ((diffEndShift + diffStartShift) > 0) {
          diffStartShift = 0;
          diffEndShift = diffEndShift + diffStartShift;
        } else {
          diffStartShift = diffEndShift + diffStartShift;
          diffEndShift = 0;
        }

      } else if ((diffEndShift < 0) && (diffStartShift > 0)) {

        restoredMin += Math.min(Math.abs(diffEndShift), Math.abs(diffStartShift));

        //log.debug("restoredMin={} Math.abs(diffEndShift)={} " 
        // + "Math.abs(diffStartShift)={}",
        //    restoredMin, Math.abs(diffEndShift), Math.abs(diffStartShift));
        if ((diffEndShift + diffStartShift) > 0) {
          diffEndShift = 0;
          diffStartShift = diffEndShift + diffStartShift;
        } else {
          diffEndShift = diffEndShift + diffStartShift;
          diffStartShift = 0;
        }
      }
    }

    // calcola il numero di minuti che teoricamente avrebbe dovuto lavorare nel turno
    int teoreticShiftMinutes =
        DateUtility.getDifferenceBetweenLocalTime(startShift, startLunchTime) + DateUtility
            .getDifferenceBetweenLocalTime(endLunchTime, endShift);

    // calcola i minuti mancanti per completare l'otraio del turno
    int lackOfMinutes = teoreticShiftMinutes - workingMinutes;

    lackOfMinutes -= restoredMin;
    workingMinutes += restoredMin;

    wp.lackOfTime = lackOfMinutes;
    wp.workedTime = workingMinutes;
    return wp;
  }


  /**
   * La lista dei periodi di turno lavorati.
   * @param personShiftDays lista dei giorni di turno di un certo tipo.
   * @return la lista dei periodi di turno lavorati.
   */
  public List<ShiftPeriod> getPersonShiftPeriods(List<PersonShiftDay> personShiftDays) {

    List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
    ShiftPeriod shiftPeriod = null;

    for (PersonShiftDay psd : personShiftDays) {

      LocalTime startShift =
          (psd.shiftSlot.equals(ShiftSlot.MORNING))
              ? psd.shiftType.shiftTimeTable.startMorning
              : psd.shiftType.shiftTimeTable.startAfternoon;
      LocalTime endShift =
          (psd.shiftSlot.equals(ShiftSlot.MORNING))
              ? psd.shiftType.shiftTimeTable.endMorning
              : psd.shiftType.shiftTimeTable.endAfternoon;

      if (shiftPeriod == null
          || !shiftPeriod.person.equals(psd.personShift.person)
          || !shiftPeriod.end.plusDays(1).equals(psd.date)
          || !shiftPeriod.startSlot.equals(startShift)) {
        shiftPeriod =
            new ShiftPeriod(
                psd.personShift.person, psd.date, psd.date, psd.shiftType,
                false, psd.shiftSlot, startShift, endShift);
        shiftPeriods.add(shiftPeriod);
        log.debug("\nCreato nuovo shiftPeriod, person={}, start={}, end={}, type={}, fascia={}, "
                + "orario={} - {}",
            shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type,
            shiftPeriod.shiftSlot, startShift.toString("HH:mm"), endShift.toString("HH:mm"));
      } else {
        shiftPeriod.end = psd.date;
        log.debug("Aggiornato ShiftPeriod, person={}, start={}, end={}, type={}, fascia={}, "
                + "orario={} - {}",
            shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type,
            shiftPeriod.shiftSlot, startShift.toString("HH:mm"), endShift.toString("HH:mm"));
      }
    }

    return shiftPeriods;
  }


  /**
   * La lista dei periodi di turno lavorati.
   * @param personShiftCancelled lista dei turni cancellati.
   * @return la lista dei periodi di turno lavorati.
   */
  public List<ShiftPeriod> getDeletedShiftPeriods(List<ShiftCancelled> personShiftCancelled) {

    List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
    ShiftPeriod shiftPeriod = null;

    LocalTime ttStart = new LocalTime(7, 0, 0);
    LocalTime ttEnd = new LocalTime(12, 0, 0);

    for (ShiftCancelled sc : personShiftCancelled) {
      if (shiftPeriod == null || !shiftPeriod.end.plusDays(1).equals(sc.date)) {
        shiftPeriod = new ShiftPeriod(sc.date, sc.date, sc.type, true, ttStart, ttEnd);
        shiftPeriods.add(shiftPeriod);
        log.trace("Creato nuovo shiftPeriod di cancellati, start={}, end={}, type={}",
            shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type);
      } else {
        shiftPeriod.end = sc.date;
        log.trace("Aggiornato ShiftPeriod di cancellati, start={}, end={}, type={}\n",
            shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type);
      }
    }

    return shiftPeriods;
  }

  /**
   * Salva nel database i giorni di turno lavorati e cancellati contenuti nella lista di periodi
   * di turno passati come parametro.
   *
   * @param shiftType - tipo dei turni che compongono  periodi d turno passati come arametro
   * @param year anno nel quale sono stati lavorati i turni
   * @param month mese nel quale sono stati lavorati i turni
   * @param shiftPeriods - lista di periodi di turno lavorati e cancellati
   */
  public List<String> savePersonShiftDaysFromShiftPeriods(
      ShiftType shiftType, Integer year, Integer month, ShiftPeriods shiftPeriods) {

    List<String> returnList = Lists.newArrayList();
    //Il mese e l'anno ci servono per "azzerare" eventuale giorni di turno rimasti vuoti
    LocalDate monthToManage = new LocalDate(year, month, 1);

    //Conterrà i giorni del mese che devono essere attribuiti a qualche turnista
    Set<Integer> daysOfMonthToAssign = new HashSet<Integer>();
    Set<Integer> daysOfMonthForCancelled = new HashSet<Integer>();

    for (int i = 1; i <= monthToManage.dayOfMonth().withMaximumValue().getDayOfMonth(); i++) {
      daysOfMonthToAssign.add(i);
      daysOfMonthForCancelled.add(i);
    }
    log.trace("Lista dei giorni del mese = {}", daysOfMonthToAssign);

    LocalDate day = null;
    for (ShiftPeriod shiftPeriod : shiftPeriods.periods) {

      // start and end date validation
      if (shiftPeriod.start.isAfter(shiftPeriod.end)) {
        throw new IllegalArgumentException(
            String.format(
                "ShiftPeriod person.id = %s has start date %s after end date %s",
                shiftPeriod.person.id, shiftPeriod.start, shiftPeriod.end));
      }

      day = shiftPeriod.start;
      while (day.isBefore(shiftPeriod.end.plusDays(1))) {
        // normal shift
        if (!shiftPeriod.cancelled) {
          //La persona deve essere tra i turnisti
          log.debug("---Prende il personShift di {}", shiftPeriod.person);
          PersonShift personShift = 
              personShiftDayDao.getPersonShiftByPerson(shiftPeriod.person, day);
          if (personShift == null) {
            throw new IllegalArgumentException(
                String.format("Person %s is not a shift person", shiftPeriod.person));
          }

          // Se la persona è assente in questo giorno non può essere in turno (almeno che non
          // sia cancellato)
          if (absenceDao.getAbsencesInPeriod(
              Optional.fromNullable(shiftPeriod.person), day,
              Optional.<LocalDate>absent(), false).size()
              > 0) {
            String msg =
                String.format("Assenza incompatibile di %s %s per il giorno %s",
                    shiftPeriod.person.name, shiftPeriod.person.surname, day);
            returnList.add(msg);
            return returnList;
            //BadRequest.badRequest(msg);
          }

          // Salvataggio del giorno di turno
          // Se c'è un turno già presente viene sostituito, altrimenti viene creato un
          // PersonShiftDay nuovo
          log.debug("Cerco turno shiftType = {} AND date = {} AND shiftSlot = {}",
              shiftType.description, day, shiftPeriod.shiftSlot);

          PersonShiftDay personShiftDay =
              personShiftDayDao
                  .getPersonShiftDayByTypeDateAndSlot(shiftType, day, shiftPeriod.shiftSlot);
          if (personShiftDay == null) {
            personShiftDay = new PersonShiftDay();
            log.debug("Creo un nuovo personShiftDay per person = {}, day = {}, shiftType = {}",
                shiftPeriod.person.name, day, shiftType.description);
          } else {
            log.debug("Aggiorno il personShiftDay = {} di {}",
                personShiftDay, personShiftDay.personShift.person.name);
          }
          personShiftDay.date = day;
          personShiftDay.shiftType = shiftType;
          personShiftDay.shiftSlot = shiftPeriod.shiftSlot;
          personShiftDay.personShift = personShift;

          personShiftDay.save();
          log.info("Aggiornato PersonShiftDay = {} con {}\n",
              personShiftDay, personShiftDay.personShift.person);

          //Questo giorno è stato assegnato
          daysOfMonthToAssign.remove(day.getDayOfMonth());

        } else {
          // cancelled shift
          // Se non c'è già il turno cancellato lo creo
          log.debug("Cerco turno cancellato shiftType = {} AND date = {}",
              shiftType.type, day);
          ShiftCancelled shiftCancelled = shiftDao.getShiftCancelled(day, shiftType);
          log.debug("shiftCancelled = {}", shiftCancelled);

          if (shiftCancelled == null) {
            shiftCancelled = new ShiftCancelled();
            shiftCancelled.date = day;
            shiftCancelled.type = shiftType;

            log.debug("Creo un nuovo ShiftCancelled={} per day = {}, shiftType = {}",
                shiftCancelled, day, shiftType.description);

            shiftCancelled.save();
            log.debug("Creato un nuovo ShiftCancelled per day = {}, shiftType = {}",
                day, shiftType.description);
          }

          //Questo giorno è stato annullato
          daysOfMonthForCancelled.remove(day.getDayOfMonth());
        }

        day = day.plusDays(1);
      }
    }

    log.info("Turni da rimuovere = {}", daysOfMonthToAssign);

    for (int dayToRemove : daysOfMonthToAssign) {
      LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
      log.trace("Eseguo la cancellazione del giorno {}", dateToRemove);

      val personShiftDayToDelete = personShiftDayDao.byTypeAndDate(shiftType, dateToRemove);
      if (personShiftDayToDelete.isPresent()) {
        personShiftDayToDelete.get().delete();
        log.info("Rimosso turno di tipo {} del giorno {}", shiftType.description, dateToRemove);
      }
    }

    log.info("Turni cancellati da rimuovere = {}", daysOfMonthForCancelled);

    for (int dayToRemove : daysOfMonthForCancelled) {
      LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
      log.trace("Eseguo la cancellazione del giorno {}", dateToRemove);

      long cancelled = shiftDao.deleteShiftCancelled(shiftType, dateToRemove);

      if (cancelled == 1) {
        log.info("Rimosso turno cancellato di tipo {} del giorno {}",
            shiftType.description, dateToRemove);
      }
    }
    returnList.add("ok");
    return returnList;
  }

  /**
   * Costruisce la lista dei periodi di assenza in turno da una lista di giorni di assenza.
   *
   * @param absencePersonShiftDays lista dei giorni di assenza
   * @param shiftType tipo del turno
   * @return absenceShiftPeriods lista di periodi di assenza in turno
   */
  public List<AbsenceShiftPeriod> getAbsentShiftPeriodsFromAbsentShiftDays(
      List<Absence> absencePersonShiftDays, ShiftType shiftType) {

    // List of absence periods
    List<AbsenceShiftPeriod> absenceShiftPeriods = new ArrayList<AbsenceShiftPeriod>();

    AbsenceShiftPeriod absenceShiftPeriod = null;
    for (Absence abs : absencePersonShiftDays) {
      // L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di
      // reperibilità non consecutivi.
      if (absenceShiftPeriod == null
          || !absenceShiftPeriod.person.equals(abs.personDay.person)
          || !absenceShiftPeriod.end.plusDays(1).equals(abs.personDay.date)) {
        absenceShiftPeriod =
            new AbsenceShiftPeriod(
                abs.personDay.person, abs.personDay.date, abs.personDay.date,
                ShiftType.<ShiftType>findById(shiftType.id));
        absenceShiftPeriods.add(absenceShiftPeriod);
        log.trace("Creato nuovo absenceShiftPeriod, person={}, start={}, end={}",
            absenceShiftPeriod.person, absenceShiftPeriod.start, absenceShiftPeriod.end);
      } else {
        absenceShiftPeriod.end = abs.personDay.date;
        log.trace("Aggiornato absenceShiftPeriod, person={}, start={}, end={}",
            absenceShiftPeriod.person, absenceShiftPeriod.start, absenceShiftPeriod.end);
      }
    }

    return absenceShiftPeriods;
  }

  /**
   * Salva le ore di turno da retribuire di un certo mese nelle competenze.
   * Per ogni persona riceve i giorni di turno effettuati nel mese e le eventuali ore non lavorate.
   * Calcola le ore da retribuire sulla base dei giorni di turno sottraendo le eventuali ore non
   * lavorate e aggiungendo i minuti eventualemnte avanzati nel mese precedente. Le ore retribuite
   * sono la parte intera delle ore calcolate.
   * I minuti eccedenti sono memorizzati nella competenza per i mesi successivi.
   *
   * @param personsShiftHours contiene per ogni persona il numero dei giorni in turno lavorati
   *        (thDays) e gli eventuali minuti non lavorati (thLackTime)
   * @param year anno di riferimento dei turni
   * @param month mese di riferimento dei turni
   * @return la lista delle competenze corrispondenti ai turni lavorati
   * @author arianna
   */
  public List<Competence> updateDbShiftCompetences(
      Table<Person, String, Integer> personsShiftHours, int year, int month) {

    List<Competence> savedCompetences = new ArrayList<Competence>();
    int[] apprHoursAndExcMins;

    String thDays = Messages.get("PDFReport.thDays");
    String thLackTime = Messages.get("PDFReport.thLackTime");

    // get the Competence code for the ordinary shift
    CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode(codShift);

    // for each person
    for (Person person : personsShiftHours.rowKeySet()) {

      log.debug("Registro dati di {} {}", person.surname, person.name);

      BigDecimal sessanta = new BigDecimal("60");

      log.debug("Calcolo le ore di turno teoriche dai giorni = {}",
          personsShiftHours.get(person, thDays));
      BigDecimal numOfHours =
          competenceUtility.calcShiftHoursFromDays(personsShiftHours.get(person, thDays));

      // compute the worked time in minutes of the present month
      int workedMins = (personsShiftHours.contains(person, thLackTime))
          ? numOfHours.multiply(sessanta).subtract(
          new BigDecimal(personsShiftHours.get(person, thLackTime))).intValue()
          : numOfHours.multiply(sessanta).intValue();

      log.debug("Minuti lavorati = thReqHour * 60 - thLackTime = {} * 60 - {}",
          numOfHours, personsShiftHours.get(person, thLackTime));

      // compute the hours appproved and the exceede minutes on the basis of
      // the current worked minutes and the exceeded mins of the previous month
      apprHoursAndExcMins = calcShiftValueApproved(person, year, month, workedMins);

      // compute the value requested
      BigDecimal reqHours = competenceUtility.calcDecimalShiftHoursFromMinutes(workedMins);

      // save the FS reperibility competences in the DB
      Optional<Competence> shiftCompetence =
          competenceDao.getCompetence(person, year, month, competenceCode);

      // update the requested hours
      if (shiftCompetence.isPresent()) {

        // check if the competence has been processed to be sent to Rome
        // and and this case we don't change the valueApproved
        CertificatedData certData = personMonthRecapDao
            .getPersonCertificatedData(person, month, year);

        int apprHours = (certData != null && certData.isOk && (certData.competencesSent != null))
            ? shiftCompetence.get().valueApproved : apprHoursAndExcMins[0];
        int exceededMins =
            (certData != null && certData.isOk && (certData.competencesSent != null))
                ? shiftCompetence.get().exceededMins
                : apprHoursAndExcMins[1];

        shiftCompetence.get().valueApproved = apprHours;
        shiftCompetence.get().valueRequested = reqHours;
        shiftCompetence.get().exceededMins = exceededMins;
        shiftCompetence.get().save();

        log.debug("Aggiornata competenza di {} {}: valueRequested={}, valueApproved={}, "
                + "exceddMins={}", shiftCompetence.get().person.surname,
            shiftCompetence.get().person.name, shiftCompetence.get().valueRequested,
            shiftCompetence.get().valueApproved, shiftCompetence.get().exceededMins);

        savedCompetences.add(shiftCompetence.get());
      } else {
        // insert a new competence with the requested hours an reason
        Competence competence = new Competence(person, competenceCode, year, month);
        competence.valueApproved = apprHoursAndExcMins[0];
        competence.exceededMins = apprHoursAndExcMins[1];
        competence.valueRequested = reqHours;
        competence.save();

        savedCompetences.add(competence);

        log.debug("Salvata competenza {}", shiftCompetence);
      }
    }

    // return the number of saved competences
    return savedCompetences;
  }

  /**
   * Aggiorna la tabella totalPersonShiftSumDays per contenere, per ogni persona nella lista dei
   * turni personShiftDays, e per ogni tipo di turno trovato, il numero di giorni di turno
   * effettuati.
   *
   * @param personShiftDays lista di shiftDays.
   * @param personShiftSumDaysForTypes tabella contenente il numero di giorni di turno effettuati
   *        per ogni persona e tipologia di turno. Questa tabella viene aggiornata contando i 
   *        giorni di turno contenuti nella lista personShiftDays passata come parametro.
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
          shiftNum =
              (personShiftSumDaysForTypes.contains(person, shift))
                  ? personShiftSumDaysForTypes.get(person, shift) : 0;
          shiftNum++;
          personShiftSumDaysForTypes.put(person, shift, shiftNum);
        }
      }

    }
  }


  /**
   * Crea la tabella contenente le informazioni da stampare sul report dei turni mensile.
   * Per ogni persona contiene:
   * - thDays - num di giorni di turno lavorati
   * - thLackTime - numero di ore non lavorati in turno
   * - thReqHour - ore di turno spettanti
   * - thAppHour - ore di turno richieste
   * - thExceededMin - minuti in eccesso da accumulare nel mese successivo
   *
   * @param totalPersonShiftWorkedTime contiene per ogni persona, il numero di giorni lavorati e i
   *        minuti non lavorati in turno
   * @param competenceList lista di competenze relative ai turni di lavoro di un mese
   * @return totalShiftInfotabella testuale contenete tutte le informazioni
   */
  public Table<Person, String, String> getPersonsReportShiftInfo(
      Table<Person, String, Integer> totalPersonShiftWorkedTime, List<Competence> competenceList) {

    Comparator<String> nullSafeStringComparator = Comparator
        .nullsFirst(String::compareToIgnoreCase);

    Table<Person, String, String> totalShiftInfo =
        TreeBasedTable.<Person, String, String>create(
            Person.personComparator(), nullSafeStringComparator);

    for (Competence competence : competenceList) {

      // Prende i giorni di turno lavorati e le eventuali ora mancanti
      int numOfDays =
          totalPersonShiftWorkedTime.get(competence.person, Messages.get("PDFReport.thDays"));
      int lackOfMin =
          (totalPersonShiftWorkedTime.contains(
              competence.person, Messages.get("PDFReport.thLackTime")))

              ? totalPersonShiftWorkedTime
              .get(competence.person, Messages.get("PDFReport.thLackTime")) : 0;

      // prende le ore richieste, quelle approvate e i minuti in eccesso
      // che dovranno far parte del calcolo delle ore del mese successivo
      BigDecimal reqHours = competence.valueRequested;
      int numOfApprovedHours = competence.valueApproved;
      int exceededMins = competence.exceededMins;

      log.debug("In totalShiftInfo memorizzo (person {}) giorni={}, ore richieste={}, "
              + "ore approvate={}, min accumulati={}", competence.person, numOfDays,
          reqHours, numOfApprovedHours, exceededMins);
      totalShiftInfo.put(
          competence.person, Messages.get("PDFReport.thDays"), Integer.toString(numOfDays));
      totalShiftInfo.put(
          competence.person, Messages.get("PDFReport.thLackTime"),
          competenceUtility.calcStringShiftHoursFromMinutes(lackOfMin));

      totalShiftInfo.put(
          competence.person, Messages.get("PDFReport.thReqHour"), reqHours.toString());
      totalShiftInfo.put(
          competence.person, Messages.get("PDFReport.thAppHour"),
          Integer.toString(numOfApprovedHours));
      totalShiftInfo.put(
          competence.person, Messages.get("PDFReport.thExceededMin"),
          Integer.toString(exceededMins));

    }

    return totalShiftInfo;
  }

  /**
   * Per ogni persona e per ogni turno, calcola il nuumerio di giorni di turno lavorati
   * e gli eventuali minuti non lavorati che non dovranno essere retribuite.
   *
   * @param personsShiftsWorkedDays contiene, per ogni persona e per ogni turno, il nuomero dei
   *        giorni di turno lavorati
   * @param totalInconsistentAbsences contiene, per ogni persona, le eventuali inconsistenze ed in
   *        particolare la lista dei minuti non lavorati nella colonna thLackTime
   * @return totalPersonShiftWorkedTime contiene per ogni persona il numero totale di giorni di
   *        turno lavorati (col thDays) e il numero totale di minuti non lavorati che non devono 
   *        essere retribuiti (col thLackTime).
   * @author arianna
   */
  public Table<Person, String, Integer> calcShiftWorkedDaysAndLackTime(
      Table<Person, String, Integer> personsShiftsWorkedDays,
      Table<Person, String, List<String>> totalInconsistentAbsences) {

    // Contains the number of the effective hours of worked shifts
    Table<Person, String, Integer> totalPersonShiftWorkedTime =
        TreeBasedTable.<Person, String, Integer>create(Person.personComparator(), Comparator
            .nullsFirst(String::compareToIgnoreCase));

    String thLackTime = Messages.get("PDFReport.thLackTime");

    // Subcract the lack of time from the Requested Hours
    for (Person person : personsShiftsWorkedDays.rowKeySet()) {
      log.debug("Calcolo per person={} il thLackTime", person);

      int totalShiftDays = 0;
      int lackMin = 0;

      // Sum the shift worked days for each  shift type
      for (String shiftType : personsShiftsWorkedDays.columnKeySet()) {
        totalShiftDays +=
            (personsShiftsWorkedDays.contains(person, shiftType))
                ? personsShiftsWorkedDays.get(person, shiftType) : 0;
      }

      totalPersonShiftWorkedTime.put(person, Messages.get("PDFReport.thDays"), totalShiftDays);

      log.debug("Somma i minuti mancanti prendendoli da totalInconsistentAbsences({}, {})",
          person.surname, thLackTime);

      // check for lack of worked time and summarize the minutes
      if (totalInconsistentAbsences.contains(person, thLackTime)) {
        log.debug("thLackTime non è vuoto");
        String[] timeStr;
        for (String time : totalInconsistentAbsences.get(person, thLackTime)) {

          timeStr = time.split(".");
          log.debug("time = '{}' valori di timeStr = {}", time, timeStr.length);

          lackMin += Integer.parseInt(time);
        }
      }

      log.debug("memorizza in totalPersonsShiftsWorkedTimes({}, thLackTime) {}",
          person, lackMin);
      totalPersonShiftWorkedTime.put(person, thLackTime, lackMin);
    }

    return totalPersonShiftWorkedTime;
  }

  /**
   * Costruisce na calendario di un turno in un certo mese in una tabella del tipo
   * (tipoTurno, giorno_del_mese) -> SD(Person, Person) dove (Person, Person) indica la persona
   * in turno di mattina e di pomeriggio.
   *
   * @param firstOfMonth primo giorno del mese
   * @param shiftType tipo del turno
   * @param shiftCalendar tabella [Turno, Giorno, SD] che viene modificata inserendo per giorno del
   *        mese la persona in turno di mattina e di pomeriggio per il turno shiftType
   */
  public void buildMonthlyShiftCalendar(
      LocalDate firstOfMonth, ShiftType shiftType, Table<String, Integer, Sd> shiftCalendar) {

    // legge i giorni di turno del tipo 'type' da inizio a fine mese
    List<PersonShiftDay> personShiftDays =
        personShiftDayDao.byTypeInPeriod(firstOfMonth, firstOfMonth.dayOfMonth()
            .withMaximumValue(), shiftType, Optional.absent());

    // li inserisce nel calendario
    for (PersonShiftDay personShiftDay : personShiftDays) {
      Person person = personShiftDay.personShift.person;

      Sd shift = null;

      int day = personShiftDay.date.getDayOfMonth();
      String currShift = personShiftDay.shiftType.type;

      if (!shiftCalendar.contains(currShift, day)) {
        shift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING))
            ? new Sd(person, null) : new Sd(null, person);
        shiftCalendar.put(currShift, day, shift);
      } else {
        shift = shiftCalendar.get(currShift, day);
        if (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) {
          log.debug("Completo turno di {} con la mattina di {}", day, person);
          shift.mattina = person;
        } else {
          log.debug("Completo turno di {} con il pomeriggio di {}", day, person);
          shift.pomeriggio = person;
        }

        shiftCalendar.put(currShift, day, shift);
      }
    }

    //legge i turni cancellati e li registra nella tabella mensile
    log.debug("Cerco i turni cancellati di tipo '{}' e li inserisco nella tabella mensile",
        shiftType);
    List<ShiftCancelled> shiftsCancelled =
        shiftDao.getShiftCancelledByPeriodAndType(firstOfMonth, firstOfMonth.dayOfMonth()
            .withMaximumValue(), shiftType);

    Sd shift = new Sd(null, null);
    for (ShiftCancelled sc : shiftsCancelled) {
      shiftCalendar.put(shiftType.type, sc.date.getDayOfMonth(), shift);
    }
  }

  /**
   * Calcola le ore di turno da approvare date quelle richieste.
   * Poiché le ore approvate devono essere un numero intero e quelle calcolate direttamente dai
   * giorni di turno possono essere decimali, le ore approvate devono essere arrotondate per
   * eccesso o per difetto a seconda dell'ultimo arrotondamento effettuato in modo che questi
   * vengano alternati.
   *
   * @author arianna
   */
  public int[] calcShiftValueApproved(Person person, int year, int month, int requestedMins) {
    int hoursApproved = 0;
    int exceedMins = 0;

    log.debug("Nella calcShiftValueApproved person ={}, year={}, month={}, requestedMins={})",
        person, year, month, requestedMins);

    String workedTime = competenceUtility.calcStringShiftHoursFromMinutes(requestedMins);
    int hoursOfWorkedTime = Integer.parseInt(workedTime.split("\\.")[0]);
    int minsOfWorkedTime = Integer.parseInt(workedTime.split("\\.")[1]);

    log.debug("hoursOfWorkedTime = {} minsOfWorkedTime = {}", hoursOfWorkedTime, minsOfWorkedTime);

    // get the Competence code for the ordinary shift
    CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode(codShift);

    log.debug("month={}", month);

    Competence myCompetence =
        competenceDao.getLastPersonCompetenceInYear(person, year, month, competenceCode);

    int oldExceedMins = 0;

    // get the old exceede mins in the DB
    oldExceedMins =
        ((myCompetence == null)
            || ((myCompetence != null) && myCompetence.exceededMins == null))
            ? 0 : myCompetence.exceededMins;

    log.debug("oldExceedMins in the DB={}", oldExceedMins);

    // if there are no exceeded mins, the approved hours
    // match with the worked hours
    if (minsOfWorkedTime == 0) {
      hoursApproved = hoursOfWorkedTime;
      exceedMins = oldExceedMins;

    } else {
      // check if the exceeded mins of this month plus those
      // worked in the previous months make up an hour
      exceedMins = oldExceedMins + minsOfWorkedTime;
      if (exceedMins >= 60) {
        hoursApproved = hoursOfWorkedTime + 1;
        exceedMins -= 60;
      } else {
        hoursApproved = hoursOfWorkedTime;
      }

    }

    log.debug("hoursApproved={} exceedMins={}", hoursApproved, exceedMins);

    int[] result = {hoursApproved, exceedMins};

    log.debug("La calcShiftValueApproved restituisce {}", result);

    return result;
  }

  /**
   * Crea il calendario con le reperibilita' di un determinato tipo in un dato anno completo o
   * relativo ad una sola persona.
   *
   * @param year anno di riferimento del calendario
   * @param type tipo di turni da caricare
   * @param personShift opzionale, contiene la persona della quale caricare i turni, se è vuota
   *        carica tutto il turno
   * @return icsCalendar calendario
   * @author arianna
   */
  public Calendar createicsShiftCalendar(
      int year, String type, Optional<PersonShift> personShift) {
    List<PersonShiftDay> personShiftDays = new ArrayList<PersonShiftDay>();

    log.debug("nella createicsReperibilityCalendar(int {}, String {}, List<PersonShift> {})",
        year, type, personShift);
    ShiftCategories shiftCategory = shiftDao.getShiftCategoryByType(type);
    String eventLabel = "Turno ".concat(shiftCategory.description).concat(": ");

    // Create a calendar
    //---------------------------
    Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
    icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
    icsCalendar.getProperties().add(CalScale.GREGORIAN);
    icsCalendar.getProperties().add(Version.VERSION_2_0);

    // read the person(0) shift days for the year
    //-------------------------------------------------
    LocalDate from = new LocalDate(year, 1, 1);
    LocalDate to = new LocalDate(year, 12, 31);

    ShiftType shiftType = shiftDao.getShiftTypeByType(type);

    // get the working shift days
    //------------------------------

    // if the list is empty, load the entire shift days
    if (!personShift.isPresent()) {
      personShiftDays = shiftDao.getShiftDaysByPeriodAndType(from, to, shiftType);
      log.debug("Shift find called from {} to {}, type {} - found {} shift days",
          from, to, type, personShiftDays.size());
    } else {
      // load the shift days of the person in the list
      personShiftDays =
          shiftDao.getPersonShiftDaysByPeriodAndType(from, to, shiftType, personShift.get().person);
      log.debug("Shift find called from {} to {}, type {} person {} - found {} shift days",
          from, to, type, personShift.get().person.surname, personShiftDays.size());
    }

    // load the shift days in the calendar
    for (PersonShiftDay psd : personShiftDays) {

      LocalTime startShift =
          (psd.shiftSlot.equals(ShiftSlot.MORNING))
              ? psd.shiftType.shiftTimeTable.startMorning
              : psd.shiftType.shiftTimeTable.startAfternoon;
      LocalTime endShift = (psd.shiftSlot.equals(ShiftSlot.MORNING))
          ? psd.shiftType.shiftTimeTable.endMorning
          : psd.shiftType.shiftTimeTable.endAfternoon;

      log.debug("Turno di {} del {} dalle {} alle {}",
          psd.personShift.person.surname, psd.date, startShift, endShift);

      //set the start event
      java.util.Calendar start = java.util.Calendar.getInstance();
      start.set(
          psd.date.getYear(), psd.date.getMonthOfYear() - 1, psd.date.getDayOfMonth(),
          startShift.getHourOfDay(), startShift.getMinuteOfHour());

      //set the end event
      java.util.Calendar end = java.util.Calendar.getInstance();
      end.set(
          psd.date.getYear(), psd.date.getMonthOfYear() - 1, psd.date.getDayOfMonth(),
          endShift.getHourOfDay(), endShift.getMinuteOfHour());

      String label = eventLabel.concat(psd.personShift.person.surname);

      icsCalendar.getComponents().add(createDurationICalEvent(
          new DateTime(start.getTime()), new DateTime(end.getTime()), label));
      continue;
    }

    // get the deleted shift days
    //------------------------------
    // get the deleted shifts of type shiftType
    List<ShiftCancelled> shiftsCancelled =
        shiftDao.getShiftCancelledByPeriodAndType(from, to, shiftType);
    log.debug("ShiftsCancelled find called from {} to {}, type {} - found {} shift days",
        from, to, shiftType.type, shiftsCancelled.size());

    // load the calcelled shift in the calendar
    for (ShiftCancelled shiftCancelled : shiftsCancelled) {
      log.debug("Trovato turno {} ANNULLATO nel giorno {}",
          shiftCancelled.type.type, shiftCancelled.date);

      // build the event day
      java.util.Calendar shift = java.util.Calendar.getInstance();
      shift.set(
          shiftCancelled.date.getYear(), shiftCancelled.date.getMonthOfYear() - 1,
          shiftCancelled.date.getDayOfMonth());
      String label = eventLabel.concat("Annullato");

      icsCalendar.getComponents().add(createAllDayICalEvent(new Date(shift.getTime()), label));
      continue;
    }

    return icsCalendar;
  }

  /*
   * Create a VEvent width label 'label' that start at 'startDate' end end at 'endDate'
   */
  private VEvent createDurationICalEvent(DateTime startDate, DateTime endDate, String eventLabel) {
    VEvent shiftDay = new VEvent(startDate, endDate, eventLabel);
    shiftDay.getProperties().add(new Uid(UUID.randomUUID().toString()));

    return shiftDay;
  }

  /*
   * Creat an all day VEvent whith label 'label' for the day 'date'
   */
  private VEvent createAllDayICalEvent(Date date, String eventLabel) {
    VEvent shiftDay = new VEvent(date, eventLabel);

    shiftDay.getProperties().add(new Uid(UUID.randomUUID().toString()));

    return shiftDay;
  }

  /**
   * Export the shift calendar in iCal for the person with id = personId with reperibility
   * of type 'type' for the 'year' year.
   * If the personId=0, it exports the calendar for all persons of the shift of type 'type'
   */
  public Optional<Calendar> createCalendar(String type, Optional<Long> personId, int year) {
    log.debug("Sto per creare iCal per l'anno {} della person con id = {}, shift type {}",
        year, personId, type);

    Optional<PersonShift> personShift = Optional.absent();
    if (personId.isPresent()) {
      // read the shift person
      personShift =
          Optional.fromNullable(shiftDao.getPersonShiftByPersonAndType(personId.get(), type));
      if (!personShift.isPresent()) {
        log.info("Person id = {} is not associated to a shift of type = {}", personId.get(), type);
        return Optional.<Calendar>absent();
      }
    }

    Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();

    log.debug("chiama la createicsShiftCalendar({}, {}, {})", year, type, personShift);
    icsCalendar = createicsShiftCalendar(year, type, personShift);

    log.debug("Find {} periodi di turno.", icsCalendar.getComponents().size());
    log.debug("Creato iCal per l'anno {} della person con id = {}, shift type {}",
        year, personId, type);

    return Optional.of(icsCalendar);
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
  // shift day
  public static final class Sd {

    Person mattina;
    Person pomeriggio;

    public Sd(Person mattina, Person pomeriggio) {
      this.mattina = mattina;
      this.pomeriggio = pomeriggio;
    }
  }
}
