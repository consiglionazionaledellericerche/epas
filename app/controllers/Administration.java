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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.CompetenceCodeDao;
import dao.ContractDao;
import dao.GeneralSettingDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.UserDao;
import dao.absences.AbsenceComponentDao;
import dao.history.HistoryValue;
import dao.history.PersonDayHistoryDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import helpers.Web;
import it.cnr.iit.epas.CompetenceUtility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
import manager.configurations.ConfigurationManager;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.GeneralSetting;
import models.Institute;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonShiftShiftType;
import models.Role;
import models.Stamping;
import models.User;
import models.UsersRolesOffices;
import models.absences.Absence;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.LimitType;
import org.apache.commons.lang.WordUtils;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.Play;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.jobs.Job;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per alcuni metodi di amministrazione dell'applicazione.
 *
 */
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
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static PersonDayHistoryDao historyDao;
  @Inject
  static UserDao userDao;
  @Inject
  static AbsenceTypeDao absenceTypeDao;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static AbsenceComponentDao absenceComponentDao;
  @Inject
  static GeneralSettingDao generalSettingDao;
  @Inject
  static CompetenceCodeDao competenceCodeDao;
  @Inject
  static OfficeDao officeDao;
 
  /**
   * metodo che renderizza la pagina di utilities senza parametri passati.
   */
  public static void utilities() {
    log.debug("Chiamata utilities senza parametri");
    final List<Person> personList = personDao.list(
        Optional.<String>absent(),
        secureManager.officesWriteAllowed(Security.getUser().get()),
        false, LocalDate.now(), LocalDate.now(), true)
        .list();
    val officeList = officeDao.allEnabledOffices().stream()
        .sorted((o, o1) -> o.getName().compareTo(o1.getName()))
        .collect(Collectors.toList());
    render(personList, officeList);
  }

  /**
   * metodo che renderizza la pagina di utilities.
   */
  public static void utilities(Person person, Office office, Integer year, Integer month) {
    log.debug("Chiamata utilities con parametri");
    final List<Person> personList = personDao.list(
        Optional.<String>absent(),
        secureManager.officesWriteAllowed(Security.getUser().get()),
        false, LocalDate.now(), LocalDate.now(), true)
        .list();
    val officeList = officeDao.allEnabledOffices().stream()
        .sorted((o, o1) -> o.getName().compareTo(o1.getName()))
        .collect(Collectors.toList());
    render(personList, officeList, person, office, year, month);
  }

  /**
   * ritorna la/le persona/e che corrispondono ai criteri indicati.
   */
  public static void internalSearch(String name) {


    List<Person> simplePersonList = personDao.listFetched(Optional.fromNullable(name),
        null, false, null, null, false).list();

    List<IWrapperPerson> personList = FluentIterable.from(simplePersonList)
        .transform(wrapperFunctionFactory.person()).toList();

    render(personList);


  }

  /**
   * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
   *
   * @param person la persona da fixare, -1 per fixare tutte le persone
   * @param year   l'anno dal quale far partire il fix
   * @param month  il mese dal quale far partire il fix
   */
  public static void fixPersonSituation(Person person, Office office, 
      @Required Integer year, @Required Integer month, boolean onlyRecap) {

    if (person == null && office == null || (!person.isPersistent() && !office.isPersistent())) {
      Validation.addError("person", "Obbligatorio specificare un utente o un ufficio");
      Validation.addError("office", "Obbligatorio specificare un utente o un ufficio");
    }
    
    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori evidenziati");
      log.debug("Errori di validazione in fixPersonSituation, person={}, "
          + "office={}, year={}, month={}", person, office, year, month);
      Validation.keep();
      utilities(person, office, year, month);
    }

    log.info("Richiesto ricalcolo situazione mensile di {}/{} per la persona {} o l'ufficio {}",
        month, year, person, office);

    LocalDate date = new LocalDate(year, month, 1);

    // (0) Costruisco la lista di persone su cui voglio operare
    List<Person> personList = Lists.newArrayList();

    if (person != null && person.isPersistent()) {
      personList = Lists.newArrayList(person);      
    }
    if (office != null && office.isPersistent()) {
      office = Office.findById(office.id);
      personList = personDao.getActivePersonInMonth(
          Sets.newHashSet(office), new YearMonth(date.getYear(), date.getMonthOfYear()));
    }    
    
    consistencyManager.fixPersonSituation(personList, date, onlyRecap);

    flash.success("Esecuzione avviata in background");

    utilities();
  }

  /**
   * Metodo che resetta i codici 92H.
   */
  public static void reset92H() {
    List<HistoryValue<Absence>> allAbsences = historyDao.oldMissions();

    log.debug("Trovate {} assenze totali", allAbsences.size());

    for (HistoryValue<Absence> val : allAbsences) {

      Absence abs = val.value;
      long id = val.value.getPersonDay().id;
      log.debug("Id del personDay = {}", id);
      PersonDay pd = personDayDao.getPersonDayById(id);
      if (pd != null) {
        if (pd.getAbsences().contains(abs)) {
          log.debug("l'assenza {} è già nel personday, non la inserisco", abs.id);
          continue;
        }
        pd.save();
        log.info("Assenza con revisione {}, con id {} in data {} di tipo {}", val.type.name(), 
            val.value.id, val.value.getPersonDay().getDate(), val.value.getAbsenceType().getCode());
        List<HistoryValue<Absence>> absenceDeleted = historyDao.specificAbsence(val.value.id);
        if (!absenceDeleted.isEmpty()) {

          log.debug("L'assenza è stata anche cancellata, la ricreo");

          Absence absence = new Absence();
          absence.setAbsenceType(abs.getAbsenceType());  
          JustifiedType type = 
              absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.absence_type_minutes);
          absence.setJustifiedType(type);
          absence.setJustifiedMinutes(abs.getJustifiedMinutes());
          absence.setPersonDay(pd);
          absence.save(); 
          JPA.em().flush();       

          pd.getAbsences().add(absence);
          pd.save();
          log.info("Salvo il personday"); 
        }

      }
    }

    renderText("Esecuzione terminata");
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
        people.add(contract.getPerson());
      }
    }

    for (Person person : people) {


      person = Person.findById(person.id);

      log.info("Rimozione timbrature disaccoppiate per {} ...", person.fullName());
      List<PersonDay> persondays = personDayDao
          .getPersonDayInPeriod(person, begin, Optional.of(end));
      int count = 0;
      for (PersonDay pd : persondays) {
        personDayManager.setValidPairStampings(pd.getStampings());

        for (Stamping stamping : pd.getStampings()) {
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

      person.setName(WordUtils.capitalizeFully(person.getName()));
      person.setSurname(WordUtils.capitalizeFully(person.getSurname()));

      person.save();
    }

    flash.success("Operazione completata");

    utilities();

  }

  /**
   * Mostra i parametri generali dell'applicazione.
   */
  public static void generalSetting() {
    val generalSetting = generalSettingDao.generalSetting();
    render("@data", generalSetting);
  }

  /**
   * Salvataggio delle impostazioni generali.
   *
   * @param generalSetting impostazioni generali da salvare.
   */
  public static void saveGeneralSetting(@Required @Valid GeneralSetting generalSetting) {
    if (Validation.hasErrors()) {
      render("@data", generalSetting);
    } else {
      generalSetting.save();
      generalSettingDao.generalSettingInvalidate();
      flash.success(Web.msgSaved(GeneralSetting.class));
      generalSetting();
    }
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
    val load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();

    final Set<Entry<String, String>> entrySet = ImmutableMap.of(
        "Available Processors", String.format("%s", runtime.availableProcessors()),
        "Used Memory", String.format("%s Mb", (runtime.totalMemory() - runtime.freeMemory()) / mb),
        "Free Memory", String.format("%s Mb", runtime.freeMemory() / mb),
        "Max Memory", String.format("%s Mb", runtime.maxMemory() / mb),
        "Total Memory", String.format("%s Mb", runtime.totalMemory() / mb)).entrySet();
    
    final Set<Entry<String, String>> entries = Sets.newHashSet(entrySet);
    entries.add(new SimpleEntry<String, String>("Load", String.format("%s", load)));
    render("@data", entries);
  }

  /**
   * Mostra le informazioni su thread correnti.
   */
  public static void threadsData() {
    val threads = ManagementFactory.getThreadMXBean();
    val threadsData = threads.dumpAllThreads(true, true);
    render("@threadsData", threadsData);
  }
  
  /**
   * Render del modale per l'aggiunta di un nuovo parametro di configurazione.
   */
  public static void addConfiguration() {
    render();
  }

  /**
   * Metodo che permette di salvare la configurazione.
   *
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

    if (user == null || user.isDisabled()) {
      notFound();
    }

    // salva il precedente
    session.put(SUDO_USERNAME, session.get(USERNAME));
    // recupera
    session.put(USERNAME, user.getUsername());
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
    Preconditions.checkNotNull(person.getUser());
    switchUserTo(person.getUser().id);
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
      person.setEmail(person.getEmail().substring(0, person.getEmail().indexOf("@") + 1) + domain);
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
        uro.getRole().getName().equals(Role.PERSONNEL_ADMIN) 
        && uro.getUser().getPerson() != null)
        .map(uro -> uro.getUser().getPerson().getEmail())
        .distinct().collect(Collectors.toList());

    renderText(emails);

  }

  /**
   * normalizza le date dei personReperibilities nel caso in cui ci fossero dei problemi 
   * con più date che per la stessa persona, sullo stesso tipo di reperibilità, 
   * presentano endDate = null.
   */
  public static void normalizationReperibilities() {

    Map<Person, List<PersonReperibility>> map = Maps.newHashMap();
    List<PersonReperibility> list = PersonReperibility.findAll();
    List<PersonReperibility> repList = null;
    log.info("Inizio la normalizzazione delle date...");
    log.debug("Creo la mappa persona-personreperibility");
    for (PersonReperibility pr : list) {
      if (pr.getStartDate() != null && pr.getEndDate() == null) {
        if (!map.containsKey(pr.getPerson())) {
          repList = Lists.newArrayList();                  
        } else {
          repList = map.get(pr.getPerson());                  
        }
        repList.add(pr);
        map.put(pr.getPerson(), repList);
      }      
    }
    log.debug("Valuto la mappa per controllare le date dei personreperibilities");
    for (Map.Entry<Person, List<PersonReperibility>> entry : map.entrySet()) {

      if (entry.getValue().size() > 1) {
        List<PersonReperibility> multipleReps = entry.getValue();
        log.debug("Ordino le person reperibilities");
        Collections.sort(multipleReps, PersonReperibility.PersonReperibilityComparator);       
        PersonReperibility pr = null;
        log.debug("Controllo le personreperibilities");
        for (PersonReperibility rep : multipleReps) {
          if (pr == null) {
            pr = rep;
            continue;
          }
          if (rep.getPersonReperibilityType().equals(pr.getPersonReperibilityType())) {
            log.warn("Ho due person reperibilities relativi allo stesso tipo");
            if (rep.getStartDate() != null && pr.getStartDate() != null 
                && rep.getEndDate() == null && pr.getEndDate() == null) {
              log.warn("Sono nel caso di due person reperibilities con data fine nulla "
                  + "per lo stesso tipo");
              if (rep.getStartDate().isBefore(pr.getStartDate())) {
                log.debug("Cancello quello più futuro di {} con data {}", pr.getPerson(), pr.getStartDate());
                pr.delete();                
              } else {
                log.debug("Cancello quello più futuro di {} con data {}", 
                    rep.getPerson(), rep.getStartDate());
                rep.delete();
              }
            }
          } else {
            continue;
          }
        }

      }      
    }
    log.info("Terminata esecuzione");
    renderText("Ok");
  }

  /**
   * Metodo di normalizzazione degli elementi presenti nella lista delle persone assegnate a una 
   * certa attività di turno. Rimuove tutte le occorrenze con data di inizio e fine nulle.
   */
  public static void normalizationShifts() {
    List<PersonShiftShiftType> psstList = PersonShiftShiftType.findAll();
    log.debug("Recupero tutte le associazioni tra persone e attività di turno.");
    for (PersonShiftShiftType psst : psstList) {
      if (psst.getBeginDate() == null && psst.getEndDate() == null) {
        log.debug("Rimuovo l'occorrenza di {} sull'attività {} perchè ha date nulle", 
            psst.getPersonShift().getPerson().fullName(), psst.getShiftType().getDescription());
        psst.delete();
      }
    }    
    renderText("Ok");
  }

  /**
   * Metodo che applica le competenze a presenza mensile/giornaliera a tutti gli
   * uffici (come per il BonusJob).
   *
   * @param year l'anno
   * @param month il mese
   */
  public static void applyBonusAllOffices(int year, int month) {
    YearMonth yearMonth = new YearMonth(year, month);
    List<CompetenceCode> codeList = competenceCodeDao
        .getCompetenceCodeByLimitType(LimitType.onMonthlyPresence);
    codeList.forEach(item -> {
      competenceManager.applyBonus(Optional.absent(), item, yearMonth);
    });
    codeList = competenceCodeDao.getCompetenceCodeByLimitType(LimitType.entireMonth);
    codeList.forEach(item -> {
      competenceManager.applyBonus(Optional.absent(), item, yearMonth);
    });
  }
  
  /**
   * Metodo che applica le competenze a presenza mensile/giornaliera.
   *
   * @param office la sede 
   * @param code il codice di assenza
   * @param year l'anno
   * @param month il mese
   */
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
   * - segnalati come temporary.
   *
   * @param office sede
   */
  @SuppressWarnings("deprecation")
  public static void importCertificationContracts(Office office) {

    notFoundIfNull(office);

    Map<String, ContrattoAttestati> contrattiAttestati = null;

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

      JPAPlugin.closeTx(false);
      JPAPlugin.startTx(false);     
      Person person = personDao.getPersonByNumber(contrattoAttestati.matricola);
      if (person == null) {
        continue;
      }
      log.info("{}", person.fullName());
      Optional<Contract> currentContract = wrapperFactory.create(person).getCurrentContract();
      if (!currentContract.isPresent() || currentContract.get().getEndDate() != null) {
        continue;
      }
      if (!currentContract.get().getBeginDate().equals(contrattoAttestati.beginContract)) {
        continue;
      }
      if (currentContract.get().isTemporaryMissing()) {
        log.info("******************** contratto attivo {} è stato determinato", person.fullName());
        currentContract.get().setEndDate(contrattoAttestati.endContract);
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
      if (contrattiAttestati.get(wrPerson.getValue().getNumber()) == null) {
        log.info("************* contratto attivo {} è stato terminato (dipendente non più in sede)",
            wrPerson.getValue().fullName());
        wrPerson.getCurrentContract().get().setEndDate(LocalDate.now().minusDays(1));
        wrPerson.getCurrentContract().get().setEndContract(LocalDate.now().minusDays(1));
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
      if (contrattiAttestati.get(wrPerson.getValue().getNumber()) == null) {
        continue;
      }

      Contract contract = wrPerson.getCurrentContract().get();
      ContrattoAttestati contrattoAttestati = contrattiAttestati.get(wrPerson.getValue().getNumber());

      //contratto attestati iniziato dopo di quello attivo epas (chiudere)
      if (contrattoAttestati.beginContract.isAfter(contract.getBeginDate())) {
        contract.setEndContract(contrattoAttestati.beginContract.minusDays(1));
        contract.setEndDate(contrattoAttestati.beginContract.minusDays(1));
        contractManager.properContractUpdate(contract, null, true);
        log.info("******** contratto attivo {} è stato terminato (perchè attivato altro contratto)",
            wrPerson.getValue().fullName());
        terminatedNewContract++;
        continue;
      }

      //contratto attestati iniziato prima di quello attivo epas  (update contract)
      if (contrattoAttestati.beginContract.isBefore(contract.getBeginDate())) {
        contract.setBeginDate(contrattoAttestati.beginContract);
        contract.setEndDate(contrattoAttestati.endContract);
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
      contract.setPerson(person);
      contract.setBeginDate(contrattoAttestati.beginContract);
      contract.setEndDate(contrattoAttestati.endContract);
      contractManager.properContractCreate(contract, Optional.absent(), true);

    }

    flash.success("Sono stati definiti %s tempi determinati, sono "
        + "state disattivate %s persone perchè non più appartenenti alla sede, "
        + "sono state terminate %s persone perchè hanno un contratto più recente su attestati, "
        + "sono stati aggiornati %s contratti perchè iniziati successivamente ad attestati.", 
        defined, terminatedInactive, terminatedNewContract, updateOldContract);

    utilities();
  }

  /**
   * Un metodo da sviluppare per l'export della situazione delle
   * persone attive formato csv.
   * nome, contract.id, monte ore anno passato, monte ore anno corrente, buoni pasto residui.
   * alla fine del mese precedente.
   */
  public static void exportDifferences() throws IOException {

    SortedMap<Long, ContractMonthRecap> map = Maps.newTreeMap();

    List<ContractMonthRecap> list = ContractMonthRecap.findAll();
    for (ContractMonthRecap cmr : list) {
      if (cmr.getYear() != LocalDate.now().minusMonths(1).getYear()) {
        continue;
      }
      if (cmr.getMonth() != LocalDate.now().minusMonths(1).getMonthOfYear()) {
        continue;
      }

      map.put(cmr.getContract().id, cmr);
    }

    File tempFile = File.createTempFile("cmr-situation-temp", ".csv");
    FileInputStream inputStream = new FileInputStream(tempFile);
    BufferedWriter out = new BufferedWriter(new FileWriter(tempFile, true));
    for (ContractMonthRecap cmr : map.values()) {

      out.write(cmr.getContract().getPerson().fullName()
          + "," + cmr.getContract().id 
          + "," + cmr.getRemainingMinutesLastYear()
          + "," + cmr.getRemainingMinutesCurrentYear()
          + "," + cmr.getRemainingMealTickets());
      out.newLine();
    }
    out.close();

    renderBinary(inputStream, "cmr-situation-506.csv");
  }


  /**
   * Aggiorna la configurazione di tutti gli uffici.
   *
   * @see: manager.configurations.ConfigurationManager::updateAllOfficeConfigurations
   */
  public static void updateAllOfficeConfigurations() {
    configurationManager.updateAllOfficesConfigurations();
    renderText("Aggiornati i parametri di configuratione di tutti gli uffici.");
  }
  
  public static void updatePeopleConfigurations() {
    configurationManager.updatePeopleConfigurations();
    renderText("Aggiornati i parametri di configurazione di tutte le persone.");
  }


  /**
   * Metodo che riposiziona una sede in un nuovo istituto in caso di accorpamenti.
   *
   * @param office la sede da spostare
   * @param institute l'istituto in cui spostare la sede
   */
  public static void adjustSeats(Office office, Institute institute, 
      String sedeId, String codiceSede) {
    List<Office> officeList = Office.findAll();
    List<Institute> instituteList = Institute.findAll();
    if (!office.isPersistent() || !institute.isPersistent()) {
      render(officeList, instituteList);
    } else {
      changeSeatLocation(office, institute, sedeId, codiceSede);
      officeList = Office.findAll();
      instituteList = Institute.findAll();
      flash.success("Aggiornato rapporto tra %s e %s", office.getName(), institute.getCode());
      render(officeList, instituteList);
    }    

  }

  /**
   * Metodo privato che fa il cambio di appartenenza di una sede.
   *
   * @param office la sede da spostare
   * @param institute l'istituto in cui spostare la sede
   */
  private static void changeSeatLocation(Office office, Institute institute, 
      String sedeId, String codiceSede) {
    Institute oldInstitute = office.getInstitute();
    oldInstitute.getSeats().remove(office);
    office.setInstitute(institute);
    int separatorChar = office.getName().indexOf("-");
    if (separatorChar == -1) {
      separatorChar = office.getName().indexOf(" ");
    }
    String city = office.getName().substring(separatorChar, office.getName().length());
    office.setName(institute.getCode() + city);
    if (!Strings.isNullOrEmpty(sedeId)) {
      office.setCodeId(sedeId);
    }
    if (!Strings.isNullOrEmpty(codiceSede)) {
      office.setCode(codiceSede);
    }
    institute.getSeats().add(office);
    oldInstitute.save();
    office.save();
    institute.save();

  }
  
  public static void emergency(Boolean confirm) {
    render(confirm);
  }
  
  /**
   * Il peggior hack di sempre per uccidere il play.
   * È da utilizzare in casi disperati come quando sta finendo la 
   * RAM e non si riesce a fare altre operazioni.
   * Da usare solo nella versione con Docker che effettua il 
   * riavvio del servizio.
   */
  public static void shutdown(Boolean confirm) {
    if (confirm == null || !confirm) {
      flash.success("Conferma lo shutdown selezionando la casella di conferma");
      confirm = false;
      render("@emergency", confirm);
      return;
    }
    new Job<Void>() {
      public void doJob() {
        log.warn("Killing ePAS -> bye bye, see you soon :-)");
        System.exit(0);
      }
    }.afterRequest();
    redirect("/");
  }

  /**
   * Controlla che se è presente un previousContract e che
   * sia effettivamento il contratto precedente, altrimenti 
   * lo corregge.
   * 
   * @param id l'id del contratto da verificare e correggere se necessario
   */
  public static void fixPreviousContract(Long id) {
    val contract = contractDao.byId(id);
    notFoundIfNull(contract);
    boolean fixed = contractManager.fixPreviousContract(contract);
    if (fixed) {
      flash.success("Precedente contratto di %s corretto", contract.getLabel());
    } else {
      flash.error("Correzione del contratto %s non necessaria", contract.getLabel());
    }
    contractsToFix();
  }

  /**
   * Controlla tutti i contratti con previousContract impostato
   * che potrebbero avere dei problemi e li corregge se necessario.
   */
  public static void fixContractsWithWrongPreviousContract(Optional<Integer> maxSize) {
    log.info("Richiesto il fix del previousContract di max {} contratti", maxSize);
    int fixedContracts = contractManager.fixContractsWithWrongPreviousContract(maxSize);
    log.info("Corretti {} contratti", fixedContracts);
    if (fixedContracts > 0) {
      flash.success("Corretti %s contratti", fixedContracts);
    } else {
      flash.error("Non ci sono contratti da correggere");
    }
    redirect("@Administration.contractsToFix");
  }

  public static void contractsToFix() {
    val contracts = contractDao.getContractsWithWrongPreviousContract();
    render(contracts);
  }
}