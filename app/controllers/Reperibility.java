
package controllers;

import static play.modules.pdf.PDF.renderPDF;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import controllers.Resecure.BasicAuth;

import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;

import it.cnr.iit.epas.JsonReperibilityChangePeriodsBinder;
import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
import manager.ReperibilityManager;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.User;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.binding.As;
import play.data.validation.Required;
import play.i18n.Messages;
import play.modules.pdf.PDF.Options;
import play.mvc.Controller;
import play.mvc.With;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;


/**
 * Controller con tutti i metodi per la gestione (via REST) delle reperibilità.
 *
 * @author cristian
 */
@Slf4j
@With(Resecure.class)
public class Reperibility extends Controller {

  private static String codFr = "207";
  private static String codFs = "208";

  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonReperibilityDayDao personReperibilityDayDao;
  @Inject
  private static ReperibilityManager reperibilityManager;
  @Inject
  private static AbsenceDao absenceDao;
  @Inject
  private static AbsenceManager absenceManager;
  @Inject
  private static CompetenceCodeDao competenceCodeDao;
  @Inject
  private static CompetenceDao competenceDao;

  /**
   * Restituisce la lista dei reperibili attivi al momento di un determinato tipo.
   *
   * @author arianna
   *
   */
  @BasicAuth
  public static void personList(Long type) {
    response.accessControl("*");
    //response.setHeader("Access-Control-Allow-Origin", "http://sistorg.devel.iit.cnr.it");

    log.debug("Esegue la personList con type={}", type);

    List<Person> personList = personDao.getPersonForReperibility(type);
    log.debug("Reperibility personList called, found {} reperible person", personList.size());
    render(personList);
  }

  /**
   * Fornisce i periodi di reperibilità del personale reperibile di tipo
   * 'type' nell'intervallo di tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'.
   *
   * <p>Fornisce i periodi di reperibilità del personale reperibile di tipo
   * 'type' nell'intervallo di tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'
   * </p>
   * <p>
   * per provarlo:
   *    curl --user sistorg -H "Accept: application/json" http://localhost:9001/reperibility/1/find/2012/11/26/2013/01/06
   * </p>
   * @author cristian
   * @author arianna
   *
   */
  @BasicAuth
  public static void find(Integer yearFrom, Integer monthFrom, Integer dayFrom,
      Integer yearTo, Integer monthTo, Integer dayTo, Long type) {
    response.accessControl("*");
    //response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

    // reperibility type validation
    PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
    notFoundIfNull(reperibilityType, String.format("ReperibilityType id = %s doesn't exist", type));

    PersonReperibilityType prt = personReperibilityDayDao.getPersonReperibilityTypeById(type);

    // date interval construction
    final LocalDate from = new LocalDate(yearFrom, monthFrom, dayFrom);
    final LocalDate to = new LocalDate(yearTo, monthTo, dayTo);

    List<PersonReperibilityDay> reperibilityDays =
        personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(
            from, to, reperibilityType, Optional.<PersonReperibility>absent());

    log.debug("Reperibility find called from {} to {}, found {} reperibility days",
        from, to, reperibilityDays.size());

    // Manager ReperibilityManager called to find out the reperibilityPeriods
    List<ReperibilityPeriod> reperibilityPeriods =
        reperibilityManager.getPersonReperibilityPeriods(reperibilityDays, prt);
    log.debug("Find {} reperibilityPeriods. ReperibilityPeriods = {}",
        reperibilityPeriods.size(), reperibilityPeriods);

    render(reperibilityPeriods);
  }


  /**
   * Fornisce la lista del personale reperibile di tipo 'type' nell'intervallo di
   * tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'.
   *
   * @author arianna
   */
  @BasicAuth
  public static void who(Integer yearFrom, Integer monthFrom, Integer dayFrom,
      Integer yearTo, Integer monthTo, Integer dayTo, Long type) {
    response.accessControl("*");
    //response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

    List<Person> personList = new ArrayList<Person>();

    // reperibility type validation
    PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
    notFoundIfNull(reperibilityType, String.format("ReperibilityType id = %s doesn't exist", type));

    // date interval construction
    final LocalDate from = new LocalDate(yearFrom, monthFrom, dayFrom);
    final LocalDate to = new LocalDate(yearTo, monthTo, dayTo);

    List<PersonReperibilityDay> reperibilityDays =
            personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(
                    from, to, reperibilityType, Optional.<PersonReperibility>absent());

    log.debug("Reperibility who called from {} to {}, found {} reperibility days",
        from, to, reperibilityDays.size());

    personList = reperibilityManager.getPersonsFromReperibilityDays(reperibilityDays);
    log.debug("trovati {} reperibili: {}", personList.size(), personList);

    render(personList);
  }


