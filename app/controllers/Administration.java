package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import it.cnr.iit.epas.CompetenceUtility;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.CompetenceManager;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.EmailManager;
import manager.PersonDayInTroubleManager;
import manager.PersonDayManager;
import manager.SecureManager;
import manager.UserManager;
import manager.attestati.dto.internal.clean.ContrattoAttestati;
import manager.attestati.service.CertificationService;

import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Role;
import models.Stamping;
import models.User;
import models.UsersRolesOffices;

import org.apache.commons.lang.WordUtils;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.Play;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;

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
  static WrapperModelFunctionFactory wrapperFunctionFactory;
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
  @Inject
  static CompetenceManager competenceManager;
  @Inject
  static CertificationService certService;
  @Inject
  static ContractManager contractManager;


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
    consistencyManager.fixPersonSituation(optPerson, Security.getUser(), date, onlyRecap);

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

    if (Validation.hasErrors()) {
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
          .getActiveContractsInPeriod(begin, Optional.fromNullable(end), Optional.absent());
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
        personDayManager.setValidPairStampings(pd.stampings);

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

  /**
   * Riformatta nome e cognome di tutte le persone in minuscolo con la prima lettera maiuscola
   * (Es. Mario Rossi).
   */
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

  /**
   * Mostra tutti i parametri di configurazione del play.
   */
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

  /**
   * Render del modale per l'aggiunta di un nuovo parametro di configurazione.
   */
  public static void addConfiguration() {
    render();
  }

  /**
   * @param name     Nome del parametro
   * @param value    Valore del parametro
   * @param newParam booleano che discrimina un nuovo inserimento da una modifica.
   */
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

  /**
   * Sostituisce il dominio email di tutte le persone dell'ufficio specificato.
   *
   * @param office   Ufficio interessato.
   * @param domain   nuovo dominio
   * @param sendMail booleano per effettuare l'invio email d'avviso di creazione delle persone.
   */
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

  /**
   * renderizza gli indirizzi email di tutti gli amministratori sul sistema.
   */
  public static void administratorsEmails() {

    List<UsersRolesOffices> uros = UsersRolesOffices.findAll();

    List<String> emails = uros.stream().filter(uro ->
        uro.role.name.equals(Role.PERSONNEL_ADMIN) && uro.user.person != null)
        .map(uro -> uro.user.person.email).distinct().collect(Collectors.toList());

    renderText(emails);

  }
  
  public static void applyBonus(Office office, CompetenceCode code, int year, int month) {
    
    Optional<Office> optOffice = Optional.<Office>absent();
    if (office.isPersistent()) {
      optOffice = Optional.fromNullable(office);
    }
    YearMonth yearMonth = new YearMonth(year, month);
    competenceManager.applyBonus(optOffice, code, yearMonth);
    //consistencyManager.fixPersonSituation(optPerson, Security.getUser(), date, onlyRecap);

    flash.success("Esecuzione terminata");

    utilities();
  }
  
  /**
   * Import data fine contratti a tempo determinato da attestati.
   * Imposta la data fine per i soli contratti attivi epas:
   * - con stessa data inizio
   * - con data fine nulla
   * - segnalati come temporary
   * @param office sede
   */
  public static void importCertificationContracts(Office office) {
    
    notFoundIfNull(office);
    
    Map<Integer, ContrattoAttestati> contrattiAttestati = null;

    //Doppio tentativo (mese corrente e mese precedente)
    try { 
      contrattiAttestati = certService.getCertificationContracts(office, 
          LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
    } catch (Exception ex) {
      log.info("Impossibile scaricare i contratti stralcio mese attuale");
    }
    try { 
      if (contrattiAttestati == null || contrattiAttestati.isEmpty()) {
        contrattiAttestati = certService.getCertificationContracts(office, 
            LocalDate.now().getYear(), LocalDate.now().getMonthOfYear() - 1);
      }
    } catch (Exception ex) {
      log.info("Impossibile scaricare i contratti stralcio mese precedente");
    }
    try { 
      if (contrattiAttestati == null || contrattiAttestati.isEmpty()) {
        contrattiAttestati = certService.getCertificationContracts(office, 
            LocalDate.now().getYear(), LocalDate.now().getMonthOfYear() - 2);
      }
    } catch (Exception ex2) {
      log.info("Impossibile scaricare i contratti stralcio due mesi precedenti");
    }

    if (contrattiAttestati == null || contrattiAttestati.isEmpty()) {
      flash.error("Impossibile prelvare l'informazione sulla data presunta fine contratti.");
      utilities();
    }
    
    int defined = 0;
    int terminatedInactive = 0;
    int terminatedNewContract = 0;
    int updateOldContract = 0;
    
    //Sistemo i determinati senza data fine
    for (ContrattoAttestati contrattoAttestati : contrattiAttestati.values()) {
      if (contrattoAttestati.endContract == null) {
        continue;
      }
      Person person = personDao.getPersonByNumber(contrattoAttestati.matricola);
      if (person == null) {
        continue;
      }
      log.info("{}", person.fullName());
      Optional<Contract> currentContract = wrapperFactory.create(person).getCurrentContract();
      if (!currentContract.isPresent() || currentContract.get().endDate != null) {
        continue;
      }
      if (!currentContract.get().beginDate.equals(contrattoAttestati.beginContract)) {
        continue;
      }
      if (currentContract.get().isTemporaryMissing) {
        log.info("******************** contratto attivo {} è stato determinato", person.fullName());
        currentContract.get().endDate = contrattoAttestati.endContract;
        contractManager.properContractUpdate(currentContract.get(), null, false);
        defined++;
      }
    }

    //Disabilito quelli non più appartenenti alla sede
    for (IWrapperPerson wrPerson : FluentIterable.from(personDao.listFetched(Optional.absent(),
        ImmutableSet.of(office), false, null, null, false).list())
        .transform(wrapperFunctionFactory.person()).toList()) {

      if (!wrPerson.getCurrentContract().isPresent()) {
        continue;
      }

      //non più appartenenti (ex. David Rossi)
      if (contrattiAttestati.get(wrPerson.getValue().number) == null) {
        log.info("************* contratto attivo {} è stato terminato (dipendente non più in sede)",
            wrPerson.getValue().fullName());
        wrPerson.getCurrentContract().get().endDate = LocalDate.now().minusDays(1);
        wrPerson.getCurrentContract().get().endContract = LocalDate.now().minusDays(1);
        contractManager.properContractUpdate(wrPerson.getCurrentContract().get(), null, true);
        terminatedInactive++;
        continue;
      }
    }
    
    //Disabilito i contratti scaduti (nuovo contratto presente)
    for (Person person : FluentIterable.from(personDao.listFetched(Optional.absent(),
        ImmutableSet.of(office), false, null, null, false).list())) {

      JPAPlugin.closeTx(false);
      JPAPlugin.startTx(false);      
      
      person = personDao.getPersonById(person.id);
      IWrapperPerson wrPerson = wrapperFactory.create(person);
      if (!wrPerson.getCurrentContract().isPresent()) {
        continue;
      }

      //non più appartenenti (ex. David Rossi)
      if (contrattiAttestati.get(wrPerson.getValue().number) == null) {
        continue;
      }

      Contract contract = wrPerson.getCurrentContract().get();
      ContrattoAttestati contrattoAttestati = contrattiAttestati.get(wrPerson.getValue().number);
      
      //contratto attestati iniziato dopo di quello attivo epas (chiudere)
      if (contrattoAttestati.beginContract.isAfter(contract.beginDate)) {
        contract.endContract = contrattoAttestati.beginContract.minusDays(1);
        contract.endDate = contrattoAttestati.beginContract.minusDays(1);
        contractManager.properContractUpdate(contract, null, true);
        log.info("******** contratto attivo {} è stato terminato (perchè attivato altro contratto)",
            wrPerson.getValue().fullName());
        terminatedNewContract++;
        continue;
      }
      
      //contratto attestati iniziato prima di quello attivo epas  (update contract)
      if (contrattoAttestati.beginContract.isBefore(contract.beginDate)) {
        contract.beginDate = contrattoAttestati.beginContract;
        contract.endDate = contrattoAttestati.endContract;
        contractManager.properContractUpdate(contract, null, true);
        log.info("******* contratto attivo {} è stato aggiornato (perchè attestati iniziava prec.)",
            wrPerson.getValue().fullName());
        updateOldContract++;
      }
      
    }
    
    JPA.em().flush();
    
    //Creare i nuovi
    for (ContrattoAttestati contrattoAttestati : contrattiAttestati.values()) {
      
      Person person = personDao.getPersonByNumber(contrattoAttestati.matricola);
      if (person == null) {
        continue;
      }
      IWrapperPerson wrPerson = wrapperFactory.create(person);
      if (wrPerson.getCurrentContract().isPresent()) {
        continue;
      }
      
      Contract contract = new Contract();
      contract.person = person;
      contract.beginDate = contrattoAttestati.beginContract;
      contract.endDate = contrattoAttestati.endContract;
      contractManager.properContractCreate(contract, Optional.absent(), true);
      
    }

    flash.success("Sono stati definiti %s tempi determinati, sono "
        + "state disattivate %s persone perchè non più appartenenti alla sede, "
        + "sono state terminate %s persone perchè hanno un contratto più recente su attestati, "
        + "sono stati aggiornati %s contratti perchè iniziati successivamente ad attestati.", 
        defined, terminatedInactive, terminatedNewContract, updateOldContract);
    
    utilities();
  }

}
