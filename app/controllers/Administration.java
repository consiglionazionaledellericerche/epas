package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.ExportToYaml;

import lombok.extern.slf4j.Slf4j;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.ConfigurationManager;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.PersonDayInTroubleManager;
import manager.PersonDayManager;
import manager.SecureManager;

import models.AbsenceType;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.enumerate.EpasParam;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.Parameter;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.data.validation.Required;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

import javax.inject.Inject;


@Slf4j
@With({Resecure.class, RequestInit.class})
public class Administration extends Controller {

  @Inject
  private static SecureManager secureManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static ExportToYaml exportToYaml;
  @Inject
  private static CompetenceUtility competenceUtility;
  @Inject
  private static ConfGeneralManager confGeneralManager;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static ContractDao contractDao;
  @Inject
  private static ContractManager contractManager;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static PersonDayInTroubleManager personDayInTroubleManager;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static ConfYearManager confYearManager;

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
      LocalDate initUse = confGeneralManager
          .getLocalDateFieldValue(Parameter.INIT_USE_PROGRAM, person.office).orNull();

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
   * @param year     l'anno dal quale far partire il fix
   * @param month    il mese dal quale far partire il fix
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
   * metodo che permette la costruzione di codici di assenza e qualifiche a partire da file .yml.
   */
  public static void buildYaml() {
    //general
    exportToYaml.buildAbsenceTypesAndQualifications(
        "db/import/absenceTypesAndQualifications" + DateTime.now()
            .toString("dd-MM-HH:mm") + ".yml");

    exportToYaml.buildCompetenceCodes(
        "db/import/competenceCode" + DateTime.now().toString("dd-MM-HH:mm") + ".yml");

    exportToYaml.buildVacationCodes(
        "db/import/vacationCode" + DateTime.now().toString("dd-MM-HH:mm") + ".yml");

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
   * metodo che controlla se ci sono errori nei periodi di ferie.
   *
   * @param from la data da cui partire
   */
  public static void fixVacationPeriods(LocalDate from) {

    List<Contract> contracts = contractDao
        .getActiveContractsInPeriod(from, Optional.<LocalDate>absent());

    for (Contract contract : contracts) {
      contractManager.setContractVacationPeriod(contract);

      log.info("Il contratto di {} iniziato il {} non è stato ripristinato "
          + "con i piani ferie corretti.", contract.person.fullName(), contract.beginDate);
    }

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
   * Procedura abilitata solo all'utente developer per compiere la migrazione alla nuova gestione
   * dei paraemtri di configurazione.
   */
  public static void migrateConfiguration() {
    
    //migrazione nuova configurazione
    List<Office> offices = officeDao.allOffices().list();
    for (Office office : offices) {
      
      log.info("Inizio migrazione parametri generali {}", office.name);

      Integer day = confGeneralManager
          .getIntegerFieldValue(Parameter.DAY_OF_PATRON, office);
      Integer month = confGeneralManager
          .getIntegerFieldValue(Parameter.MONTH_OF_PATRON, office);

      configurationManager.updateDayMonth(EpasParam.DAY_OF_PATRON, office, day, month, 
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate));
      
      Boolean web = confGeneralManager
          .getBooleanFieldValue(Parameter.WEB_STAMPING_ALLOWED, office);
      
      configurationManager.updateBoolean(EpasParam.WEB_STAMPING_ALLOWED, office, web, 
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate));
      
      // Verificare il vecchio splitter e ad esempio rimuovere le quadre.
      List<String> ipList = Splitter.on("-")
          .splitToList(confGeneralManager.getFieldValue(Parameter.ADDRESSES_ALLOWED, office));
      configurationManager.updateIpList(EpasParam.ADDRESSES_ALLOWED, office, ipList, 
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate));
      
      Integer integer = confGeneralManager
          .getIntegerFieldValue(Parameter.NUMBER_OF_VIEWING_COUPLE, office);
      
      configurationManager.updateInteger(EpasParam.NUMBER_OF_VIEWING_COUPLE, office, integer, 
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate));
      