  /**
   * Legge le assenze dei reperibili di una determinata tipologia in un dato
   * intervallo di tempo (portale sistorg).
   *
   * @author arianna
   */
  @BasicAuth
  public static void absence(Integer yearFrom, Integer monthFrom, Integer dayFrom,
      Integer yearTo, Integer monthTo, Integer dayTo, Long type) {
    response.accessControl("*");
    //response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

    log.trace("Sono nella absence");

    PersonReperibilityType repType = PersonReperibilityType.findById(type);
    notFoundIfNull(repType, String.format("PersonReperibilityType type = %s doesn't exist", type));

    // date interval construction
    final LocalDate from = new LocalDate(yearFrom, monthFrom, dayFrom);
    final LocalDate to = new LocalDate(yearTo, monthTo, dayTo);

    // read the reperibility person list
    List<Person> personList = personDao.getPersonForReperibility(type);
    log.debug("Reperibility personList called, found {} reperible person of type {}",
        personList.size(), type);

    // Lists of absence for a single reperibility person and for all persons
    List<Absence> absencePersonReperibilityDays = new ArrayList<Absence>();

    // List of absence periods
    List<AbsenceReperibilityPeriod> absenceReperibilityPeriods =
        new ArrayList<AbsenceReperibilityPeriod>();

    if (personList.size() == 0) {
      render(absenceReperibilityPeriods);
      return;
    }

    absencePersonReperibilityDays =
        absenceDao.getAbsenceForPersonListInPeriod(personList, from, to);

    log.debug("Trovati {} giorni di assenza", absencePersonReperibilityDays.size());

    // get the absent reperibility periods from the absent days
    absenceReperibilityPeriods =
        reperibilityManager.getAbsentReperibilityPeriodsFromAbsentReperibilityDays(
            absencePersonReperibilityDays, repType);

    log.debug("Find {} absenceReperibilityPeriod. AbsenceReperibilityPeriod = {}",
        absenceReperibilityPeriods.size(), absenceReperibilityPeriods.toString());
    render(absenceReperibilityPeriods);
  }


  /**
   * Restituisce la lista delle persone reperibili assenti di una determinata
   * tipologia in un dato intervallo di tempo (portale sistorg).
   *
   * @author arianna
   */
  @BasicAuth
  public static void whoIsAbsent(Integer yearFrom, Integer monthFrom, Integer dayFrom,
      Integer yearTo, Integer monthTo, Integer dayTo, Long type) {
    response.accessControl("*");
    //response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

    // date interval construction
    final LocalDate from = new LocalDate(yearFrom, monthFrom, dayFrom);
    final LocalDate to = new LocalDate(yearTo, monthTo, dayTo);

    // read the reperibility person list
    final List<Person> personList = personDao.getPersonForReperibility(type);
    log.debug("Reperibility personList called, found {} reperible person of type {}",
        personList.size(), type);

    if (personList.size() == 0) {
      render(personList);
      return;
    }

    // Lists of absence for a single reperibility person and for all persons
    final List<Absence> absencePersonReperibilityDays =
        absenceDao.getAbsenceForPersonListInPeriod(personList, from, to);
    log.debug("Trovati {} giorni di assenza", absencePersonReperibilityDays.size());

    final List<Person> absentPersonsList =
        absenceManager.getPersonsFromAbsentDays(absencePersonReperibilityDays);

    log.debug("Find {} person. absentPersonsList = {}",
        absentPersonsList.size(), absentPersonsList.toString());
    render(absentPersonsList);
  }


