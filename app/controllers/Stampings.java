package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.UserDao;
import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.Web;
import helpers.validators.StringIsTime;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.NullStringBinder;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
import manager.NotificationManager;
import manager.PersonManager;
import manager.SecureManager;
import manager.StampingManager;
import manager.configurations.EpasParam;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.recaps.troubles.PersonTroublesInMonthRecap;
import manager.recaps.troubles.PersonTroublesInMonthRecapFactory;

import models.Office;
import models.Person;
import models.PersonDay;
import models.Role;
import models.Stamping;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.data.binding.As;
import play.data.validation.CheckWith;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.JavaRules;
import security.SecurityRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;


/**
 * Controller per la gestione delle timbrature.
 *
 * @author alessandro
 */
@Slf4j
@With({Resecure.class})
public class Stampings extends Controller {

  @Inject
  private static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static StampingManager stampingManager;
  @Inject
  private static StampingDao stampingDao;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static SecureManager secureManager;
  @Inject
  private static PersonTroublesInMonthRecapFactory personTroubleRecapFactory;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static StampingHistoryDao stampingsHistoryDao;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonManager personManager;
  @Inject
  private static NotificationManager notificationManager;
  @Inject
  private static JavaRules jRules;
  @Inject
  private static UserDao userDao;

  /**
   * Tabellone timbrature dipendente.
   *
   * @param year  anno
   * @param month mese
   */
  public static void stampings(final Integer year, final Integer month) {

    IWrapperPerson wrperson = wrapperFactory
        .create(Security.getUser().get().person);

    if (!wrperson.isActiveInMonth(new YearMonth(year, month))) {
      flash.error("Non esiste situazione mensile per il mese di %s %s",
          DateUtility.fromIntToStringMonth(month), year);

      YearMonth last = wrperson.getLastActiveMonth();
      stampings(last.getYear(), last.getMonthOfYear());
    }

    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrperson.getValue(), year, month, true);

