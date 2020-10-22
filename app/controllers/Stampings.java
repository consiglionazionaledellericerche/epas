package controllers;

import static play.modules.pdf.PDF.renderPDF;

//import com.beust.jcommander.Strings;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dao.GroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.RoleDao;
import dao.StampingDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import helpers.Web;
import helpers.validators.StringIsTime;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.NullStringBinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.ConsistencyManager;
import manager.NotificationManager;
import manager.PersonDayManager;
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
import models.UsersRolesOffices;
import models.enumerate.StampTypes;
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


/**
 * Controller per la gestione delle timbrature.
 *
 * @author alessandro
 */
@Slf4j
@With({Resecure.class})
public class Stampings extends Controller {

  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static StampingManager stampingManager;
  @Inject
  static StampingDao stampingDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static SecureManager secureManager;
  @Inject
  static PersonTroublesInMonthRecapFactory personTroubleRecapFactory;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static StampingHistoryDao stampingsHistoryDao;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static PersonDayManager personDayManager;
  @Inject
  static NotificationManager notificationManager;
  @Inject
  static UserDao userDao;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  static RoleDao roleDao;
  @Inject
  static GroupDao groupDao;


  /**
   * Tabellone timbrature dipendente.
   *
   * @param year  anno
   * @param month mese
   */
  public static void stampings(final Integer year, final Integer month) {

    if (year == null || month == null) {
      stampings(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
    }
    val currentPerson = Security.getUser().get().person;
    //Accesso da utente di sistema senza persona associata
    if (currentPerson == null) {
      Application.index();
    }
    
    IWrapperPerson wrperson = wrapperFactory.create(currentPerson);

    if (!wrperson.isActiveInMonth(new YearMonth(year, month))) {
      flash.error("Non esiste situazione mensile per il mese di %s %s",
          DateUtility.fromIntToStringMonth(month), year);

      YearMonth last = wrperson.getLastActiveMonth();
      stampings(last.getYear(), last.getMonthOfYear());
    }

    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrperson.getValue(), year, month, true);

    Person person = wrperson.getValue();

    // Questo mi serve per poter fare le verifiche tramite le drools per l'inserimento timbrature in
    // un determinato mese
    final YearMonth yearMonth = new YearMonth(year, month);

    render("@personStamping", psDto, person, yearMonth);
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
    
    // Questo mi serve per poter fare le verifiche tramite le drools per l'inserimento timbrature in
    // un determinato mese
    final YearMonth yearMonth = new YearMonth(year, month);

    render(psDto, person, yearMonth);
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
   * Restituisce la form compilata secondo la modalità di chi fa la richiesta.
   * @param personId l'id della persona
   * @param date la data in cui si vuole inserire la timbratura
   */
  public static void insert(Long personId, LocalDate date) {
    
    final Person person = personDao.getPersonById(personId);

    notFoundIfNull(person);

    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    rules.checkIfPermitted(person);
    
    List<StampTypes> offsite = Lists.newArrayList();
    offsite.add(StampTypes.LAVORO_FUORI_SEDE);
    boolean insertOffsite = false;
    boolean insertNormal = true;
    boolean autocertification = false;
    
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      render(person, date, offsite, insertOffsite, insertNormal, autocertification);
    }
    IWrapperPerson wrperson = wrapperFactory
        .create(Security.getUser().get().person);
    if (user.person != null && user.person.equals(person)) {
      if (UserDao.getAllowedStampTypes(user).contains(StampTypes.LAVORO_FUORI_SEDE)) {
        insertOffsite = true;
        insertNormal = false;        
      }
    }
    
    if (user.person != null && user.person.equals(person) 
        && !wrperson.isTechnician()) {
      if (person.office.checkConf(EpasParam.TR_AUTOCERTIFICATION, "true")) {
        autocertification = true;
      }
    }
    
    if (autocertification == true  && insertOffsite == true) {
      insertOffsite = false;
    }
    render(person, date, offsite, insertOffsite, insertNormal, autocertification);
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
    if (stamping.isOffSiteWork()) {
      render("@editOffSite", stamping, person, date, historyStamping);
    }

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
  public static void save(Long personId, @Required LocalDate date, @Required Stamping stamping,
      @Required @CheckWith(StringIsTime.class) String time) {

    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    
    if (stamping.way == null) {
      Validation.addError("stamping.way", "Obbligatorio");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      
      List<HistoryValue<Stamping>> historyStamping = Lists.newArrayList();
      if (stamping.isPersistent()) {
        historyStamping = stampingsHistoryDao.stampings(stamping.id);
      }

      render("@edit", stamping, person, date, time, historyStamping);
    }
    stamping.date = stampingManager.deparseStampingDateTime(date, time);

    // serve per poter discriminare dopo aver fatto la save della timbratura se si
    // trattava di una nuova timbratura o di una modifica
    boolean newInsert = !stamping.isPersistent();

    // Se si tratta di un update ha già tutti i riferimenti al personday
    if (newInsert) {
      final PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person, date);
      stamping.personDay = personDay;
      // non è usato il costruttore con la add, quindi aggiungiamo qui a mano:
      personDay.stampings.add(stamping);
    }
        
    rules.checkIfPermitted(stamping);
    final User currentUser = Security.getUser().get();
    String result = stampingManager
        .persistStamping(stamping, date, time, person, currentUser, newInsert);
    if (!Strings.isNullOrEmpty(result)) {
      flash.error(result);
    } else {
      flash.success(Web.msgSaved(Stampings.class));
    }
    
    
    //redirection stuff
    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)
        && currentUser.person.id.equals(person.id)) {
      stampings(date.getYear(), date.getMonthOfYear());
    }
    personStamping(person.id, date.getYear(), date.getMonthOfYear());
  }
  
  /**
   * Metodo che permette il salvataggio della timbratura per lavoro fuori sede.
   * @param personId l'id della persona
   * @param date la data per cui si vuole salvare la timbratura
   * @param stamping la timbratura da salvare
   * @param time l'orario della timbratura
   */
  public static void saveOffSite(Long personId, @Required LocalDate date, 
      @Required Stamping stamping, @Required @CheckWith(StringIsTime.class) String time) {
    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    
    if (stamping.way == null) {
      Validation.addError("stamping.way", "Obbligatorio");
    }
    if (Strings.isNullOrEmpty(stamping.reason)) {
      Validation.addError("stamping.reason", "Obbligatorio");
    }
    if (Strings.isNullOrEmpty(stamping.place)) {
      Validation.addError("stamping.place", "Obbligatorio");
    }
    stamping.date = stampingManager.deparseStampingDateTime(date, time);
    val validationResult = validation.valid(stamping);
    if (!validationResult.ok) {
      response.status = 400;     
      List<StampTypes> offsite = Lists.newArrayList();
      offsite.add(StampTypes.LAVORO_FUORI_SEDE);
      boolean disableInsert = false;
      User user = Security.getUser().get();
      if (user.person != null) {
        if (person.office.checkConf(EpasParam.WORKING_OFF_SITE, "true") 
            && person.checkConf(EpasParam.OFF_SITE_STAMPING, "true")) {
          disableInsert = true;
        }
      }
      render("@insert", stamping, person, date, time, disableInsert, offsite);
    }
    
    // serve per poter discriminare dopo aver fatto la save della timbratura se si
    // trattava di una nuova timbratura o di una modifica
    boolean newInsert = !stamping.isPersistent();

    // Se si tratta di un update ha già tutti i riferimenti al personday
    if (newInsert) {
      final PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person, date);
      stamping.personDay = personDay;
      // non è usato il costruttore con la add, quindi aggiungiamo qui a mano:
      personDay.stampings.add(stamping);
    }
    log.debug("inizio salvataggio della timbratura fuori sede, person = {}", person);
    rules.checkIfPermitted(stamping);
    log.debug("dopo permessi -> salvataggio della timbratura fuori sede, person = {}", person);
    
    final User currentUser = Security.getUser().get();
    
    String result = stampingManager
        .persistStamping(stamping, date, time, person, currentUser, newInsert);
    if (!Strings.isNullOrEmpty(result)) {
      flash.error(result);
    } else {
      flash.success(Web.msgSaved(Stampings.class));
    }
    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)
        && currentUser.person.id.equals(person.id)) {
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

    notificationManager.notificationStampingPolicy(currentUser, stamping, false, false, true);
    
    //redirection stuff
    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)
        && currentUser.person.id.equals(personDay.person.id)) {
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
   * Lista delle timbrature inserite in un mese dall'amministratore.
   * 
   * @param year anno 
   * @param month mese 
   * @param officeId ufficio di riferimento
   */
  public static void stampingsByAdmin(
      final int year, final int month, final Long officeId, boolean pdf) {
    Set<Office> offices = secureManager
        .officesReadAllowed(Security.getUser().get());
    if (offices.isEmpty()) {
      forbidden();
    }
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    List<Stamping> stampingsByAdmin = stampingDao.adminStamping(new YearMonth(year, month), office);
     
    if (pdf) {
      renderPDF("/Stampings/stampingsByAdminPDF.html", 
          month, year, office, offices, stampingsByAdmin);
    } else {
      render(month, year, office, offices, stampingsByAdmin);
    }
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

    notFoundIfNull(officeId);
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate date;
    if (year == null || month == null || day == null) {
      date = LocalDate.now();
    } else {
      date = new LocalDate(year, month, day);
    }

    List<Person> activePersonsInDay = personDao.list(
        Optional.<String>absent(), Sets.newHashSet(office),
        false, date, date, true).list();

    int numberOfInOut = stampingManager
        .maxNumberOfStampingsInMonth(date, activePersonsInDay);

    List<PersonStampingDayRecap> daysRecap = Lists.newArrayList();

    daysRecap = stampingManager.populatePersonStampingDayRecapList(
        activePersonsInDay, date, numberOfInOut);
    
    Map<String, Integer> map = stampingManager.createDailyMap(daysRecap);

    render(daysRecap, office, date, numberOfInOut, map);
  }

  /**
   * La presenza giornaliera del responsabile gruppo.
   * TODO: da rivedere con la nuova implementazione dei gruppi   
   * @param year  anno
   * @param month mese
   * @param day   giorno
   */
  public static void dailyPresenceForPersonInCharge(Integer year, Integer month, Integer day) {

    LocalDate date = new LocalDate(year, month, day);

    final User user = Security.getUser().get();
    Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    Optional<UsersRolesOffices> uro = uroDao.getUsersRolesOffices(user, role, user.person.office);
    List<Person> people = Lists.newArrayList();
    if (uro.isPresent()) {
      
      people = groupDao.groupsByManager(Optional.fromNullable(user.person))
          .stream().flatMap(g -> g.getPeople().stream().distinct()).collect(Collectors.toList()); 
    } else {
      flash.error("{} non sono presenti gruppi associati alla tua persona. "
          + "Rivolgiti all'amministratore del personale", user.person.fullName());
      stampings(year, month);
    }
    
    int numberOfInOut = stampingManager.maxNumberOfStampingsInMonth(date, people);

    List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();

    daysRecap = stampingManager.populatePersonStampingDayRecapList(people, date, numberOfInOut);
    Office office = user.person.office;
    Map<String, Integer> map = stampingManager.createDailyMap(daysRecap);
    //Per dire al template generico di non visualizzare i link di modifica e la tab di controllo
    boolean showLink = false;
    boolean groupView = true;

    render("@dailyPresence", date, numberOfInOut, showLink, daysRecap, groupView, office, map);
  }
  
 

}

