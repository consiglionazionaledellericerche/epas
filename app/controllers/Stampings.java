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

//import com.beust.jcommander.Strings;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import common.security.SecurityRules;
import dao.GroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonsOfficesDao;
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
import models.BadgeReader;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Role;
import models.Stamping;
import models.User;
import models.UsersRolesOffices;
import models.Zone;
import models.ZoneToZones;
import models.enumerate.StampTypes;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import play.data.binding.As;
import play.data.validation.CheckWith;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;


/**
 * Controller per la gestione delle timbrature.
 *
 * @author Alessandro Martelli
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
  @Inject
  static PersonsOfficesDao personsOfficesDao;


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
    val currentPerson = Security.getUser().get().getPerson();
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
        .create(wrperson.getValue(), year, month, true, Optional.absent());

    Person person = wrperson.getValue();

    // Questo mi serve per poter fare le verifiche tramite le drools per l'inserimento timbrature in
    // un determinato mese
    final YearMonth yearMonth = new YearMonth(year, month);

    val currentUser = currentPerson.getUser();
    val canEditAllStampingsAndAbsences = 
        currentUser.hasAccountRoles(models.enumerate.AccountRole.ADMIN, models.enumerate.AccountRole.DEVELOPER) 
          || currentUser.hasRoles(models.Role.PERSONNEL_ADMIN);
    val canInsertAbsences = 
        rules.check("AbsenceGroups.insert", yearMonth) && rules.check("AbsenceGroups.insert", person);
    
    val canInsertStampings = 
        rules.check("Stampings.insert", yearMonth) && rules.check("Stampings.insert", person);
    val canViewPersonDayHistory = rules.check("PersonDays.personDayHistory", person.getOffice(new LocalDate(year,month,1)).get());

    render("@personStamping", psDto, person, yearMonth, 
        canEditAllStampingsAndAbsences, canInsertAbsences, canInsertStampings, canViewPersonDayHistory);
  }


  /**
   * Tabellone timbrature amministratore.
   *
   * @param personId dipendente
   * @param year     anno
   * @param month    mese
   */
  public static void personStamping(final Long personId, int year, int month) {

    if (personId == null) {
      flash.error(
          "Dipendente di cui mostrare le timbrare non selezionato correttamente.");
      flash.keep();
      log.info(
          "personStamping -> personId è null, re-indirizzati verso le timbrature utente corrente");
      personStamping(Security.getUser().get().getPerson().id, year, month);
    }

    Person person = personDao.getPersonById(personId);
    Preconditions.checkNotNull(person);

    if (personsOfficesDao.monthlyAffiliations(person, year, month).size() > 1) {
      if (person.getCurrentOffice().get() != Security.getUser().get().getPerson().getCurrentOffice().get()) {
        rules.checkIfPermitted(person.getOffice(new LocalDate(year, month, 1)).get());
      }
    } else {
      rules.checkIfPermitted(person.getCurrentOffice().get());
    }
    

    val yearMonth = new YearMonth(
        year != 0 ? year : YearMonth.now().getYear(),
            month != 0 ? month : YearMonth.now().getMonthOfYear());

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    if (!wrPerson.isActiveInMonth(yearMonth)) {

      flash.error("Non esiste situazione mensile per il mese di %s",
          person.fullName(), DateUtility.fromIntToStringMonth(yearMonth.getMonthOfYear()));

      YearMonth last = wrapperFactory.create(person).getLastActiveMonth();
      personStamping(personId, last.getYear(), last.getMonthOfYear());
    }
    Optional<Office> officeOwner = Security.getUser().get().getPerson() != null 
        ? Security.getUser().get().getPerson().getCurrentOffice() : Optional.absent();
    PersonStampingRecap psDto = 
        stampingsRecapFactory.create(
            person, yearMonth.getYear(), yearMonth.getMonthOfYear(), true, officeOwner);

    val currentUser = Security.getUser();
    val canEditAllStampingsAndAbsences = 
        currentUser.isPresent() &&
        (currentUser.get().hasAccountRoles(models.enumerate.AccountRole.ADMIN, models.enumerate.AccountRole.DEVELOPER) 
          || currentUser.get().hasRoles(models.Role.PERSONNEL_ADMIN));
    val canInsertAbsences = 
        rules.check("AbsenceGroups.insert", yearMonth) && rules.check("AbsenceGroups.insert", person);
    val canInsertStampings = 
        rules.check("Stampings.insert", yearMonth) && rules.check("Stampings.insert", person);
    val canViewPersonDayHistory = rules.check("PersonDays.personDayHistory", person.getOffice(new LocalDate(year, month, 1)).get());

    render(psDto, person, yearMonth, 
        canEditAllStampingsAndAbsences, canInsertAbsences, canInsertStampings, canViewPersonDayHistory);
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
   *
   * @param personId l'id della persona
   * @param date la data in cui si vuole inserire la timbratura
   */
  public static void insert(Long personId, LocalDate date) {
    notFoundIfNull(personId);
    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    Preconditions.checkNotNull(date);
    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    rules.checkIfPermitted(person);

    List<StampTypes> offsite = Lists.newArrayList();
    offsite.add(StampTypes.LAVORO_FUORI_SEDE);
    boolean insertOffsite = false;
    boolean insertNormal = true;
    boolean autocertification = false;
    
    List<BadgeReader> badgeReaders = person.getBadges()
        .stream().map(b -> b.getBadgeReader()).collect(Collectors.toList());
    List<Zone> zones = badgeReaders.stream()
        .flatMap(br ->  br.getZones().stream().filter(z -> z != null)).collect(Collectors.toList());

    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      render(person, date, offsite, insertOffsite, insertNormal, autocertification, zones);
    }

    IWrapperPerson wrperson = wrapperFactory
        .create(Security.getUser().get().getPerson());
    if (user.getPerson() != null && user.getPerson().equals(person)) {
      if (UserDao.getAllowedStampTypes(user).contains(StampTypes.LAVORO_FUORI_SEDE)) {
        insertOffsite = true;
        insertNormal = false;        
      }
    }

    if (user.getPerson() != null && user.getPerson().equals(person) 
        && !wrperson.isTechnician()) {
      if (person.getOffice(date).get().checkConf(EpasParam.TR_AUTOCERTIFICATION, "true")) {
        autocertification = true;
      }
    }

    if (autocertification == true  && insertOffsite == true) {
      insertOffsite = false;
    }
    render(person, date, offsite, insertOffsite, insertNormal, autocertification, zones);
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

    boolean ownStamping = false;
    final Person person = stamping.getPersonDay().getPerson();
    final LocalDate date = stamping.getPersonDay().getDate();


    if (stamping.isOffSiteWork()) {
      render("@editOffSite", stamping, person, date, historyStamping);
    }
    if (Security.getUser().isPresent() && person.equals(Security.getUser().get().getPerson())
        && !Security.getUser().get().hasRoles(Role.PERSONNEL_ADMIN)) {
      ownStamping = true;
    }
    if (stamping.isServiceReasons() && ownStamping) {
      render("@editServiceReasons", stamping, person, date, historyStamping);
    }

    List<BadgeReader> badgeReaders = person.getBadges()
        .stream().map(b -> b.getBadgeReader()).collect(Collectors.toList());

    List<Zone> zones = badgeReaders.stream()
        .flatMap(br ->  br.getZones().stream().filter(z -> z != null)).collect(Collectors.toList());

    render(stamping, person, date, historyStamping, ownStamping, zones);
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
      @Required @CheckWith(StringIsTime.class) String time, Zone zone) {

    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    if (stamping.getWay() == null) {
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
    stamping.setDate(stampingManager.deparseStampingDateTime(date, time));

    // serve per poter discriminare dopo aver fatto la save della timbratura se si
    // trattava di una nuova timbratura o di una modifica
    boolean newInsert = !stamping.isPersistent();

    // Se si tratta di un update ha già tutti i riferimenti al personday
    if (newInsert) {
      final PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person, date);
      stamping.setPersonDay(personDay);
      // non è usato il costruttore con la add, quindi aggiungiamo qui a mano:
      personDay.getStampings().add(stamping);
    }
    
    rules.checkIfPermitted(stamping);
    final User currentUser = Security.getUser().get();
    stamping.setStampingZone(zone.getName());
    String result = stampingManager
        .persistStamping(stamping, person, currentUser, newInsert, false);
    if (!Strings.isNullOrEmpty(result)) {
      flash.error(result);
    } else {
      flash.success(Web.msgSaved(Stampings.class));
    }


    //redirection stuff
    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)
        && currentUser.getPerson().id.equals(person.id)) {
      stampings(date.getYear(), date.getMonthOfYear());
    }
    personStamping(person.id, date.getYear(), date.getMonthOfYear());
  }

  /**
   * Metodo che permette il salvataggio della timbratura per lavoro fuori sede.
   *
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

    //Temporaneo per la validazione
    stamping.setDate(LocalDateTime.now());
    validation.valid(stamping);

    if (Validation.hasErrors()) {
      response.status = 400;
      List<StampTypes> offsite = Lists.newArrayList();
      offsite.add(StampTypes.LAVORO_FUORI_SEDE);
      boolean disableInsert = false;
      User user = Security.getUser().get();
      if (user.getPerson() != null) {
        if (person.getOffice(date).get().checkConf(EpasParam.WORKING_OFF_SITE, "true") 

            && person.checkConf(EpasParam.OFF_SITE_STAMPING, "true")) {
          disableInsert = true;
        }
      }
      render("@editOffSite", stamping, person, date, time, disableInsert, offsite);
    }

    stamping.setDate(stampingManager.deparseStampingDateTime(date, time));
    // serve per poter discriminare dopo aver fatto la save della timbratura se si
    // trattava di una nuova timbratura o di una modifica
    boolean newInsert = !stamping.isPersistent();

    // Se si tratta di un update ha già tutti i riferimenti al personday
    if (newInsert) {
      final PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person, date);
      stamping.setPersonDay(personDay);
      // non è usato il costruttore con la add, quindi aggiungiamo qui a mano:
      personDay.getStampings().add(stamping);
    }
    log.debug("inizio salvataggio della timbratura fuori sede, person = {}", person);
    rules.checkIfPermitted(stamping);
    log.debug("dopo permessi -> salvataggio della timbratura fuori sede, person = {}", person);

    final User currentUser = Security.getUser().get();

    String result = stampingManager
        .persistStamping(stamping, person, currentUser, newInsert, false);
    if (!Strings.isNullOrEmpty(result)) {
      flash.error(result);
    } else {
      flash.success(Web.msgSaved(Stampings.class));
    }
    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)
        && currentUser.getPerson().id.equals(person.id)) {
      stampings(date.getYear(), date.getMonthOfYear());
    }
    personStamping(person.id, date.getYear(), date.getMonthOfYear());
  }


  /**
   * Effettua il salvataggio dei soli campi reason, place e note di una
   * timbratura per motivi di servizio.
   */
  public static void saveServiceReasons(Long stampingId, Stamping stamping) {
    log.info("Inside saveServiceReasons");
    notFoundIfNull(stampingId);
    notFoundIfNull(stamping);
    val currentStamping = stampingDao.getStampingById(stampingId);
    notFoundIfNull(currentStamping);
    currentStamping.setReason(stamping.getReason());
    currentStamping.setPlace(stamping.getPlace());
    currentStamping.setNote(stamping.getNote());
    currentStamping.save();
    flash.success(Web.msgSaved(Stampings.class));
    log.info("Modificata timbratura per motivi di servizio {}", currentStamping);

    val currentUser = Security.getUser().get();
    val person = currentStamping.getPersonDay().getPerson();
    val date = currentStamping.getDate();
    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)
        && currentUser.getPerson().id.equals(person.id)) {
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

    final PersonDay personDay = stamping.getPersonDay();
    stamping.delete();

    consistencyManager.updatePersonSituation(personDay.getPerson().id, personDay.getDate());

    flash.success("Timbratura rimossa correttamente.");

    notificationManager.notificationStampingPolicy(currentUser, stamping, false, false, true);

    //redirection stuff
    if (!currentUser.isSystemUser() && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)
        && currentUser.getPerson().id.equals(personDay.getPerson().id)) {
      Stampings.stampings(personDay.getDate().getYear(), personDay.getDate().getMonthOfYear());
    }
    personStamping(personDay.getPerson().id, personDay.getDate().getYear(),
        personDay.getDate().getMonthOfYear());
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

    rules.checkIfPermitted(stamp.getPersonDay().getPerson());

    stamp.setNote(note);

    stamp.setMarkedByEmployee(true);

    stamp.save();

    consistencyManager
    .updatePersonSituation(stamp.getPersonDay().getPerson().id, stamp.getPersonDay().getDate());

    flash.success("Aggiornata timbratura del %s di %s.", stamp.getDate().toString("dd/MM/YYYY"),
        stamp.getPersonDay().getPerson().fullName());

    Stampings.stampings(stamp.getPersonDay().getDate().getYear(), 
        stamp.getPersonDay().getDate().getMonthOfYear());
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
   * TODO: da rivedere con la nuova implementazione dei gruppi.
   *
   * @param year  anno
   * @param month mese
   * @param day   giorno
   */
  public static void dailyPresenceForPersonInCharge(Integer year, Integer month, Integer day) {

    LocalDate date = new LocalDate(year, month, day);
    final User user = Security.getUser().get();
    
    if (user.getPerson() == null) {
      flash.error("%s è un'utenza di servizio non associata a nessuna persona, "
          + "non sono quindi presenti gruppi associati al tuo utente.", user.getUsername());
      render("@dailyPresence", date);
    }

    Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);

    Optional<UsersRolesOffices> uro = uroDao.getUsersRolesOffices(user, role, 
        user.getPerson().getCurrentOffice().get());

    List<Person> people = Lists.newArrayList();
    if (uro.isPresent()) {

      people = groupDao.groupsByManager(Optional.fromNullable(user.getPerson()))
          .stream().flatMap(g -> g.getPeople().stream().distinct()).collect(Collectors.toList()); 
    } else {
      flash.error("{} non sono presenti gruppi associati alla tua persona. "
          + "Rivolgiti all'amministratore del personale", user.getPerson().fullName());
      stampings(year, month);
    }

    int numberOfInOut = stampingManager.maxNumberOfStampingsInMonth(date, people);

    List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();

    daysRecap = stampingManager.populatePersonStampingDayRecapList(people, date, numberOfInOut);

    Office office = user.getPerson().getCurrentOffice().get();

    Map<String, Integer> map = stampingManager.createDailyMap(daysRecap);
    //Per dire al template generico di non visualizzare i link di modifica e la tab di controllo
    boolean showLink = false;
    boolean groupView = true;

    render("@dailyPresence", date, numberOfInOut, showLink, daysRecap, groupView, office, map);
  }

}