    Person person = wrperson.getValue();
    render("@personStamping", psDto, person);
  }


  /**
   * Tabellone timbrature amministratore.
   *
   * @param personId dipendente
   * @param year     anno
   * @param month    mese
   */
  public static void personStamping(final Long personId, final int year, final int month) {

    Person person = personDao.getPersonById(personId);
    Preconditions.checkNotNull(person);

    rules.checkIfPermitted(person.office);

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    if (!wrPerson.isActiveInMonth(new YearMonth(year, month))) {

      flash.error("Non esiste situazione mensile per il mese di %s",
          person.fullName(), DateUtility.fromIntToStringMonth(month));

      YearMonth last = wrapperFactory.create(person).getLastActiveMonth();
      personStamping(personId, last.getYear(), last.getMonthOfYear());
    }

    PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month, true);

    render(psDto, person);
  }

  /**
   * Nuova timbratura inserita dall'amministratore.
   *
   * @param personId id della persona
   * @param date     data
   */
  public static void blank(Long personId, LocalDate date) {

    final Person person = personDao.getPersonById(personId);

    notFoundIfNull(person);

    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    rules.checkIfPermitted(person);

    render("@edit", person, date);
  }

  /**
   * Modifica timbratura dall'amministratore.
   *
   * @param stampingId ID timbratura
   */
  public static void edit(Long stampingId) {

    final Stamping stamping = stampingDao.getStampingById(stampingId);

    notFoundIfNull(stamping);

    rules.checkIfPermitted(stamping);

    final List<HistoryValue<Stamping>> historyStamping = stampingsHistoryDao
        .stampings(stamping.id);

    final Person person = stamping.personDay.person;
    final LocalDate date = stamping.personDay.date;

    render(stamping, person, date, historyStamping);
  }

  /**
   * Salva timbratura.
   *
   * @param personId id persona
   * @param date     data
   * @param stamping timbratura
   * @param time     orario
   */
  public static void save(Long personId, LocalDate date, @Valid Stamping stamping,
      @CheckWith(StringIsTime.class) String time) {

    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    final PersonDay personDay = personDayDao.getOrBuildPersonDay(person, date);

    // server per poter discriminare dopo aver fatto la save della timbratura se si
    // trattava di una nuova timbratura o di una modifica
    boolean newInsert = false;
    if (!stamping.isPersistent()) {
      newInsert = true;
      stamping.personDay = personDay;
    }

    if (Validation.hasErrors()) {
      response.status = 400;

      log.info(validation.errorsMap().toString());
      List<HistoryValue<Stamping>> historyStamping = Lists.newArrayList();
      if (stamping.isPersistent()) {
        historyStamping = stampingsHistoryDao.stampings(stamping.id);
      }

      render("@edit", stamping, person, date, time, historyStamping);
    }

    stamping.date = stampingManager.deparseStampingDateTime(date, time);

    rules.checkIfPermitted(stamping);

    final User currentUser = Security.getUser().get();

    if (!currentUser.isSystemUser()) {
      if (currentUser.hasRoles(Role.PERSONNEL_ADMIN)) {
        stamping.markedByEmployee = false;
        stamping.markedByAdmin = true;
      } else {
        stamping.markedByEmployee = true;
        stamping.markedByAdmin = false;
      }
    }

    personDay.stampings.add(stamping);
    personDay.save();

    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

    flash.success(Web.msgSaved(Stampings.class));

    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN) &&
        currentUser.person.id.equals(person.id)) {

      if (!(currentUser.person.office.checkConf(EpasParam.TR_AUTOCERTIFICATION, "true")
          && currentUser.person.qualification.qualification <= 3)) {
        notificationManager.notifyStamping(stamping,
            newInsert ? NotificationManager.CRUD.CREATE : NotificationManager.CRUD.UPDATE);
      }
      stampings(date.getYear(), date.getMonthOfYear());
    }

    personStamping(person.id, date.getYear(), date.getMonthOfYear());
  }


  /**
   * Elimina la timbratura.
   *
   * @param id timbratura
   */
  public static void delete(Long id) {

    final Stamping stamping = stampingDao.getStampingById(id);

    notFoundIfNull(stamping);

    final User currentUser = Security.getUser().orNull();

    rules.checkIfPermitted(stamping);

    final PersonDay personDay = stamping.personDay;
    stamping.delete();

    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

    flash.success("Timbratura rimossa correttamente.");

    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN) &&
        currentUser.person.id.equals(personDay.person.id)) {

      if (!(currentUser.person.office.checkConf(EpasParam.TR_AUTOCERTIFICATION, "true")
          && currentUser.person.qualification.qualification <= 3)) {
        notificationManager.notifyStamping(stamping, NotificationManager.CRUD.DELETE);
      }

      Stampings.stampings(personDay.date.getYear(), personDay.date.getMonthOfYear());
    }

    personStamping(personDay.person.id, personDay.date.getYear(),
        personDay.date.getMonthOfYear());
  }

  /**
   * L'impiegato può impostare la causale.
   *
   * @param stampingId timbratura
   * @param note       note
   */
  public static void updateEmployee(Long stampingId,
      @As(binder = NullStringBinder.class) String note) {

    Stamping stamp = stampingDao.getStampingById(stampingId);
    if (stamp == null) {
      notFound();
    }

    rules.checkIfPermitted(stamp.personDay.person);

    stamp.note = note;

    stamp.markedByEmployee = true;

    stamp.save();

    consistencyManager.updatePersonSituation(stamp.personDay.person.id, stamp.personDay.date);

    flash.success("Aggiornata timbratura del %s di %s.", stamp.date.toString("dd/MM/YYYY"),
        stamp.personDay.person.fullName());

    Stampings.stampings(stamp.personDay.date.getYear(), stamp.personDay.date.getMonthOfYear());
  }

  /**
   * Timbrature mancanti per le persone attive della sede.
   *
   * @param year     anno
   * @param month    mese
   * @param officeId sede
   */
  public static void missingStamping(final int year, final int month, final Long officeId) {

    Set<Office> offices = secureManager
        .officesReadAllowed(Security.getUser().get());
    if (offices.isEmpty()) {
      forbidden();
    }
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = new LocalDate(year, month, 1)
        .dayOfMonth().withMaximumValue();

    List<Person> activePersons = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false, monthBegin, monthEnd, true)
        .list();

    List<PersonTroublesInMonthRecap> missingStampings =
        Lists.newArrayList();

    for (Person person : activePersons) {

      PersonTroublesInMonthRecap pt = personTroubleRecapFactory
          .create(person, monthBegin, monthEnd);
      missingStampings.add(pt);
    }
    render(month, year, office, offices, missingStampings);
  }

  /**
   * Presenza giornaliera dei dipendenti visibili all'amministratore.
   *
   * @param year     anno
   * @param month    mese
   * @param day      giorno
   * @param officeId sede
   */
  public static void dailyPresence(final Integer year, final Integer month,
      final Integer day, final Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate date = new LocalDate(year, month, day);

    List<Person> activePersonsInDay = personDao.list(
        Optional.<String>absent(), Sets.newHashSet(office),
        false, date, date, true).list();

    int numberOfInOut = stampingManager
        .maxNumberOfStampingsInMonth(date, activePersonsInDay);

    List<PersonStampingDayRecap> daysRecap = Lists.newArrayList();

    daysRecap = stampingManager.populatePersonStampingDayRecapList(
        activePersonsInDay, date, numberOfInOut);

    render(daysRecap, office, date, numberOfInOut);
  }

  /**
   * La presenza giornaliera del responsabile gruppo.
   *
   * @param year  anno
   * @param month mese
   * @param day   giorno
   */
  public static void dailyPresenceForPersonInCharge(Integer year, Integer month, Integer day) {

    LocalDate date = new LocalDate(year, month, day);

    final User user = Security.getUser().get();

    List<Person> people = user.person.people;
    int numberOfInOut = stampingManager.maxNumberOfStampingsInMonth(date, people);

    List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();

    daysRecap = stampingManager.populatePersonStampingDayRecapList(people, date, numberOfInOut);

    //Per dire al template generico di non visualizzare i link di modifica e la tab di controllo
    boolean showLink = false;
    boolean groupView = true;

    render("@dailyPresence", date, numberOfInOut, showLink, daysRecap, groupView);
  }

  /**
   * La presenza festiva nell'anno.
   *
   * @param year anno
   */
  public static void holidaySituation(int year) {

    List<Person> simplePersonList = personDao.list(
        Optional.<String>absent(),
        secureManager.officesReadAllowed(Security.getUser().get()),
        false, new LocalDate(year, 1, 1),
        new LocalDate(year, 12, 31), false).list();

    List<IWrapperPerson> personList = FluentIterable
        .from(simplePersonList)
        .transform(wrapperFunctionFactory.person()).toList();
    render(personList, year);
  }

  /**
   * La presenza festiva della persona nell'anno.
   *
   * @param personId persona
   * @param year     anno
   */
  public static void personHolidaySituation(Long personId, int year) {

    Person per = personDao.getPersonById(personId);
    Preconditions.checkNotNull(per);

    rules.checkIfPermitted(per.office);

    IWrapperPerson person = wrapperFactory.create(per);

    render(person, year);
  }


  /**
   * Abilita / disabilita l'orario festivo.
   *
   * @param personDayId giorno
   */
  public static void toggleWorkingHoliday(Long personDayId) {

    PersonDay pd = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(pd);
    Preconditions.checkNotNull(pd.isPersistent());
    Preconditions.checkState(pd.isHoliday && pd.timeAtWork > 0);

    rules.checkIfPermitted(pd.person.office);

    pd.acceptedHolidayWorkingTime = !pd.acceptedHolidayWorkingTime;
    if (!pd.acceptedHolidayWorkingTime) {
      pd.isTicketForcedByAdmin = false;
    }
    pd.save();

    consistencyManager.updatePersonSituation(pd.person.id, pd.date);

    flash.success("Operazione completata. Per concludere l'operazione di ricalcolo "
        + "sui mesi successivi o sui riepiloghi mensili potrebbero occorrere alcuni secondi. "
        + "Ricaricare la pagina.");

    Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
  }

  public static void forceMealTicket(Long personDayId, boolean confirmed,
      MealTicketDecision mealTicketDecision) {

    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());

    rules.checkIfPermitted(personDay.person.office);

    if (!confirmed) {
      confirmed = true;

      mealTicketDecision = MealTicketDecision.COMPUTED;

      if (personDay.isTicketForcedByAdmin) {
        if (personDay.isTicketAvailable) {
          mealTicketDecision = MealTicketDecision.FORCED_TRUE;
        } else {
          mealTicketDecision = MealTicketDecision.FORCED_FALSE;
        }
      }

      render(personDay, confirmed, mealTicketDecision);
    }

    if (mealTicketDecision.equals(MealTicketDecision.COMPUTED)) {
      personDay.isTicketForcedByAdmin = false;
    } else {
      personDay.isTicketForcedByAdmin = true;
      if (mealTicketDecision.equals(MealTicketDecision.FORCED_FALSE)) {
        personDay.isTicketAvailable = false;
      }
      if (mealTicketDecision.equals(MealTicketDecision.FORCED_TRUE)) {
        personDay.isTicketAvailable = true;
      }
    }

    personDay.save();
    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

    flash.success("Buono Pasto impostato correttamente.");

    Stampings.personStamping(personDay.person.id, personDay.date.getYear(),
        personDay.date.getMonthOfYear());

  }

  public enum MealTicketDecision {
    COMPUTED, FORCED_TRUE, FORCED_FALSE;
  }
}

