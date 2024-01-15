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

package controllers;

import static play.modules.pdf.PDF.renderPDF;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import controllers.Resecure.BasicAuth;
import controllers.Resecure.NoCheck;
import dao.AbsenceDao;
import dao.PersonDao;
import dao.PersonShiftDayDao;
import dao.RoleDao;
import dao.ShiftDao;
import dao.UsersRolesOfficesDao;
import it.cnr.iit.epas.JsonShiftPeriodsBinder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ShiftManager;
import manager.ShiftManager2;
import models.Competence;
import models.Office;
import models.Person;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.Role;
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
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.joda.time.LocalDate;
import play.data.binding.As;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.modules.pdf.PDF.Options;
import play.mvc.Controller;
import play.mvc.With;


/**
 * Implements work shifts.
 *
 * @author Arianna Del Soldato
 * @author Dario Tagliaferri
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
  private static ShiftManager2 shiftManager2;
  @Inject
  private static AbsenceDao absenceDao;
  @Inject
  private static UsersRolesOfficesDao uroDao;
  @Inject
  private static RoleDao roleDao;


  /**
   * Restituisce la lista delle persone in un determinato turno.
   *
   * @author Arianna Del Soldato
   */
  //@BasicAuth
  public static void personList(String type) {
    response.accessControl("*");

    log.debug("Cerco persone del turno {}", type);

    ShiftType shiftType = shiftDao.getShiftTypeByType(type);
    notFoundIfNull(shiftType, String.format("ShiftType type = %s doesn't exist", type));

    log.debug("Cerco Turnisti di tipo {}", shiftType.getType());

    //final List<Person> personList = personDao.getPersonForShift(type);
    final List<PersonShiftShiftType> personList = shiftDao
        .getAssociatedPeopleToShift(shiftType, Optional.fromNullable(LocalDate.now()));

    log.debug("Shift personList called, found {} shift person", personList.size());

    render(personList);
  }


  /**
   * Get shifts from the DB and render to the sistorg portal calendar.
   *
   * @author Arianna Del Soldato
   */
  //@BasicAuth
  public static void timeTable(String type) {
    response.accessControl("*");

    log.trace("Cercata la time table di un turno");

    // type validation
    ShiftType shiftType = shiftDao.getShiftTypeByType(type);
    notFoundIfNull(shiftType, String.format("ShiftType type = %s doesn't exist", type));

    log.debug("Cerco la time table del turno di tipo {}", shiftType.getType());

    ShiftTimeTable shiftTimeTable = shiftType.getShiftTimeTable();

    render(shiftTimeTable);

  }


  /**
   * Get shifts from the DB and render to the sistorg portal calendar.
   *
   * @author Arianna Del Soldato
   * @author Dario Tagliaferri
   */
  //@BasicAuth
  public static void find(
      Integer yearFrom, Integer monthFrom, Integer dayFrom,
      Integer yearTo, Integer monthTo, Integer dayTo, String type) {

    response.accessControl("*");

    // type validation
    ShiftType shiftType = shiftDao.getShiftTypeByType(type);
    notFoundIfNull(shiftType, String.format("ShiftType type = %s doesn't exist", type));

    log.debug("Cerco turni di tipo {}", shiftType.getType());

    LocalDate from = new LocalDate(yearFrom, monthFrom, dayFrom);
    LocalDate to = new LocalDate(yearTo, monthTo, dayTo);

    List<PersonShiftDay> personShiftDays =
        shiftDao.getShiftDaysByPeriodAndType(from, to, shiftType);
    log.debug("Shift find called from {} to {}, type {} - found {} shift days",
        from, to, shiftType.getType(), personShiftDays.size());

    List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
    List<ShiftPeriod> deletedShiftPeriods = new ArrayList<ShiftPeriod>();

    // get the shift periods
    shiftPeriods = shiftManager.getPersonShiftPeriods(personShiftDays);

    // get the cancelled shifts of type shiftType
    List<ShiftCancelled> shiftCancelled =
        shiftDao.getShiftCancelledByPeriodAndType(from, to, shiftType);
    log.debug("ShiftCancelled find called from {} to {}, type {} - found {} shift days",
        from, to, shiftType.getType(), shiftCancelled.size());

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
   * @author Arianna Del Soldato
   */
  //@BasicAuth
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

    log.debug("shiftType={}", shiftType.getDescription());

    // save the recived shift periods with type shiftType in the month "month" of the "year" year
    List<String> list = shiftManager
        .savePersonShiftDaysFromShiftPeriods(shiftType, year, month, body);
    renderJSON(list);
  }


  /**
   * Crea una tabella con le eventuali inconsistenze tra le timbrature di un
   * turnista e le fasce di orario da rispettare per un determinato turno, in un dato periodo di
   * tempo (Person, [thNoStampings, thBadStampings, thAbsences], List [gg MMM]).
   *
   * @author Arianna Del Soldato
   */
  //@BasicAuth
  public static void getInconsistencyTimestamps2Timetable(
      ShiftType shiftType, LocalDate startDate, LocalDate endDate) {

    // crea la tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
    Table<Person, String, List<String>> inconsistentAbsence =
        HashBasedTable.<Person, String, List<String>>create();

    // seleziona le persone nel turno 'shiftType' da inizio a fine mese
    List<PersonShiftDay> personShiftDays =
        personShiftDayDao.byTypeInPeriod(startDate, endDate, shiftType, Optional.absent());

    //inconsistentAbsence = CompetenceUtility.getShiftInconsistencyTimestampTable(personShiftDays);
    shiftManager
        .getShiftInconsistencyTimestampTable(personShiftDays, inconsistentAbsence, shiftType);

    //return inconsistentAbsence;
  }


  /**
   * Crea il file PDF con il resoconto mensile dei turni dello IIT il mese 'month'
   * dell'anno 'year' (portale sistorg).
   *
   * @author Arianna Del Soldato
   */
  //@BasicAuth
  public static void exportMonthAsPDF(int year, int month, Long shiftCategoryId) {
    //    int year = params.get("year", Integer.class);
    //    int month = params.get("month", Integer.class);
    if (shiftCategoryId == null) {
      shiftCategoryId = params.get("type", Long.class);
    }
    

    log.debug("sono nella exportMonthAsPDF con shiftCategory={} year={} e month={}",
        shiftCategoryId, year, month);

    final LocalDate firstOfMonth = new LocalDate(year, month, 1);
    final LocalDate lastOfMonth = firstOfMonth.dayOfMonth().withMaximumValue();

    Comparator<String> nullSafeStringComparator = Comparator
        .nullsFirst(String::compareToIgnoreCase);

    //  Used TreeBasedTable because of the alphabetical name order (persona, A/B, num. giorni)
    Table<Person, String, Integer> personsShiftsWorkedDays =
        TreeBasedTable.<Person, String, Integer>create(
            Person.personComparator(), nullSafeStringComparator);

    // crea la tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
    // (person, [thAbsences, thNoStampings, thBadStampings], <giorni/fasce orarie inconsistenti>)
    Table<Person, String, List<String>> personsShiftInconsistentAbsences =
        TreeBasedTable.<Person, String, List<String>>create(Person.personComparator(),
            nullSafeStringComparator);



    ShiftCategories shiftCategory = ShiftCategories.findById(shiftCategoryId);
    if (shiftCategory == null) {
      notFound(String.format("shiftCategory shiftCategory = %s doesn't exist", shiftCategory));
    }

    log.debug("shiftCategory = {}", shiftCategory);

    // Contains the number of the effective hours of worked shifts
    Table<Person, String, Integer> totalPersonShiftWorkedTime =
        TreeBasedTable.<Person, String, Integer>create(
            Person.personComparator(), nullSafeStringComparator);
    // Legge i turni associati alla categoria (es: A, B)
    List<ShiftType> shiftTypes = shiftDao.getTypesByCategory(shiftCategory);
    
    // for each shift
    for (ShiftType shiftType : shiftTypes) {
      String type = shiftType.getType();
      log.debug("ELABORA TURNO TYPE={}", type);

      // seleziona i giorni di turno di tutte le persone associate al turno 'shiftType'
      // da inizio a fine mese
      List<PersonShiftDay> personsShiftDays =
          personShiftDayDao.byTypeInPeriod(firstOfMonth, lastOfMonth, shiftType, Optional.absent());

      log.debug("CALCOLA IL NUM DI GIORNI EFFETTUATI NEL TURNO PER OGNI PERSONA");
      // conta e memorizza i giorni di turno per ogni persona
      shiftManager.countPersonsShiftsDays(personsShiftDays, personsShiftsWorkedDays);
      log.debug("* Num di persone nella personsShiftsWorkedDays = {}",
          personsShiftsWorkedDays.rowKeySet().size());

      // Memorizzo le inconsistenze del turno
      log.debug(
          "Chiamo la getShiftInconsistencyTimestampTable PER TROVARE LE INCONSISTENZE "
          + "del turno %s e memorizzarle", type);
      shiftManager
          .getShiftInconsistencyTimestampTable(personsShiftDays, personsShiftInconsistentAbsences,
              shiftType);
      log.debug("* Num di persone nella personsShiftInconsistentAbsences = {}",
          personsShiftInconsistentAbsences.rowKeySet().size());
    }

    log.debug("CALCOLA I MINUTI MANCANTI DA personsShiftInconsistentAbsences E LI METTE "
        + "in totalShiftSumHours");

    // Calcola i giorni totali di turno effettuati e le eventuali ore mancanti e li mette in
    // personsShiftInconsistentAbsences
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
            Messages.get("PDFReport.thAbsences"), Messages.get("PDFReport.thMissions"),
            Messages.get("PDFReport.thNoStampings"), Messages.get("PDFReport.thBadWorkindDay"),
            Messages.get("PDFReport.thBadStampings"), Messages.get("PDFReport.thMissingTime"),
            Messages.get("PDFReport.thIncompleteTime"),
            Messages.get("PDFReport.thWarnStampings"));
    List<String> thShift =
        Arrays.asList(
            Messages.get("PDFReport.thDays"), Messages.get("PDFReport.thLackTime"),
            Messages.get("PDFReport.thReqHour"), Messages.get("PDFReport.thAppHour"),
            Messages.get("PDFReport.thExceededMin"));

    log.debug("thInconsistence={} - thShift={}", thInconsistence, thShift);

    LocalDate today = new LocalDate();
    String shiftDesc = shiftCategory.getDescription();
    final String supervisor = shiftCategory.getSupervisor().getFullname();
    String seatSupervisor = "";
    Office office = shiftCategory.getOffice();
    List<User> directors = uroDao
        .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.SEAT_SUPERVISOR), office);
    if (!directors.isEmpty()) {
      seatSupervisor = directors.get(0).getPerson().getFullname();
    } else {
      seatSupervisor = "responsabile di sede non configurato";
    }
    

    renderPDF(options, today, firstOfMonth, totalShiftInfo, personsShiftInconsistentAbsences,
        thInconsistence, thShift, shiftDesc, supervisor, seatSupervisor, office);
  }


  /**
   * Crea il file PDF corrispondente al calendario mensile dei turni di tipo 'A, B' per il mese
   * 'month' dell'anno 'year'. (portale sistorg).
   *
   * @author Arianna Del Soldato
   */
  //@BasicAuth
  public static void exportMonthCalAsPDF(int year, int month, Long type) {

    log.debug("sono nella exportMonthCalAsPDF con shiftCategory={} year={} e month={}",
        type, year, month);

    ShiftCategories shiftCategory = ShiftCategories.findById(type);
    if (shiftCategory == null) {
      notFound(String.format("shiftCategory shiftCategory = %s doesn't exist", shiftCategory));
    }
    
    List<ShiftType> shiftTypes = shiftDao.getTypesByCategory(shiftCategory);
    
    log.debug("shiftTypes={}", shiftTypes);

    // crea la tabella dei turni mensile (tipo turno, giorno) ->
    //  (persona turno mattina, persona turno pomeriggio)
    Table<String, Integer, ShiftManager.Sd> shiftCalendar =
        HashBasedTable.<String, Integer, ShiftManager.Sd>create();

    // prende il primo giorno del mese
    LocalDate firstOfMonth = new LocalDate(year, month, 1);

    for (ShiftType shiftType : shiftTypes) {
      log.debug("controlla type={}", shiftType.getType());

      // put the shift information i ìn the calendar shiftCalendar
      shiftManager.buildMonthlyShiftCalendar(firstOfMonth, shiftType, shiftCalendar);

    }

    LocalDate today = new LocalDate();
    String shiftDesc = shiftCategory.getDescription();
    String supervisor =
        shiftCategory.getSupervisor().getName().concat(" ")
        .concat(shiftCategory.getSupervisor().getSurname());
    renderPDF(today, firstOfMonth, shiftCalendar, shiftDesc, supervisor);
  }


  /**
   * Restituisce la lista delle assenze delle persone di un certo turno in un certo periodo di
   * tempo.
   *
   * @author Arianna Del Soldato
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
    log.debug("Cerco Turnisti di tipo {}", shiftType.getType());

    // get the list of persons involved in the shift of type 'type'
    List<PersonShiftShiftType> people = shiftManager2
        .shiftWorkers(shiftType, new LocalDate(yearFrom, monthFrom, dayFrom), 
            new LocalDate(yearTo, monthTo, dayTo));
    List<Person> personList = people.stream()
        .<Person>map(psst -> psst.getPersonShift().getPerson()).collect(Collectors.toList());
    //    List<Person> personList =
    //        JPA.em().createQuery(
    //          "SELECT p FROM PersonShiftShiftType psst JOIN psst.personShift ps JOIN ps.person p "
    //                + "WHERE psst.shiftType.type = :type "
    //                + "AND (psst.beginDate IS NULL OR psst.beginDate <= now()) "
    //                + "AND (psst.endDate IS NULL OR psst.endDate >= now())")
    //            .setParameter("type", type)
    //            .getResultList();
    

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


  /**
   * Restituisce la informazioni sul turno in formato iCal.
   */
  @BasicAuth
  public static void ical(@Required String type, @Required int year) {
    if (Validation.hasErrors()) {
      badRequest("Parametri mancanti. " + Validation.errors());
    }
    Optional<User> currentUser = Security.getUser();
    log.debug("Shift::ical type={}, year={}, currentUser={}", type, year, currentUser);

    response.accessControl("*");

    ShiftType shiftType = shiftDao.getShiftTypeByType(type);

    if (shiftType == null) {
      notFound(String.format("ShiftType type = %s doesn't exist", type));
    }

    ImmutableList<Person> canAccess =
        ImmutableList.<Person>builder()
            .addAll(personDao.getPersonForShift(type, 
                LocalDate.now().withYear(year).monthOfYear().withMinimumValue()
                .dayOfMonth().withMinimumValue()))
            .add(shiftType.getShiftCategories().getSupervisor()).build();

    if (!currentUser.isPresent() || currentUser.get().getPerson() == null
        || !canAccess.contains(currentUser.get().getPerson())) {
      log.debug("Accesso all'iCal dei turni non autorizzato: Type = {}, Current User = {}, "
              + "canAccess = {}",
          type, currentUser.get(), canAccess, currentUser.get());
      unauthorized();
    }

    Long personId = currentUser.get().getPerson().getId();
    try {
      Optional<Calendar> calendar =
          shiftManager.createCalendar(type, Optional.fromNullable(personId), year);
      if (!calendar.isPresent()) {
        log.warn("Impossible to create shift calendar for personId = {}, type = {}, year = {}",
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
    }
  }



}
