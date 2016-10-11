package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.CompetenceUtility;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
import manager.EmailManager;
import manager.PersonDayInTroubleManager;
import manager.PersonDayManager;
import manager.SecureManager;
import manager.UserManager;

import models.AbsenceType;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Role;
import models.Stamping;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.JustifiedTimeAtWork;

import org.apache.commons.lang.WordUtils;
import org.assertj.core.util.Strings;
import org.joda.time.LocalDate;

import play.Play;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

@Slf4j
@With({Resecure.class})
public class Administration extends Controller {

  static final String SUDO_USERNAME = "sudo.username";
  static final String USERNAME = "username";

  @Inject
  static SecureManager secureManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static CompetenceUtility competenceUtility;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static ContractDao contractDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static PersonDayManager personDayManager;
  @Inject
  static PersonDayInTroubleManager personDayInTroubleManager;
  @Inject
  static UserManager userManager;
  @Inject
  static EmailManager emailManager;

  /**
   * metodo che inizializza i codici di assenza e gli stampType presenti nel db romano.
   */
  public static void initializeRomanAbsences() {

    AbsenceType absenceType = AbsenceType.find("byCode", "PEPE").first();
    if (absenceType == null) {
      // creare le assenze romane
      absenceType = new AbsenceType();
      absenceType.code = "PEPE";
      absenceType.description = "Permesso Personale";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "RITING").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "RITING";
      absenceType.description = "AUTORIZ.DIRIG.RITARDO.INGR.TUR";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }

