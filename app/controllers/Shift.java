package controllers;

import static play.modules.pdf.PDF.renderPDF;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import controllers.Resecure.BasicAuth;

import dao.AbsenceDao;
import dao.PersonDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;

import it.cnr.iit.epas.JsonShiftPeriodsBinder;

import lombok.extern.slf4j.Slf4j;

import manager.ShiftManager;

import models.Competence;
import models.Person;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftCategories;
import models.ShiftTimeTable;
import models.ShiftType;
import models.User;
import models.absences.Absence;
import models.exports.AbsenceShiftPeriod;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.joda.time.LocalDate;

import play.data.binding.As;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.modules.pdf.PDF.Options;
import play.mvc.Controller;
import play.mvc.With;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;


/**
 * Implements work shifts.
 *
 * @author arianna
 * @author dario
 */
@Slf4j
@With(Resecure.class)
public class Shift extends Controller {


  @Inject
  private static ShiftDao shiftDao;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static ShiftManager shiftManager;
  @Inject
  private static PersonShiftDayDao personShiftDayDao;

  @Inject
  private static AbsenceDao absenceDao;


  /**
   * Restituisce la lista delle persone in un determinato turno.
   *
   * @author arianna
   *
   */
  @BasicAuth
  public static void personList(String type) {
    response.accessControl("*");

    log.debug("Cerco persone del turno {}", type);

    ShiftType shiftType = shiftDao.getShiftTypeByType(type);
    notFoundIfNull(shiftType, String.format("ShiftType type = %s doesn't exist", type));

    log.debug("Cerco Turnisti di tipo {}", shiftType.type);

    final List<Person> personList = personDao.getPersonForShift(type);

    log.debug("Shift personList called, found {} shift person", personList.size());

    if (log.isTraceEnabled()) {
      for (Person p : personList) {
        log.trace("name={} surname={} id={} jolly={}",
            p.name, p.surname, p.id, p.personShift.jolly);
      }
    }
    render(personList);
  }


  /**
   * Get shifts from the DB and render to the sistorg portal calendar.
   *
   * @author arianna
   */
  @BasicAuth
  public static void timeTable(String type) {
    response.accessControl("*");

    log.trace("Cercata la time table di un turno");

    // type validation
    ShiftType shiftType = shiftDao.getShiftTypeByType(type);
    notFoundIfNull(shiftType, String.format("ShiftType type = %s doesn't exist", type));

    log.debug("Cerco la time table del turno di tipo {}", shiftType.type);

    ShiftTimeTable shiftTimeTable = shiftType.shiftTimeTable;

    render(shiftTimeTable);

  }

  /**
   * Get shifts from the DB and render to the sistorg portal calendar.
   *
   * @author arianna
   * @author dario
   */
  @BasicAuth
  public static void find(
      Integer yearFrom, Integer monthFrom, Integer dayFrom,
      Integer yearTo, Integer monthTo, Integer dayTo, String type) {

    response.accessControl("*");

    // type validation
    ShiftType shiftType = shiftDao.getShiftTypeByType(type);
    notFoundIfNull(shiftType, String.format("ShiftType type = %s doesn't exist", type));

    log.debug("Cerco turni di tipo {}", shiftType.type);

    LocalDate from = new LocalDate(yearFrom, monthFrom, dayFrom);
    LocalDate to = new LocalDate(yearTo, monthTo, dayTo);

    List<PersonShiftDay> personShiftDays =
        shiftDao.getShiftDaysByPeriodAndType(from, to, shiftType);
    log.debug("Shift find called from {} to {}, type {} - found {} shift days",
        from, to, shiftType.type, personShiftDays.size());

    List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
    List<ShiftPeriod> deletedShiftPeriods = new ArrayList<ShiftPeriod>();

    // get the shift periods
    shiftPeriods = shiftManager.getPersonShiftPeriods(personShiftDays);

    // get the cancelled shifts of type shiftType
    List<ShiftCancelled> shiftCancelled =
        shiftDao.getShiftCancelledByPeriodAndType(from, to, shiftType);
    log.debug("ShiftCancelled find called from {} to {}, type {} - found {} shift days",
        from, to, shiftType.type, shiftCancelled.size());

    // get the period of cancelled shifts
    deletedShiftPeriods = shiftManager.getDeletedShiftPeriods(shiftCancelled);

    // add the deleted period to the worked one
    shiftPeriods.addAll(deletedShiftPeriods);

    log.debug("Find {} shiftPeriods.", shiftPeriods.size());
    render(shiftPeriods);

  }