      Optional<LocalDate> mealTicket = confGeneralManager
          .getLocalDateFieldValue(Parameter.DATE_START_MEAL_TICKET, office);
      LocalDate date = new LocalDate(EpasParam.DATE_START_MEAL_TICKET.defaultValue);
      if (mealTicket.isPresent()) {
        date = mealTicket.get();
      } 
      configurationManager.updateLocalDate(EpasParam.DATE_START_MEAL_TICKET, office, date, 
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate));

      Boolean sendEmail = confGeneralManager
          .getBooleanFieldValue(Parameter.SEND_EMAIL, office);
      
      configurationManager.updateBoolean(EpasParam.SEND_EMAIL, office, sendEmail, 
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate));
      
      String email = confGeneralManager.getFieldValue(Parameter.EMAIL_TO_CONTACT, office);
      
      configurationManager.updateEmail(EpasParam.EMAIL_TO_CONTACT, office, email, 
          Optional.of(office.beginDate), Optional.fromNullable(office.endDate));
      
      Integer year = office.beginDate.getYear();
      while (year != null) {
        
        log.info("Inizio migrazione parametri annuali {} {}", year, office.name);
        
        day = confYearManager
            .getIntegerFieldValue(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, year);
        month = confYearManager
            .getIntegerFieldValue(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, year);
        configurationManager.updateYearlyDayMonth(EpasParam.EXPIRY_VACATION_PAST_YEAR, office, 
            day, month, year, true);
        
        month = confYearManager
            .getIntegerFieldValue(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13, office, year);
        configurationManager.updateYearlyMonth(EpasParam.MONTH_EXPIRY_RECOVERY_DAYS_13, office, 
            month, year, true);
        
        month = confYearManager
            .getIntegerFieldValue(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49, office, year);
        configurationManager.updateYearlyMonth(EpasParam.MONTH_EXPIRY_RECOVERY_DAYS_49, office, 
            month, year, true);
        
        integer = confYearManager
            .getIntegerFieldValue(Parameter.MAX_RECOVERY_DAYS_13, office, year);
        configurationManager.updateYearlyInteger(EpasParam.MAX_RECOVERY_DAYS_13, office, 
            integer, year, true);
        
        integer = confYearManager
            .getIntegerFieldValue(Parameter.MAX_RECOVERY_DAYS_49, office, year);
        configurationManager.updateYearlyInteger(EpasParam.MAX_RECOVERY_DAYS_49, office, 
            integer, year, true);
        
        
        if ((year == LocalDate.now().getYear()) 
            || (office.calculatedEnd() != null && year == office.calculatedEnd().getYear())) {
          year = null;
        } else {
          year++;
        }
      }
      
      log.info("Inizio migrazione parametri periodici {}", office.name);
      
      Integer hour = confYearManager
          .getIntegerFieldValue(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, office, 
              LocalDate.now().getYear());
      configurationManager.updateLocalTime(EpasParam.HOUR_MAX_TO_CALCULATE_WORKTIME, office, 
          new LocalTime(hour, 0), Optional.of(office.beginDate), 
          Optional.fromNullable(office.endDate));
      
      Integer mealTimeStartHour = confGeneralManager
          .getIntegerFieldValue(Parameter.MEAL_TIME_START_HOUR, office);
      Integer mealTimeStartMinute = confGeneralManager
          .getIntegerFieldValue(Parameter.MEAL_TIME_START_MINUTE, office);
      Integer mealTimeEndHour = confGeneralManager
          .getIntegerFieldValue(Parameter.MEAL_TIME_END_HOUR, office);
      Integer mealTimeEndMinute = confGeneralManager
          .getIntegerFieldValue(Parameter.MEAL_TIME_END_MINUTE, office);
      LocalTime startLunch = new LocalTime()
          .withHourOfDay(mealTimeStartHour)
          .withMinuteOfHour(mealTimeStartMinute);
      LocalTime endLunch = new LocalTime()
          .withHourOfDay(mealTimeEndHour)
          .withMinuteOfHour(mealTimeEndMinute);
      
      configurationManager.updateLocalTimeInterval(EpasParam.LUNCH_INTERVAL, office, 
          startLunch, endLunch, Optional.of(office.beginDate), 
          Optional.fromNullable(office.endDate));
      
    }
    
    LocalTime test = new LocalTime(15, 0);
    log.info("test is {}", test.toString("hh:mm"));
  }

}
