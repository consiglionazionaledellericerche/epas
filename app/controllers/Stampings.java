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
import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.PersonTags;
import helpers.Web;
import helpers.validators.StringIsTime;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.NullStringBinder;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
import manager.SecureManager;
import manager.StampingManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.recaps.troubles.PersonTroublesInMonthRecap;
import manager.recaps.troubles.PersonTroublesInMonthRecapFactory;

import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.data.binding.As;
import play.data.validation.CheckWith;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;


/**
 * Controller per la gestione delle timbrature.
 * @author alessandro
 */
@Slf4j
@With({RequestInit.class, Resecure.class})
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

  /**
   * Tabellone timbrature dipendente.
   *
   * @param year  anno
   * @param month mese
   */
  public static void stampings(final Integer year, final Integer month) {

    IWrapperPerson person = wrapperFactory
        .create(Security.getUser().get().person);

    if (!person.isActiveInMonth(new YearMonth(year, month))) {
      flash.error("Non esiste situazione mensile per il mese di %s %s",
          DateUtility.fromIntToStringMonth(month), year);

      YearMonth last = person.getLastActiveMonth();
      stampings(last.getYear(), last.getMonthOfYear());
    }

    PersonStampingRecap psDto = stampingsRecapFactory
        .create(person.getValue(), year, month, true);
    
    //Per dire al template generico di non visualizzare i link di modifica
    boolean showLink = false;
    
    render("@personStamping", psDto, person, showLink);
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

    //Per dire al template generico di non visualizzare i link di modifica
    boolean showLink = true;
    
    render(psDto, person, showLink);
  }
  
  /**
   * Nuova timbratura inserita dall'amministratore.
   * @param person persona
   * @param date data
   */
  public static void blank(@Required Person person, @Required LocalDate date) {

    if (!person.isPersistent()) {
      notFound();
    }

    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    rules.checkIfPermitted(person.office);

    render("@edit", person, date);
  }

  /**
   * Modifica timbratura dall'amministratore.
   * @param stamping timbratura
   */
  public static void edit(@Valid Stamping stamping) {

    if (!stamping.isPersistent()) {
      notFound();
    }

    final List<HistoryValue<Stamping>> historyStamping = stampingsHistoryDao
        .stampings(stamping.id);

    rules.checkIfPermitted(stamping.personDay.person.office);

    final Person person = stamping.personDay.person;
    final LocalDate date = stamping.personDay.date;

    render(stamping, person, date, historyStamping);
  }

  /**
   * Salva timbratura.
   * @param person persona
   * @param date data
   * @param stamping timbratura
   * @param time orario
   */
  public static void save(@Required Person person, @Required LocalDate date,
                          @Valid Stamping stamping, @CheckWith(StringIsTime.class) String time) {

    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    PersonDay personDay = personDayDao.getOrBuildPersonDay(person, date);

    rules.checkIfPermitted(person.office);

    if (!stamping.isPersistent()) {
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

    personDay.save();
    stamping.date = stampingManager.deparseStampingDateTime(date, time);
    stamping.markedByAdmin = true;

    personDay.stampings.add(stamping);
    personDay.save();

    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

    flash.success(Web.msgSaved(Stampings.class));

    Stampings.personStamping(person.id,
        date.getYear(), date.getMonthOfYear());

  }

  /**
   * Elimina la timbratura.
   * @param id timbratura
   */
  public static void delete(Long id) {

    final Stamping stamping = stampingDao.getStampingById(id);

    Preconditions.checkState(stamping != null);

    rules.checkIfPermitted(stamping.personDay.person);

    final PersonDay personDay = stamping.personDay;
    stamping.delete();

    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

    flash.success("Timbratura rimossa correttamente.");

    personStamping(personDay.person.id, personDay.date.getYear(),
        personDay.date.getMonthOfYear());
  }

  /**
   * L'impiegato può impostare la causale.
   * @param stampingId timbratura
   * @param note note
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

    flash.success(
        "Timbratura per il giorno %s per %s aggiornata.",
        PersonTags.toDateTime(stamp.date.toLocalDate()), stamp.personDay.person.fullName());

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

    boolean showLink = false;
    render(daysRecap, office, date, numberOfInOut, showLink);
  }
  
  /**
   * La presenza giornaliera del responsabile gruppo.
   * @param year anno
   * @param month mese
   * @param day giorno
   */
  public static void dailyPresenceForPersonInCharge(Integer year, Integer month, Integer day) {

    if (!Security.getUser().get().person.isPersonInCharge) {
      forbidden();
    }

    LocalDate date = new LocalDate(year, month, day);

    User user = Security.getUser().get();

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
   * @param personId persona 
   * @param year anno
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
   * @param personDayId giorno
   */
  public static void toggleWorkingHoliday(Long personDayId) {

    PersonDay pd = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(pd);
    Preconditions.checkNotNull(pd.isPersistent());
    Preconditions.checkState(pd.isHoliday == true && pd.timeAtWork > 0);

    rules.checkIfPermitted(pd.person.office);

    pd.acceptedHolidayWorkingTime = !pd.acceptedHolidayWorkingTime;
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
  
  public static enum MealTicketDecision {

    COMPUTED, FORCED_TRUE, FORCED_FALSE;
  }
  

}