  /**
   * Update working shifts in the DB that have been red from the sistorg portal calendar.
   *
   * @author arianna
   *
   */
  @BasicAuth
  public static void update(
      String type, Integer year, Integer month,
      @As(binder = JsonShiftPeriodsBinder.class) ShiftPeriods body) {
    log.debug("update: Received shiftPeriods {}", body);

    if (body == null) {
      badRequest();
    }

    // type validation
    ShiftType shiftType = shiftDao.getShiftTypeByType(type);
    if (shiftType == null) {
      throw new IllegalArgumentException(String.format("ShiftType type = %s doesn't exist", type));
    }

    log.debug("shiftType={}", shiftType.description);

    // save the recived shift periods with type shiftType in the month "month" of the "year" year
    shiftManager.savePersonShiftDaysFromShiftPeriods(shiftType, year, month, body);

  }


  /**
   * Crea una tabella con le eventuali inconsistenze tra le timbrature di un
   * turnista e le fasce di orario da rispettare per un determinato turno, in un dato periodo di
   * tempo (Person, [thNoStampings, thBadStampings, thAbsences], List [gg MMM]).
   *
   * @author arianna
   */
  @BasicAuth
  public static void getInconsistencyTimestamps2Timetable(
      ShiftType shiftType, LocalDate startDate, LocalDate endDate) {

    // crea la tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
    Table<Person, String, List<String>> inconsistentAbsence =
        HashBasedTable.<Person, String, List<String>>create();

    // seleziona le persone nel turno 'shiftType' da inizio a fine mese
    List<PersonShiftDay> personShiftDays =
        personShiftDayDao.getPersonShiftDayByTypeAndPeriod(startDate, endDate, shiftType);

    //inconsistentAbsence = CompetenceUtility.getShiftInconsistencyTimestampTable(personShiftDays);
    shiftManager.getShiftInconsistencyTimestampTable(personShiftDays, inconsistentAbsence, shiftType);

    //return inconsistentAbsence;
  }


