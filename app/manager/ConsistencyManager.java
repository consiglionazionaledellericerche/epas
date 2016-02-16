package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.IWrapperPersonDay;

import it.cnr.iit.epas.DateInterval;

import manager.cache.StampTypeManager;

import models.Absence;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.EpasParam;
import models.enumerate.EpasParam.RecomputationType;
import models.enumerate.EpasParam.EpasParamValueType.LocalTimeInterval;
import models.enumerate.Troubles;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.JPA;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class ConsistencyManager {

  private static final Logger log = LoggerFactory.getLogger(ConsistencyManager.class);
  private final SecureManager secureManager;
  private final OfficeDao officeDao;
  private final PersonManager personManager;
  private final PersonDao personDao;
  private final PersonDayManager personDayManager;
  private final ContractMonthRecapManager contractMonthRecapManager;
  private final PersonDayInTroubleManager personDayInTroubleManager;
  private final IWrapperFactory wrapperFactory;
  private final PersonDayDao personDayDao;
  private final StampTypeManager stampTypeManager;
  private final ConfigurationManager configurationManager;
  
  /**
   * Constructor.
   * @param secureManager
   * @param officeDao
   * @param personDao
   * @param personDayDao
   * @param personManager
   * @param personDayManager
   * @param contractMonthRecapManager
   * @param personDayInTroubleManager
   * @param confGeneralManager
   * @param confYearManager
   * @param stampTypeManager
   * @param wrapperFactory
   */
  @Inject
  public ConsistencyManager(SecureManager secureManager, 
      OfficeDao officeDao,
      PersonDao personDao,
      PersonDayDao personDayDao,
      
      PersonManager personManager, 
      PersonDayManager personDayManager,
      ContractMonthRecapManager contractMonthRecapManager,
      PersonDayInTroubleManager personDayInTroubleManager, 
      ConfigurationManager configurationManager,
      StampTypeManager stampTypeManager,

      IWrapperFactory wrapperFactory) {

    this.secureManager = secureManager;
    this.officeDao = officeDao;
    this.personManager = personManager;
    this.personDao = personDao;
    this.personDayManager = personDayManager;
    this.contractMonthRecapManager = contractMonthRecapManager;
    this.personDayInTroubleManager = personDayInTroubleManager;
    this.configurationManager = configurationManager;
    this.wrapperFactory = wrapperFactory;
    this.personDayDao = personDayDao;
    this.stampTypeManager = stampTypeManager;
  }

  /**
   * Ricalcolo della situazione di una persona (o tutte) dal mese e anno specificati ad oggi.
   * @param person persona (se absent tutte)
   * @param user utente loggato
   * @param fromDate dalla data
   * @param sendMail se deve inviare le email sui giorni con problemi
   * @param onlyRecap se si vuole aggiornare solo i riepiloghi
   */
  public void fixPersonSituation(Optional<Person> person, Optional<User> user, LocalDate fromDate,
      boolean sendMail, boolean onlyRecap) {

    Set<Office> offices = user.isPresent() ? secureManager.officesWriteAllowed(user.get())
        : Sets.newHashSet(officeDao.getAllOffices());

    // (0) Costruisco la lista di persone su cui voglio operare
    List<Person> personList = Lists.newArrayList();

    if (person.isPresent() && user.isPresent()) {
      // if(personManager.isAllowedBy(user.get(), person.get()))
      personList.add(person.get());
    } else {
      personList = personDao.list(Optional.<String>absent(), offices, false, fromDate,
              LocalDate.now().minusDays(1), true).list();
    }

    for (Person p : personList) {
      if (onlyRecap) {
        updatePersonRecaps(p.id, fromDate);
      } else {
        updatePersonSituation(p.id, fromDate);
      }
      // attenzione quando si forzano le transazioni o si invalida la cache dell'entityManager,
      // possono esserci effetti collaterali....vedi il blocco try sotto
      JPA.em().flush();
      JPA.em().clear();
    }

    log.info("Inizia la parte di pulizia days in trouble...");
    for (Person p : personList) {

      personDayInTroubleManager.cleanPersonDayInTrouble(p);

      JPA.em().flush();
      JPA.em().clear();
    }
    log.info("... Conclusa.");

    if (sendMail 
        && LocalDate.now().getDayOfWeek() != DateTimeConstants.SATURDAY
        && LocalDate.now().getDayOfWeek() != DateTimeConstants.SUNDAY) {

      log.info("Inizia la parte di invio email...");
      
      LocalDate begin = new LocalDate().minusMonths(1);
      LocalDate end = new LocalDate().minusDays(1);

      personList = personDao.list(Optional.<String>absent(), offices, false, fromDate,
              LocalDate.now().minusDays(1), true).list();

      try {
        personDayInTroubleManager.sendTroubleEmails(personList, begin, end, Lists.newArrayList(
            Troubles.UNCOUPLED_FIXED, 
            Troubles.UNCOUPLED_HOLIDAY, 
            Troubles.UNCOUPLED_WORKING));
        
      } catch (Exception e) {
        e.printStackTrace();
      }
      log.info("... Conclusa.");
    }
    
    log.info("Conclusa procedura FixPersonsSituation con parametri!");

  }

  /**
   * Ricalcola i riepiloghi mensili del contratto a partire dalla data from.
   *
   * @param contract contract
   * @param from     la data da cui far partire l'aggiornamento.
   */
  public void updatePersonRecaps(Long personId, LocalDate from) {
    updatePersonSituationEngine(personId, from, Optional.<LocalDate>absent(), true);
  }

  /**
   * Aggiorna la situazione della persona a partire dalla data from.
   *
   * @param contract contract
   * @param from     la data da cui far partire l'aggiornamento.
   */
  public void updatePersonSituation(Long personId, LocalDate from) {
    updatePersonSituationEngine(personId, from, Optional.<LocalDate>absent(), false);
  }

  /**
   * Aggiorna la situazione del contratto a partire dalla data from.
   *
   * @param contract contract
   * @param from     la data da cui far partire l'aggiornamento.
   */
  public void updateContractSituation(Contract contract, LocalDate from) {

    LocalDate to = wrapperFactory.create(contract).getContractDatabaseInterval().getEnd();
    updatePersonSituationEngine(contract.person.id, from, Optional.fromNullable(to), false);
  }

  /**
   * Ricalcola i riepiloghi mensili del contratto a partire dalla data from.
   *
   * @param contract contract
   * @param from     la data da cui far partire l'aggiornamento.
   */
  public void updateContractRecaps(Contract contract, LocalDate from) {

    LocalDate to = wrapperFactory.create(contract).getContractDatabaseInterval().getEnd();
    updatePersonSituationEngine(contract.person.id, from, Optional.fromNullable(to), true);
  }
  
  /**
   * Effettua la ricomputazione.
   * @param office
   * @param recomputationTypes
   * @param recomputeFrom
   */
  public void performRecomputation(Office office, List<RecomputationType> recomputationTypes, 
      LocalDate recomputeFrom) {
    
    if (recomputationTypes.isEmpty()) {
      return;
    }
    
    if (recomputeFrom == null) {
      return;
    }
    
    for (Person person : office.persons) {
      if (recomputationTypes.contains(RecomputationType.DAYS)) {
        updatePersonSituation(person.id, recomputeFrom);
      } 
      if (recomputationTypes.contains(RecomputationType.RESIDUAL_HOURS) 
          || recomputationTypes.contains(RecomputationType.RESIDUAL_MEALTICKETS)) {
        updatePersonRecaps(person.id, recomputeFrom);
      }
      JPA.em().flush();
      JPA.em().clear();
    }
    
  }


  private void updatePersonSituationEngine(Long personId, LocalDate from, Optional<LocalDate> to,
                                           boolean updateOnlyRecaps) {

    final Person person = personDao.fetchPersonForComputation(personId, Optional.fromNullable(from),
            Optional.<LocalDate>absent());

    log.info("Lanciato aggiornamento situazione {} da {} a oggi", person.getFullname(), from);

    if (person.qualification == null) {
      log.info("... annullato ricalcolo per {} in quanto priva di qualifica", person.getFullname());
      return;
    }

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    // Gli intervalli di ricalcolo dei person day.
    LocalDate lastPersonDayToCompute = LocalDate.now();
    if (to.isPresent() && to.get().isBefore(lastPersonDayToCompute)) {
      lastPersonDayToCompute = to.get();
    }
    LocalDate date = personFirstDateForEpasComputation(person, Optional.fromNullable(from));

    List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, date,
            Optional.fromNullable(lastPersonDayToCompute));

    // Costruire la tabella hash
    HashMap<LocalDate, PersonDay> personDaysMap = Maps.newHashMap();
    for (PersonDay personDay : personDays) {
      personDaysMap.put(personDay.date, personDay);
    }

    log.info("... fetch dei dati conclusa, inizio dei ricalcoli.");

    PersonDay previous = null;

    if (!updateOnlyRecaps) {

      while (!date.isAfter(lastPersonDayToCompute)) {

        if (!wrPerson.isActiveInDay(date)) {
          date = date.plusDays(1);
          previous = null;
          continue;
        }

        // Prendere da map
        PersonDay personDay = personDaysMap.get(date);
        if (personDay == null) {
          personDay = new PersonDay(person, date);
        }

        IWrapperPersonDay wrPersonDay = wrapperFactory.create(personDay);

        // set previous for progressive
        if (previous != null) {
          wrPersonDay.setPreviousForProgressive(Optional.fromNullable(previous));
        }
        // set previous for night stamp
        if (previous != null) {
          wrPersonDay.setPreviousForNightStamp(Optional.fromNullable(previous));
        }

        populatePersonDay(wrPersonDay);

        previous = personDay;
        date = date.plusDays(1);

      }

      log.info("... ricalcolo dei giorni lavorativi conclusa.");
    }
    // (3) Ricalcolo dei residui per mese
    populateContractMonthRecapByPerson(person, new YearMonth(from));

    log.info("... ricalcolo dei riepiloghi conclusa.");
  }

  /**
   * Il primo giorno di ricalcolo ePAS per la persona. <br> La data più recente fra: 1) from <br> 2)
   * inizio utilizzo software per l'office della persona <br> 3) creazione della persona.
   */
  private LocalDate personFirstDateForEpasComputation(Person person, Optional<LocalDate> from) {

    LocalDate officeLimit = person.office.getBeginDate();
    
    // Calcolo a partire da
    LocalDate lowerBoundDate = new LocalDate(person.createdAt);

    if (officeLimit.isAfter(lowerBoundDate)) {
      lowerBoundDate = officeLimit;
    }

    if (from.isPresent() && from.get().isAfter(lowerBoundDate)) {
      lowerBoundDate = from.get();
    }

    return lowerBoundDate;
  }


  /**
   * (1) Controlla che il personDay sia ben formato (altrimenti lo inserisce nella tabella
   * PersonDayInTrouble) (2) Popola i valori aggiornati del person day e li persiste nel db.
   */
  private void populatePersonDay(IWrapperPersonDay pd) {

    // isHoliday = personManager.isHoliday(this.value.person, this.value.date);

    // il contratto non esiste più nel giorno perchè è stata inserita data terminazione
    if (!pd.getPersonDayContract().isPresent()) {
      pd.getValue().isHoliday = false;
      pd.getValue().timeAtWork = 0;
      pd.getValue().progressive = 0;
      pd.getValue().difference = 0;
      personDayManager.setTicketStatusIfNotForced(pd.getValue(), false);
      pd.getValue().stampModificationType = null;
      pd.getValue().save();
      return;
    }

    // Nel caso in cui il personDay non sia successivo a sourceContract imposto i valori a 0
    if (pd.getPersonDayContract().isPresent()
            && pd.getPersonDayContract().get().sourceDateResidual != null
            && !pd.getValue().date.isAfter(pd.getPersonDayContract().get().sourceDateResidual)) {

      pd.getValue().isHoliday = false;
      pd.getValue().timeAtWork = 0;
      pd.getValue().progressive = 0;
      pd.getValue().difference = 0;
      personDayManager.setTicketStatusIfNotForced(pd.getValue(), false);
      pd.getValue().stampModificationType = null;
      pd.getValue().save();
      return;
    }

    // decido festivo / lavorativo
    pd.getValue().isHoliday = personManager.isHoliday(pd.getValue().person, pd.getValue().date);
    pd.getValue().save();

    // controllo uscita notturna
    handlerNightStamp(pd);

    Preconditions.checkArgument(pd.getWorkingTimeTypeDay().isPresent());

    LocalTimeInterval lunchInterval = (LocalTimeInterval)configurationManager.configValue(
        pd.getValue().person.office, EpasParam.LUNCH_INTERVAL, pd.getValue().getDate());
        
    personDayManager.updateTimeAtWork(pd.getValue(), pd.getWorkingTimeTypeDay().get(), 
        pd.isFixedTimeAtWork(), lunchInterval.from, lunchInterval.to);

    personDayManager.updateDifference(pd.getValue(), pd.getWorkingTimeTypeDay().get(), 
        pd.isFixedTimeAtWork());

    personDayManager.updateProgressive(pd.getValue(), pd.getPreviousForProgressive());

    personDayManager.updateTicketAvailable(pd.getValue(), pd.getWorkingTimeTypeDay().get(), 
        pd.isFixedTimeAtWork());

    // controllo problemi strutturali del person day
    if (pd.getValue().date.isBefore(LocalDate.now())) {
      personDayManager.checkForPersonDayInTrouble(pd);
    }

    pd.getValue().save();

  }

  /**
   * Se al giorno precedente l'ultima timbratura è una entrata disaccoppiata e nel giorno attuale vi
   * è una uscita nei limiti notturni in configurazione, allora vengono aggiunte le timbrature
   * default a 00:00.
   */
  private void handlerNightStamp(IWrapperPersonDay pd) {

    if (pd.isFixedTimeAtWork()) {
      return;
    }

    if (!pd.getPreviousForNightStamp().isPresent()) {
      return;
    }

    PersonDay previous = pd.getPreviousForNightStamp().get();

    Stamping lastStampingPreviousDay = pd.getLastStamping();

    if (lastStampingPreviousDay != null && lastStampingPreviousDay.isIn()) {

      // TODO: controllare, qui esiste un caso limite. Considero pd.date o previous.date?
      LocalTime maxHour = (LocalTime)configurationManager.configValue(pd.getValue().person.office, 
          EpasParam.HOUR_MAX_TO_CALCULATE_WORKTIME, pd.getValue().getDate());

      Collections.sort(pd.getValue().stampings);

      if (pd.getValue().stampings.size() > 0 && pd.getValue().stampings.get(0).way == WayType.out
              && maxHour.isAfter(pd.getValue().stampings.get(0).date.toLocalTime())) {

        StampModificationType smtMidnight = stampTypeManager.getStampMofificationType(
                StampModificationTypeCode.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT);

        // timbratura chiusura giorno precedente
        Stamping exitStamp = new Stamping(previous, new LocalDateTime(previous.date.getYear(),
                previous.date.getMonthOfYear(), previous.date.getDayOfMonth(), 23, 59));

        exitStamp.way = WayType.out;
        exitStamp.markedByAdmin = false;
        exitStamp.stampModificationType = smtMidnight;
        exitStamp.note =
            "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della "
            + "mezzanotte";
        exitStamp.personDay = previous;
        exitStamp.save();
        previous.stampings.add(exitStamp);
        previous.save();

        populatePersonDay(wrapperFactory.create(previous));

        // timbratura apertura giorno attuale
        Stamping enterStamp =
            new Stamping(
                pd.getValue(), new LocalDateTime(pd.getValue().date.getYear(),
                pd.getValue().date.getMonthOfYear(), pd.getValue().date.getDayOfMonth(), 0, 0));

        enterStamp.way = WayType.in;
        enterStamp.markedByAdmin = false;

        enterStamp.stampModificationType = smtMidnight;


        enterStamp.note =
                "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo "
                + "della mezzanotte";

        enterStamp.save();

        pd.getValue().stampings.add(enterStamp);
        pd.getValue().save();
      }
    }
  }

  /**
   * Costruisce i riepiloghi mensili dei contratti della persona a partire da yeraMonthFrom.
   */
  private void populateContractMonthRecapByPerson(Person person, YearMonth yearMonthFrom) {

    for (Contract contract : person.contracts) {

      IWrapperContract wrContract = wrapperFactory.create(contract);
      DateInterval contractDateInterval = wrContract.getContractDateInterval();
      YearMonth endContractYearMonth = new YearMonth(contractDateInterval.getEnd());

      // Se yearMonthFrom non è successivo alla fine del contratto...
      if (!yearMonthFrom.isAfter(endContractYearMonth)) {

        if (contract.vacationPeriods.isEmpty()) {
          log.error("No vacation period {}", contract.toString());
          continue;
        }

        populateContractMonthRecap(wrContract, Optional.fromNullable(yearMonthFrom));
      }
    }
  }

  /**
   * Costruzione di un ContractMonthRecap pulito. <br> Se esiste già un oggetto per il contract e
   * yearMonth specificati viene azzerato.
   */
  private ContractMonthRecap buildContractMonthRecap(IWrapperContract contract,
                                                     YearMonth yearMonth) {

    Optional<ContractMonthRecap> cmrOld = contract.getContractMonthRecap(yearMonth);

    if (cmrOld.isPresent()) {
      cmrOld.get().clean();
      return cmrOld.get();
    }

    ContractMonthRecap cmr = new ContractMonthRecap();
    cmr.year = yearMonth.getYear();
    cmr.month = yearMonth.getMonthOfYear();
    cmr.contract = contract.getValue();

    return cmr;
  }

  /**
   * Costruttore dei riepiloghi mensili per contract. <br> Se il contratto non è inizializzato
   * correttamente non effettua alcun ricalcolo. <br> Se yearMonthFrom è absent() calcola tutti i
   * riepiloghi dall'inizio del contratto. <br> Se yearMonthFrom è present() calcola tutti i
   * riepiloghi da yearMonthFrom. <br> Se non esiste il riepilogo precedente a yearMonthFrom chiama
   * la stessa procedura dall'inizio del contratto. (Capire se questo caso si verifica mai).
   */
  private void populateContractMonthRecap(IWrapperContract contract,
      Optional<YearMonth> yearMonthFrom) {

    // Conterrà il riepilogo precedente di quello da costruire all'iterazione n.
    Optional<ContractMonthRecap> previousMonthRecap = Optional.<ContractMonthRecap>absent();

    Optional<YearMonth> firstContractMonthRecap = contract.getFirstMonthToRecap();
    if (!firstContractMonthRecap.isPresent()) {
      // Non ho inizializzazione.
      return;
    }

    // Analisi della richiesta. Inferisco il primo mese da riepilogare.

    // Di default costruisco il primo mese da riepilogare del contratto.
    YearMonth yearMonthToCompute = firstContractMonthRecap.get();

    // Se ho specificato un mese in particolare
    // che necessita di un riepilogo precedente verifico che esso esista!
    if (yearMonthFrom.isPresent() && yearMonthFrom.get().isAfter(firstContractMonthRecap.get())) {

      yearMonthToCompute = yearMonthFrom.get();
      previousMonthRecap = contract.getContractMonthRecap(yearMonthFrom.get().minusMonths(1));
      if (!previousMonthRecap.isPresent()) {
        // Ho chiesto un mese specifico ma non ho il riepilogo precedente
        // per costruirlo. Soluzione: costruisco tutti i riepiloghi del contratto.
        populateContractMonthRecap(contract, Optional.<YearMonth>absent());
      }
    } else if (contract.getValue().sourceDateResidual != null
            && yearMonthToCompute.isEqual(new YearMonth(contract.getValue().sourceDateResidual))) {

      // Il calcolo del riepilogo del mese che ricade nel sourceDateResidual
      // è particolare e va gestito con un metodo dedicato.

      previousMonthRecap =
              Optional.fromNullable(populateContractMonthFromSource(contract, yearMonthToCompute));
      yearMonthToCompute = yearMonthToCompute.plusMonths(1);
    }

    // Ciclo sui mesi successivi fino all'ultimo mese da costruire
    YearMonth lastMonthToCompute = contract.getLastMonthToRecap();

    // Contiene il riepilogo da costruire.
    ContractMonthRecap currentMonthRecap;

    while (!yearMonthToCompute.isAfter(lastMonthToCompute)) {

      currentMonthRecap = buildContractMonthRecap(contract, yearMonthToCompute);

      LocalDate lastDayInYearMonth =
              new LocalDate(yearMonthToCompute.getYear(), yearMonthToCompute.getMonthOfYear(), 1)
                      .dayOfMonth().withMaximumValue();

      // (2) RESIDUI
      List<Absence> otherCompensatoryRest = Lists.newArrayList();
      Optional<ContractMonthRecap> recap =
              contractMonthRecapManager.computeResidualModule(currentMonthRecap, previousMonthRecap,
                      yearMonthToCompute, lastDayInYearMonth, otherCompensatoryRest);

      recap.get().save();
      contract.getValue().contractMonthRecaps.add(recap.get());
      contract.getValue().save();

      previousMonthRecap = Optional.fromNullable(currentMonthRecap);
      yearMonthToCompute = yearMonthToCompute.plusMonths(1);
    }
  }

  /**
   * Costruzione del riepilogo mensile nel caso particolare di inzializzazione del contratto nel
   * mese<br> 1) Se l'inizializzazione è l'ultimo giorno del mese costruisce il riepilogo copiando
   * le informazioni in esso contenute. <br> 2) Se l'inizializzazione non è l'ultimo giorno del mese
   * combina le informazioni presenti in inizializzazione e quelle in database (a partire dal giorno
   * successivo alla inizializzazione). <br>
   */
  private ContractMonthRecap populateContractMonthFromSource(IWrapperContract contract,
                                                             YearMonth yearMonthToCompute) {

    // Caso semplice ultimo giorno del mese
    LocalDate lastDayInSourceMonth =
            contract.getValue().sourceDateResidual.dayOfMonth().withMaximumValue();

    if (lastDayInSourceMonth.isEqual(contract.getValue().sourceDateResidual)) {
      ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);

      cmr.remainingMinutesCurrentYear = contract.getValue().sourceRemainingMinutesCurrentYear;
      cmr.remainingMinutesLastYear = contract.getValue().sourceRemainingMinutesLastYear;
      cmr.recoveryDayUsed = contract.getValue().sourceRecoveryDayUsed;

      if (contract.getValue().sourceDateMealTicket != null && contract.getValue().sourceDateResidual
              .isEqual(contract.getValue().sourceDateMealTicket)) {
        cmr.buoniPastoDaInizializzazione = contract.getValue().sourceRemainingMealTicket;
        cmr.remainingMealTickets = contract.getValue().sourceRemainingMealTicket;
      } else {
        // Non hanno significato, il riepilogo dei residui dei buoni pasto
        // inizia successivamente.
        cmr.buoniPastoDaInizializzazione = 0;
        cmr.remainingMealTickets = 0;
      }
      cmr.save();
      contract.getValue().contractMonthRecaps.add(cmr);
      contract.getValue().save();
      return cmr;
    }

    // Caso complesso, combinare inizializzazione e database.

    ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);

    contract.getValue().contractMonthRecaps.add(cmr);
    cmr.save();

    // Informazioni relative ai residui
    List<Absence> otherCompensatoryRest = Lists.newArrayList();
    contractMonthRecapManager.computeResidualModule(cmr, Optional.<ContractMonthRecap>absent(),
            yearMonthToCompute, new LocalDate().minusDays(1), otherCompensatoryRest);

    cmr.save();

    contract.getValue().save();
    return cmr;

  }
  
}