    absenceType = AbsenceType.find("byCode", "661h").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "661h";
      absenceType.description = "PERM.ORARIO GRAVI MOTIVI";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "09B").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "09B";
      absenceType.description = "ORE DI  MALAT. O VIS.ME";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "103").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "103";
      absenceType.description = "Telelavoro";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "91.").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "91.";
      absenceType.description = "RIPOSO COMPENSATIVO 1/3 L";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "91CE").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "91CE";
      absenceType.description = "RIP. COMP.CHIUSURA ENTE";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "182").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "182";
      absenceType.description = "PERM ASSIST.PARENTI 2";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }

  }

  /**
   * metodo che inizializza le persone della anagrafica.
   */
  public static void initializePersons() {

    //Tutte le persone con contratto iniziato dopo alla data di inizializzazione
    // devono avere la inizializzazione al giorno prima.
    List<Person> persons = Person.findAll();
    for (Person person : persons) {

      //Configurazione office
      LocalDate initUse = person.office.getBeginDate();

      //Contratto attuale
      Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();

      if (contract.isPresent()) {
        if (contract.get().sourceDateResidual == null
            && contract.get().beginDate.isBefore(initUse)) {
          Contract con = contract.get();
          con.sourceDateResidual = initUse.minusDays(1);
          con.sourcePermissionUsed = 0;
          con.sourceRecoveryDayUsed = 0;
          con.sourceRemainingMealTicket = 0;
          con.sourceRemainingMinutesCurrentYear = 6000;
          con.sourceRemainingMinutesLastYear = 0;
          con.sourceVacationCurrentYearUsed = 0;
          con.sourceVacationLastYearUsed = 0;
          con.save();
        }
      }
    }
  }

  /**
   * metodo che renderizza la pagina di utilities.
   */
  public static void utilities() {

    final List<Person> personList = personDao.list(
        Optional.<String>absent(),
        secureManager.officesWriteAllowed(Security.getUser().get()),
        false, LocalDate.now(), LocalDate.now(), true)
        .list();

    render(personList);
  }

  /**
   * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
   *
   * @param person la persona da fixare, -1 per fixare tutte le persone
   * @param year   l'anno dal quale far partire il fix
   * @param month  il mese dal quale far partire il fix
   */
  public static void fixPersonSituation(Person person, int year, int month, boolean onlyRecap) {

    LocalDate date = new LocalDate(year, month, 1);

    Optional<Person> optPerson = Optional.<Person>absent();
    if (person.isPersistent()) {
      optPerson = Optional.fromNullable(person);
    }
    consistencyManager.fixPersonSituation(optPerson, Security.getUser(), date, false, onlyRecap);

    flash.success("Esecuzione terminata");

    utilities();
  }

  /**
   * metodo che ritorna un controllo sui minuti in eccesso nella tabella delle competenze.
   */
  public static void updateExceedeMinInCompetenceTable() {
    competenceUtility.updateExceedeMinInCompetenceTable();
    renderText("OK");
  }

  /**
   * metodo che cancella tutte le timbrature disaccoppiate nell'arco temporale specificato.
   *
   * @param peopleId l'id della persona
   * @param begin    la data da cui partire
   * @param end      la data in cui finire
   * @param forAll   se il controllo deve essere fatto per tutti
   */
  public static void deleteUncoupledStampings(
      List<Long> peopleId, @Required LocalDate begin, LocalDate end, boolean forAll) {

    if (validation.hasErrors()) {
      params.flash();
      utilities();
    }

    if (end == null) {
      end = begin;
    }

    List<Person> people = Lists.newArrayList();
    if (!forAll) {
      for (Long id : peopleId) {
        people.add(personDao.getPersonById(id));
      }

    } else {
      // Tutte le persone attive nella finestra speficificata.
      List<Contract> contracts = contractDao
          .getActiveContractsInPeriod(begin, Optional.fromNullable(end));
      for (Contract contract : contracts) {
        people.add(contract.person);
      }
    }

    for (Person person : people) {


      person = Person.findById(person.id);

      log.info("Rimozione timbrature disaccoppiate per {} ...", person.fullName());
      List<PersonDay> persondays = personDayDao
          .getPersonDayInPeriod(person, begin, Optional.of(end));
      int count = 0;
      for (PersonDay pd : persondays) {
        personDayManager.setValidPairStampings(pd);

        for (Stamping stamping : pd.stampings) {
          if (!stamping.valid) {
            stamping.delete();
            count++;
          }
        }
      }

      log.info("... rimosse {} timbrature disaccoppiate.", count);
    }

    flash.success("Esecuzione terminata");

    utilities();
  }

  /**
   * Rimuove dal database tutti i personDayInTrouble che non appartengono ad alcun contratto o che
   * sono precedenti la sua inizializzazione.
   */
  @SuppressWarnings("deprecation")
  public static void fixDaysInTrouble() {

    List<Person> people = Person.findAll();
    for (Person person : people) {

      JPAPlugin.closeTx(false);
      JPAPlugin.startTx(false);
      person = personDao.getPersonById(person.id);
      personDayInTroubleManager.cleanPersonDayInTrouble(person);
    }

    flash.success("Operazione completata");

    utilities();
  }

  public static void capitalizePeople() {

    List<Person> people = Person.findAll();
    for (Person person : people) {

      person.name = WordUtils.capitalizeFully(person.name);
      person.surname = WordUtils.capitalizeFully(person.surname);

      person.save();
    }

    flash.success("Operazione completata");

    utilities();

  }

  public static void playConfiguration() {
    Set<Entry<Object, Object>> entries = Sets.newHashSet();
    Play.configuration.entrySet().forEach(e -> {
      if (!e.getKey().toString().contains("pass") && !e.getKey().toString().contains("secret")) {
        entries.add(e);
      }
    });
    render("@data", entries);
  }

  /**
   * Mostra le proprietà della jvm.
   */
  public static void jvmConfiguration() {
    final Collection<Entry<Object, Object>> entries = Collections2.filter(
        System.getProperties().entrySet(), new Predicate<Entry<Object, Object>>() {
          @Override
          public boolean apply(@Nullable Entry<Object, Object> objectObjectEntry) {
            return !"java.class.path".equals(objectObjectEntry.getKey());
          }
        });
    render("@data", entries);
  }

  /**
   * Mostra i valori di Runtime del processo play (Memoria usata, memoria libera, etc).
   */
  public static void runtimeData() {

    final int mb = 1024 * 1024;
    final Runtime runtime = Runtime.getRuntime();

    final Set<Entry<String, String>> entries = ImmutableMap.of(
        "Available Processors", String.format("%s", runtime.availableProcessors()),
        "Used Memory", String.format("%s Mb", (runtime.totalMemory() - runtime.freeMemory()) / mb),
        "Free Memory", String.format("%s Mb", runtime.freeMemory() / mb),
        "Max Memory", String.format("%s Mb", runtime.maxMemory() / mb),
        "Total Memory", String.format("%s Mb", runtime.totalMemory() / mb)).entrySet();

    render("@data", entries);
  }

  public static void addConfiguration() {
    render();
  }

  public static void saveConfiguration(@Required String name, @Required String value, 
      boolean newParam) {
    if (Validation.hasErrors()) {
      response.status = 400;
      if (newParam) {
        render("@addConfiguration", name, value);
      }
    } else {
      Play.configuration.setProperty(name, value);
      // Questo metodo viene chiamato sia tramite x-editable che tramite form nel modale
      // il booleano newParam viene usato per discriminare la provenienza della chiamata
      if (newParam) {
        flash.success("Parametro di configurazione correttamente salvato: %s=%s", name, value);
        playConfiguration();
      }
    }
  }

  /**
   * Switch in un'altro user.
   */
  public static void switchUserTo(long id) {

    final User user = Administrators.userDao.getUserByIdAndPassword(id, Optional.<String>absent());

    if (user == null || user.disabled) {
      notFound();
    }

    // salva il precedente
    session.put(SUDO_USERNAME, session.get(USERNAME));
    // recupera
    session.put(USERNAME, user.username);
    // redirect alla radice
    session.remove("officeSelected");
    redirect(Play.ctxPath + "/");
  }

  /**
   * Switch nell'user di una persona.
   */
  public static void switchUserToPersonUser(long id) {

    final Person person = Administrators.personDao.getPersonById(id);
    notFoundIfNull(person);
    Preconditions.checkNotNull(person.user);
    switchUserTo(person.user.id);
  }

  /**
   * ritorna alla precedente persona.
   */
  public static void restoreUser() {
    if (session.contains(SUDO_USERNAME)) {
      session.put(USERNAME, session.get(SUDO_USERNAME));
      session.remove(SUDO_USERNAME);
    }
    // redirect alla radice
    redirect(Play.ctxPath + "/");
  }

  public static void changePeopleEmailDomain(@Required Office office, @Required String domain,
      boolean sendMail) {

    final Pattern domainPattern = 
        Pattern.compile("(?:[\\w](?:[\\w-]*[\\w])?\\.)+[a-zA-Z0-9](?:[\\w-]*[\\w])?");

    if (!Strings.isNullOrEmpty(domain) && domain.contains("@")) {
      Validation.addError("domain", "Specificare il dominio senza il carattere @");
    }
    if (!domainPattern.matcher(domain).matches()) {
      Validation.addError("domain", "Formato non corretto");
    }
    if (Validation.hasErrors()) {
      render("@utilities", office, domain, sendMail);
    }

    final List<Person> people = personDao.byOffice(office);

    people.forEach(person -> {
      person.email = person.email.substring(0, person.email.indexOf("@") + 1) + domain;
      person.save();
      if (sendMail) {
        userManager.generateRecoveryToken(person);
        emailManager.newUserMail(person);
      }
    });

    flash.success("Indirizzo email reimpostato per %s persone dell'ufficio %s",
        people.size(), office);

    utilities();
  }
  
  
  public static void administratorsEmails() {
    
    List<UsersRolesOffices> uros = UsersRolesOffices.findAll();
    
    List<String> emails = uros.stream().filter(uro -> {
      return uro.role.name.equals(Role.PERSONNEL_ADMIN) && uro.user.person != null;
    }).map(uro -> uro.user.person.email).collect(Collectors.toList());
    
    renderText(emails);
    
  }

}