  /**
   * Crea il file PDF con il resoconto mensile dei turni dello IIT il mese 'month'
   * dell'anno 'year' (portale sistorg).
   *
   * @author arianna
   */
  @BasicAuth
  public static void exportMonthAsPDF() {
    int year = params.get("year", Integer.class);
    int month = params.get("month", Integer.class);
    Long shiftCategoryId = params.get("type", Long.class);

    log.debug("sono nella exportMonthAsPDF con shiftCategory={} year={} e month={}",
        shiftCategoryId, year, month);

    final LocalDate firstOfMonth = new LocalDate(year, month, 1);
    final LocalDate lastOfMonth = firstOfMonth.dayOfMonth().withMaximumValue();

    Comparator<String> nullSafeStringComparator = Comparator
            .nullsFirst(String::compareToIgnoreCase); 
        
    
    //  Used TreeBasedTable becouse of the alphabetical name order (persona, A/B, num. giorni)
    Table<Person, String, Integer> personsShiftsWorkedDays =
        TreeBasedTable.<Person, String, Integer>create(
            Person.personComparator(), nullSafeStringComparator);

    // crea la tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
    // (person, [thAbsences, thNoStampings, thBadStampings], <giorni/fasce orarie inconsistenti>)
    Table<Person, String, List<String>> personsShiftInconsistentAbsences =
        TreeBasedTable.<Person, String, List<String>>create(
            Person.personComparator(), nullSafeStringComparator);
    
    // Contains the number of the effective hours of worked shifts
    Table<Person, String, Integer> totalPersonShiftWorkedTime =
        TreeBasedTable.<Person, String, Integer>create(
            Person.personComparator(), nullSafeStringComparator);

    ShiftCategories shiftCategory = ShiftCategories.findById(shiftCategoryId);
    if (shiftCategory == null) {
      notFound(String.format("shiftCategory shiftCategory = %s doesn't exist", shiftCategory));
    }

    log.debug("shiftCategory = {}", shiftCategory);

    // Legge i turni associati alla categoria (es: A, B)
    List<ShiftType> shiftTypes =
        ShiftType.find("SELECT st FROM ShiftType st WHERE st.shiftCategories = ?", shiftCategory)
          .fetch();

    // for each shift
    for (ShiftType shiftType : shiftTypes) {
      String type = shiftType.type;
      log.debug("ELABORA TURNO TYPE={}", type);

      // seleziona i giorni di turno di tutte le persone associate al turno 'shiftType'
      // da inizio a fine mese
      List<PersonShiftDay> personsShiftDays =
          personShiftDayDao.getPersonShiftDayByTypeAndPeriod(firstOfMonth, lastOfMonth, shiftType);

      log.debug("CALCOLA IL NUM DI GIORNI EFFETTUATI NEL TURNO PER OGNI PERSONA");
      // conta e memorizza i giorni di turno per ogni persona
      shiftManager.countPersonsShiftsDays(personsShiftDays, personsShiftsWorkedDays);
      log.debug("* Num di persone nella personsShiftsWorkedDays = {}", 
          personsShiftsWorkedDays.rowKeySet().size());


      // Memorizzo le inconsistenze del turno
      log.debug("Chiamo la getShiftInconsistencyTimestampTable PER TROVARE LE INCONSISTENZE "
          + "del turno %s e memorizzarle", type);
      shiftManager.getShiftInconsistencyTimestampTable(
          personsShiftDays, personsShiftInconsistentAbsences, shiftType);
      log.debug("* Num di persone nella personsShiftInconsistentAbsences = {}", 
          personsShiftInconsistentAbsences.rowKeySet().size());
    }
    

    log.debug("CALCOLA I MINUTI MANCANTI DA personsShiftInconsistentAbsences E LI METTE "
        + "in totalShiftSumHours");


    // Calcola i giorni totali di turno effettuati e le eventuali ore mancanti
    totalPersonShiftWorkedTime =
        shiftManager.calcShiftWorkedDaysAndLackTime(
            personsShiftsWorkedDays, personsShiftInconsistentAbsences);

    // save the total requested Shift Hours in the DB
    log.debug("AGGIORNA IL DATABASE");

    List<Competence> savedCompetences =
        shiftManager.updateDbShiftCompetences(totalPersonShiftWorkedTime, year, month);


    // Contains for each person the numer of days and hours of worked shift
    Table<Person, String, String> totalShiftInfo =
        HashBasedTable.<Person, String, String>create();

    // crea la tabella con le informazioni per il report PDF mensile
    totalShiftInfo =
        shiftManager.getPersonsReportShiftInfo(totalPersonShiftWorkedTime, savedCompetences);

    Options options = new Options();
    options.pageSize = IHtmlToPdfTransformer.A4L;

    List<String> thInconsistence =
        Arrays.asList(
            Messages.get("PDFReport.thAbsences"), Messages.get("PDFReport.thNoStampings"),
            Messages.get("PDFReport.thMissingTime"), Messages.get("PDFReport.thBadStampings"),
            Messages.get("PDFReport.thMissions"), Messages.get("PDFReport.thIncompleteTime"),
            Messages.get("PDFReport.thWarnStampings"));
    List<String> thShift =
        Arrays.asList(
            Messages.get("PDFReport.thDays"), Messages.get("PDFReport.thLackTime"),
            Messages.get("PDFReport.thReqHour"), Messages.get("PDFReport.thAppHour"),
            Messages.get("PDFReport.thExceededMin"));

    log.debug("thInconsistence={} - thShift={}", thInconsistence, thShift);

    LocalDate today = new LocalDate();
    String shiftDesc = shiftCategory.description;
    String supervisor =
        shiftCategory.supervisor.name.concat(" ").concat(shiftCategory.supervisor.surname);

    renderPDF(options, today, firstOfMonth, totalShiftInfo, personsShiftInconsistentAbsences,
        thInconsistence, thShift, shiftDesc, supervisor);
  }


  /**
   * Crea il file PDF con il calendario mensile dei turni di tipo 'A, B' per il mese
   * 'month' dell'anno 'year'. (portale sistorg).
   *
   * @author arianna
   */
  @BasicAuth
  public static void exportMonthCalAsPDF(int year, int month, Long type) {

    log.debug("sono nella exportMonthCalAsPDF con shiftCategory={} year={} e month={}",
        type, year, month);

    ShiftCategories shiftCategory = ShiftCategories.findById(type);
    if (shiftCategory == null) {
      notFound(String.format("shiftCategory shiftCategory = %s doesn't exist", shiftCategory));
    }

    List<ShiftType> shiftTypes =
        ShiftType.find(
            "SELECT st FROM ShiftType st WHERE st.shiftCategories = ?", shiftCategory).fetch();
    log.debug("shiftTypes={}", shiftTypes);

    // crea la tabella dei turni mensile (tipo turno, giorno) ->
    //  (persona turno mattina, persona turno pomeriggio)
    Table<String, Integer, ShiftManager.Sd> shiftCalendar =
        HashBasedTable.<String, Integer, ShiftManager.Sd>create();


    // prende il primo giorno del mese
    LocalDate firstOfMonth = new LocalDate(year, month, 1);

    for (ShiftType shiftType : shiftTypes) {
      log.debug("controlla type={}", shiftType.type);

      // put the shift information i ìn the calendar shiftCalendar
      shiftManager.buildMonthlyShiftCalendar(firstOfMonth, shiftType, shiftCalendar);

    }

    LocalDate today = new LocalDate();
    String shiftDesc = shiftCategory.description;
    String supervisor =
        shiftCategory.supervisor.name.concat(" ").concat(shiftCategory.supervisor.surname);
    renderPDF(today, firstOfMonth, shiftCalendar, shiftDesc, supervisor);
  }