  /**
   * Aggiorna le informazioni relative alla Reperibilità del personale.
   * <p>
   * Per provarlo è possibile effettuare una chiamata JSON come questa: $  curl -H "Content-Type:
   * application/json" -X PUT \ -d '[ {"id" : "49","start" : 2012-12-05,"end" : "2012-12-10",
   * "reperibility_type_id" : "1"}, { "id" : "139","start" : "2012-12-12" , "end" : "2012-12-14",
   * "reperibility_type_id" : "1" } , { "id" : "139","start" : "2012-12-17","end" : "2012-12-18",
   * "reperibility_type_id" : "1" } ]' \ http://localhost:9000/reperibility/1/update/2012/12
   * </p>
   *
   * @author cristian
   * @author arianna
   *
   */
  @BasicAuth
  public static void update(Long type, Integer year, Integer month,
      @As(binder = JsonReperibilityPeriodsBinder.class) ReperibilityPeriods body) {

    log.debug("update: Received reperebilityPeriods {}", body);
    if (body == null) {
      badRequest();
    }

    //PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
    final PersonReperibilityType reperibilityType =
        personReperibilityDayDao.getPersonReperibilityTypeById(type);
    if (reperibilityType == null) {
      throw new IllegalArgumentException(
          String.format("ReperibilityType id = %s doesn't exist", type));
    }

    //Conterrà i giorni del mese che devono essere attribuiti a qualche reperibile
    Set<Integer> repDaysOfMonthToRemove = new HashSet<Integer>();

    repDaysOfMonthToRemove =
        reperibilityManager.savePersonReperibilityDaysFromReperibilityPeriods(
            reperibilityType, year, month, body.periods);

    Logger.debug("Giorni di reperibilità da rimuovere = %s", repDaysOfMonthToRemove);

    int deletedRep =
        reperibilityManager.deleteReperibilityDaysFromMonth(
            reperibilityType, year, month, repDaysOfMonthToRemove);
    log.info("Deleted {} days, reperibilityType={}, {}/{}, repDaysOfMonthToRemove={}",
            deletedRep, reperibilityType, year, month, repDaysOfMonthToRemove);
  }


  /**
   * Scambia due periodi di reperibilità di due persone reperibili diverse.
   * <p>
   * Per provarlo è possibile effettuare una chiamata JSON come questa: $  curl -H "Content-Type:
   * application/json" -X PUT \ -d '[ {"mail_req" : "ruberti@iit.cnr.it", "mail_sub" :
   * "lorenzo.rossi@iit.cnr.it", "req_start_date" : "2012-12-10", "req_end_date" : "2012-12-10",
   * "sub_start_date" : "2012-12-10", "sub_end_date" : "2012-12-10"} ]' \
   * http://scorpio.nic.it:9001/reperibility/1/changePeriods
   * </p>
   *
   * @author arianna
   */
  @BasicAuth
  public static void changePeriods(
      Long type, @As(binder = JsonReperibilityChangePeriodsBinder.class) ReperibilityPeriods body) {

    log.debug("update: Received reperebilityPeriods %s", body);
    if (body == null) {
      badRequest();
    }

    PersonReperibilityType reperibilityType =
        personReperibilityDayDao.getPersonReperibilityTypeById(type);

    notFoundIfNull(reperibilityType, String.format("ReperibilityType id = %s doesn't exist", type));

    boolean changed =
        reperibilityManager.changeTwoReperibilityPeriods(reperibilityType, body.periods);

    if (changed) {
      log.info("Periodo di reperibilità cambiato con successo!");
    } else {
      log.info("Il cambio di reperibilità non è stato effettuato");
    }

  }


  /**
   * Crea il file PDF con il calendario annuale delle reperibilità di tipi
   * 'type' per l'anno 'year' (portale sistorg).
   *
   * @author arianna, cristian
   *
   */
  @BasicAuth
  public static void exportYearAsPDF(int year, Long reperibilityId) {

    PersonReperibilityType reperibilityType =
        personReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);
    notFoundIfNull(
        reperibilityType, String.format("ReperibilityType id = %s doesn't exist", reperibilityId));

    // build the reperibility calendar
    List<Table<Person, Integer, String>> reperibilityMonths = Lists.newArrayList();
    reperibilityMonths =
        reperibilityManager.buildYearlyReperibilityCalendar(year, reperibilityType);

