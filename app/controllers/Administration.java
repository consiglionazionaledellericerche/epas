package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.CompetenceUtility;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
import manager.PersonDayInTroubleManager;
import manager.PersonDayManager;
import manager.SecureManager;

import models.AbsenceType;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.enumerate.JustifiedTimeAtWork;

import org.apache.commons.lang.WordUtils;
import org.joda.time.LocalDate;

import play.Play;
import play.data.validation.Required;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;


@Slf4j
@With({Resecure.class, RequestInit.class})
public class Administration extends Controller {

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

}