  /**
   * Restituisce la lista delle assenze delle persone di un certo turno in un certo periodo di
   * tempo.
   *
   * @author arianna
   *
   */
  @BasicAuth
  public static void absence(
      Integer yearFrom, Integer monthFrom, Integer dayFrom,
      Integer yearTo, Integer monthTo, Integer dayTo, String type) {
    response.accessControl("*");

    ShiftType shiftType = shiftDao.getShiftTypeByType(type);
    if (shiftType == null) {
      notFound(String.format("ShiftType type = %s doesn't exist", type));
    }
    log.debug("Cerco Turnisti di tipo {}", shiftType.type);

    // get the list of persons involved in the shift of type 'type'
    List<Person> personList =
        JPA.em().createQuery(
            "SELECT p FROM PersonShiftShiftType psst JOIN psst.personShift ps JOIN ps.person p "
            + "WHERE psst.shiftType.type = :type "
            + "AND (psst.beginDate IS NULL OR psst.beginDate <= now()) "
            + "AND (psst.endDate IS NULL OR psst.endDate >= now())")
            .setParameter("type", type)
            .getResultList();

    log.debug("Shift personList called, found {} shift person", personList.size());

    // Lists of absence for a single shift person and for all persons
    List<Absence> absencePersonShiftDays = new ArrayList<Absence>();

    // List of absence periods
    List<AbsenceShiftPeriod> absenceShiftPeriods = new ArrayList<AbsenceShiftPeriod>();

    if (personList.size() == 0) {
      render(absenceShiftPeriods);
      return;
    }

    LocalDate from = new LocalDate(yearFrom, monthFrom, dayFrom);
    LocalDate to = new LocalDate(yearTo, monthTo, dayTo);

    absencePersonShiftDays = absenceDao.getAbsenceForPersonListInPeriod(personList, from, to);

    log.debug("Trovati {} giorni di assenza", absencePersonShiftDays.size());

    absenceShiftPeriods =
        shiftManager.getAbsentShiftPeriodsFromAbsentShiftDays(absencePersonShiftDays, shiftType);

    log.debug("Find {} absenceShiftPeriod. AbsenceShiftPeriod = {}",
        absenceShiftPeriods.size(), absenceShiftPeriods.toString());
    render(absenceShiftPeriods);
  }


  @BasicAuth
  public static void iCal(@Required String type, @Required int year, Long personId) {
    if (validation.hasErrors()) {
      badRequest("Parametri mancanti. " + validation.errors());
    }
    Optional<User> currentUser = Security.getUser();

    response.accessControl("*");

    ShiftType shiftType = shiftDao.getShiftTypeByType(type);

    if (shiftType == null) {
      notFound(String.format("ShiftType type = %s doesn't exist", type));
    }

    ImmutableList<Person> canAccess =
            ImmutableList.<Person>builder()
                    .addAll(personDao.getPersonForShift(type))
                    .add(shiftType.shiftCategories.supervisor).build();


    if (!currentUser.isPresent() || currentUser.get().person == null
            || !canAccess.contains(currentUser.get().person)) {
      log.debug("Accesso all'iCal dei turni non autorizzato: Type = {}, Current User = {}, "
                      + "canAccess = {}",
              type, currentUser.get(), canAccess, currentUser.get());
      unauthorized();
    }

    try {
      Optional<Calendar> calendar =
          shiftManager.createCalendar(type, Optional.fromNullable(personId), year);
      if (!calendar.isPresent()) {
        log.info("Impossible to create shift calendar for personId = {}, type = {}, year = {}",
            personId, type, year);
        notFound(
            String.format("Person id = %d is not associated to a shift of type = %s",
                personId, type));
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      CalendarOutputter outputter = new CalendarOutputter();
      outputter.output(calendar.get(), bos);

      response.setHeader("Content-Type", "application/ics");
      InputStream is = new ByteArrayInputStream(bos.toByteArray());
      renderBinary(is, "reperibilitaRegistro.ics");
      bos.close();
      is.close();
    } catch (IOException ex) {
      log.error("Io exception building ical", ex);
    } catch (ValidationException ex) {
      log.error("Validation exception generating ical", ex);
    }
  }
}