    // build the reperibility summary report
    Table<Person, String, Integer> reperibilitySumDays =
        HashBasedTable.<Person, String, Integer>create();
    reperibilitySumDays = reperibilityManager.buildYearlyReperibilityReport(reperibilityMonths);
    log.info(
        "Creazione del documento PDF con il calendario annuale delle reperibilità per l'anno %s",
        year);


    LocalDate firstOfYear = new LocalDate(year, 1, 1);
    Options options = new Options();
    options.pageSize = IHtmlToPdfTransformer.A4L;
    renderPDF(options, year, firstOfYear, reperibilityMonths, reperibilitySumDays);
  }


  /**
   * Restituisce una tabella con le eventuali inconsistenze tra le timbrature dei
   * reperibili di un certo tipo e i turni di reperibilità svolti in un determinato periodo di tempo
   * ritorna una tabella del tipo (Person, [thNoStamping, thAbsence], List<'gg MMM'>).
   *
   * @author arianna
   */
  public static Table<Person, String, List<String>> getInconsistencyTimestamps2Reperibilities(
      Long reperibilityId, LocalDate startDate, LocalDate endDate) {
    // for each person contains days with absences and no-stamping  matching the reperibility days
    Table<Person, String, List<String>> inconsistentAbsence =
        TreeBasedTable.<Person, String, List<String>>create();

    //PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);
    PersonReperibilityType reperibilityType =
        personReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);
    notFoundIfNull(
        reperibilityType, String.format("ReperibilityType id = %s doesn't exist", reperibilityId));

    List<PersonReperibilityDay> personReperibilityDays =
        personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(
            startDate, endDate, reperibilityType, Optional.<PersonReperibility>absent());

    inconsistentAbsence =
        reperibilityManager.getReperibilityInconsistenceAbsenceTable(
            personReperibilityDays, startDate, endDate);

    return inconsistentAbsence;
  }


  /**
   * Crea il file PDF con il resoconto mensile delle reperibilità di tipo 'type' per
   * il mese 'month' dell'anno 'year'.
   * Segnala le eventuali inconsistenze con le assenze o le mancate timbrature
   * (portale sistorg)
   *
   * @author arianna
   *
   */
  @BasicAuth
  public static void exportMonthAsPDF(
      @Required int year, @Required int month, @Required Long reperibilityId) {

    if (validation.hasErrors()) {
      badRequest("Parametri mancanti. " + validation.errors());
    }

    PersonReperibilityType reperibilityType =
        personReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);

    notFoundIfNull(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));

    final LocalDate today = LocalDate.now();

    // for each person contains the number of rep days fr o fs (feriali o festivi)
    final Table<Person, String, Integer> reperibilitySumDays =
        TreeBasedTable.<Person, String, Integer>create();

    // for each person contains the list of the rep periods divided by fr o fs
    final Table<Person, String, List<String>> reperibilityDateDays =
        TreeBasedTable.<Person, String, List<String>>create();

    // get the Competence code for the reperibility working or non-working days
    final CompetenceCode competenceCodeFs = competenceCodeDao.getCompetenceCodeByCode(codFs);

    final CompetenceCode competenceCodeFr = competenceCodeDao.getCompetenceCodeByCode(codFr);

    log.debug("Creazione dei  competenceCodeFS competenceCodeFR {}/{}",
        competenceCodeFs, competenceCodeFr);

    // get all the reperibility of a certain type in a certain month
    LocalDate firstOfMonth = new LocalDate(year, month, 1);

    List<PersonReperibilityDay> personReperibilityDays =
            personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(
                firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(),
                reperibilityType, Optional.<PersonReperibility>absent());

    log.debug("dimensione personReperibilityDays = {}", personReperibilityDays.size());

    // update the reperibility days in the DB
    int updatedCompetences =
        reperibilityManager.updateDBReperibilityCompetences(personReperibilityDays, year, month);
    log.debug("Salvate o aggiornate {} competences", updatedCompetences);

    // builds the table with the summary of days and reperibility periods description
    // reading data from the Competence table in the DB
    List<Competence> frCompetences =
        competenceDao.getCompetenceInReperibility(reperibilityType, year, month, competenceCodeFr);
    log.debug("Trovate {} competences di tipo {} nel mese {}/{}",
        frCompetences.size(), reperibilityType, month, year);

    // update  reports for the approved days and reasons for the working days
    reperibilityManager.updateReperibilityDaysReportFromCompetences(
        reperibilitySumDays, frCompetences);
    reperibilityManager.updateReperibilityDatesReportFromCompetences(
        reperibilityDateDays, frCompetences);


    // builds the table with the summary of days and reperibility periods description
    // reading data from the Competence table in the DB
    List<Competence> fsCompetences =
        competenceDao.getCompetenceInReperibility(reperibilityType, year, month, competenceCodeFs);
    log.debug("Trovate %d competences di tipo {} nel mese {}/{}",
        fsCompetences.size(), reperibilityType, month, year);

    // update  reports for the approved days and reasons for the holidays
    reperibilityManager.updateReperibilityDaysReportFromCompetences(
        reperibilitySumDays, fsCompetences);
    reperibilityManager.updateReperibilityDatesReportFromCompetences(
        reperibilityDateDays, fsCompetences);

    // get the table with the absence and no stampings inconsistency
    // for each person contains days with absences and no-stamping  matching the reperibility days
    final Table<Person, String, List<String>> inconsistentAbsence =
        reperibilityManager.getReperibilityInconsistenceAbsenceTable(
            personReperibilityDays, firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue());

    log.info("Creazione del documento PDF con il resoconto delle reperibilità per il periodo "
        + "{}/{} Fs={} Fr={}",
        firstOfMonth.plusMonths(0).monthOfYear().getAsText(),
        firstOfMonth.plusMonths(0).year().getAsText(), codFs, codFr);

    final String cFr = codFr;
    final String cFs = codFs;
    final String thNoStamp = Messages.get("PDFReport.thNoStampings");
    final String thAbs = Messages.get("PDFReport.thAbsences");

    renderPDF(today, firstOfMonth, reperibilitySumDays, reperibilityDateDays,
        inconsistentAbsence, cFs, cFr, thNoStamp, thAbs);
  }

  /**
   * Genera il calendario iCal delle reperibilità.
   * @param type il tipo di reperibilità (solo le reperibilità di questo tipo verrano mostrate
   * @param year l'anno di cui mostrare le reperibilità
   * @param personId l'eventuale personId se si vuole mostrare il calendario di una sola persona
   */
  @BasicAuth
  public static void iCal(@Required Long type, @Required int year, Long personId) {

    if (validation.hasErrors()) {
      badRequest("Parametri mancanti. " + validation.errors());
    }
    Optional<User> currentUser = Security.getUser();

    response.accessControl("*");

    PersonReperibilityType reperibilityType =
        personReperibilityDayDao.getPersonReperibilityTypeById(type);
    if (reperibilityType == null) {
      notFound(String.format("ReperibilityType id = %s doesn't exist", type));
    }

    ImmutableList<Person> canAccess =
            ImmutableList.<Person>builder()
                    .addAll(personDao.getPersonForReperibility(type))
                    .add(reperibilityType.supervisor).build();


    if (!currentUser.isPresent() || currentUser.get().person == null
            || !canAccess.contains(currentUser.get().person)) {
      log.debug(
          "Accesso all'iCal delle reperibilità non autorizzato: Type = {}, Current User = {}, "
          + "canAccess = {}",
              type, currentUser.get(), canAccess, currentUser.get());
      unauthorized();
    }


    try {
      Optional<Calendar> calendar =
          reperibilityManager.createCalendar(type, Optional.fromNullable(personId), year);
      notFoundIfNull(
          calendar.orNull(),
          String.format("No person associated to a reperibility of type = %s", reperibilityType));

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      CalendarOutputter outputter = new CalendarOutputter();
      outputter.output(calendar.get(), bos);

      response.setHeader("Content-Type", "application/ics");
      InputStream is = new ByteArrayInputStream(bos.toByteArray());
      renderBinary(is, "reperibilitaRegistro.ics");
      bos.close();
      is.close();
    } catch (IOException e) {
      log.error("Io exception building ical", e);
      error("Io exception building ical");
    } catch (ValidationException e) {
      log.error("Validation exception generating ical", e);
      error("Validation exception generating ical");
    }
  }

}
