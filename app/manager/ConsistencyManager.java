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

package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.AbsenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonShiftDayDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.IWrapperPersonDay;
import it.cnr.iit.epas.DateInterval;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import manager.cache.StampTypeManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import manager.configurations.EpasParam.RecomputationType;
import manager.services.absences.AbsenceService;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonShiftDay;
import models.PersonalWorkingTime;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.Stamping.WayType;
import models.TimeVariation;
import models.User;
import models.absences.Absence;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import models.base.IPropertiesInPeriodOwner;
import models.enumerate.MealTicketBehaviour;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.libs.F.Promise;

/**
 * Manager per la gestione della consistenza tra timbrature ed assenze.
 *
 * @author Alessandro Martelli
 */
public class ConsistencyManager {

  private static final Logger log = LoggerFactory.getLogger(ConsistencyManager.class);
  private final SecureManager secureManager;
  private final OfficeDao officeDao;
  private final PersonDao personDao;
  private final PersonDayManager personDayManager;
  private final ContractMonthRecapManager contractMonthRecapManager;
  private final PersonDayInTroubleManager personDayInTroubleManager;
  private final IWrapperFactory wrapperFactory;
  private final PersonDayDao personDayDao;
  private final PersonShiftDayDao personShiftDayDao;
  private final StampTypeManager stampTypeManager;
  private final ConfigurationManager configurationManager;
  private final ShiftManager2 shiftManager2;
  private final AbsenceService absenceService;
  private final AbsenceComponentDao absenceComponentDao;
  private final AbsenceDao absenceDao;

  /**
   * Constructor.
   *
   * @param secureManager secureManager
   * @param officeDao officeDao
   * @param personDao personDao
   * @param personDayDao personDayDao
   * @param personDayManager personDayManager
   * @param contractMonthRecapManager contractMonthRecapManager
   * @param personDayInTroubleManager personDayInTroubleManager
   * @param configurationManager configurationManager
   * @param stampTypeManager stampTypeManager
   * @param absenceService absenceService
   * @param wrapperFactory wrapperFactory
   */
  @Inject
  public ConsistencyManager(SecureManager secureManager,
      OfficeDao officeDao,
      PersonDao personDao,
      PersonDayDao personDayDao,
      PersonShiftDayDao personShiftDayDao,

      PersonDayManager personDayManager,
      ContractMonthRecapManager contractMonthRecapManager,
      PersonDayInTroubleManager personDayInTroubleManager,
      ConfigurationManager configurationManager,
      StampTypeManager stampTypeManager,
      ShiftManager2 shiftManager2,
      AbsenceService absenceService,
      AbsenceComponentDao absenceComponentDao,
      IWrapperFactory wrapperFactory, AbsenceDao absenceDao) {

    this.secureManager = secureManager;
    this.officeDao = officeDao;
    this.personDao = personDao;
    this.personDayManager = personDayManager;
    this.contractMonthRecapManager = contractMonthRecapManager;
    this.personDayInTroubleManager = personDayInTroubleManager;
    this.configurationManager = configurationManager;
    this.absenceService = absenceService;
    this.absenceComponentDao = absenceComponentDao;
    this.wrapperFactory = wrapperFactory;
    this.personDayDao = personDayDao;
    this.stampTypeManager = stampTypeManager;
    this.personShiftDayDao = personShiftDayDao;
    this.shiftManager2 = shiftManager2;
    this.absenceDao = absenceDao;
  }

  /**
   * Ricalcolo della situazione di una persona (o tutte) dal mese e anno specificati ad oggi.
   *
   * @param person persona (se absent tutte)
   * @param user utente loggato
   * @param fromDate dalla data
   * @param onlyRecap se si vuole aggiornare solo i riepiloghi
   */
  public void fixPersonSituation(Optional<Person> person, Optional<User> user, LocalDate fromDate,
      boolean onlyRecap) {

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

    final List<Promise<Void>> results = new ArrayList<>();
    for (Person p : personList) {

      results.add(new Job<Void>() {

        @Override
        public void doJob() {
          final Person person = Person.findById(p.id);

          if (onlyRecap) {
            updatePersonRecaps(person.id, fromDate);
          } else {
            updatePersonSituation(person.id, fromDate);
          }

          personDayInTroubleManager.cleanPersonDayInTrouble(person);
          log.debug("Elaborata la persona ... {}", person);
        }
      }.now());

    }
    Promise.waitAll(results);
    log.info("Conclusa procedura FixPersonsSituation con parametri!");
  }

  /**
   * Ricalcolo della situazione di una lista di persone dal mese e anno specificati ad oggi.
   *
   * @param personList la lista delle persone da ricalcolare
   * @param fromDate dalla data
   * @param onlyRecap se si vuole aggiornare solo i riepiloghi
   */
  public void fixPersonSituation(List<Person> personList, LocalDate fromDate, boolean onlyRecap) {

    final List<Promise<Void>> results = new ArrayList<>();
    for (Person p : personList) {

      results.add(new Job<Void>() {

        @Override
        public void doJob() {
          final Person person = Person.findById(p.id);

          if (onlyRecap) {
            updatePersonRecaps(person.id, fromDate);
          } else {
            updatePersonSituation(person.id, fromDate);
          }

          personDayInTroubleManager.cleanPersonDayInTrouble(person);
          log.debug("Elaborata la persona ... {}", person);
        }
      }.now());

    }
    Promise.waitAll(results);
    log.info("Conclusa procedura FixPersonsSituation con parametri!");
  }

  /**
   * Ricalcola i riepiloghi mensili del contratto a partire dalla data from.
   *
   * @param personId id della persona
   * @param from data dalla quale effettuare i ricalcoli
   */
  public void updatePersonRecaps(Long personId, LocalDate from) {
    updatePersonSituationEngine(personId, from, Optional.<LocalDate>absent(), true);
  }

  /**
   * Aggiorna la situazione della persona a partire dalla data from.
   *
   * @param personId id della persona
   * @param from data dalla quale effettuare i ricalcoli
   */
  public void updatePersonSituation(Long personId, LocalDate from) {
    updatePersonSituationEngine(personId, from, Optional.<LocalDate>absent(), false);
  }

  /**
   * Aggiorna la situazione del contratto a partire dalla data from.
   *
   * @param contract contract
   * @param from la data da cui far partire l'aggiornamento.
   */
  public void updateContractSituation(Contract contract, LocalDate from) {

    LocalDate to = wrapperFactory.create(contract).getContractDatabaseInterval().getEnd();
    updatePersonSituationEngine(contract.getPerson().id, from, Optional.fromNullable(to), false);
  }

  /**
   * Ricalcola i riepiloghi mensili del contratto a partire dalla data from.
   *
   * @param contract contract
   * @param from la data da cui far partire l'aggiornamento.
   */
  public void updateContractRecaps(Contract contract, LocalDate from) {

    LocalDate to = wrapperFactory.create(contract).getContractDatabaseInterval().getEnd();
    updatePersonSituationEngine(contract.getPerson().id, from, Optional.fromNullable(to), true);
  }

  /**
   * Effettua la ricomputazione.
   */
  public void performRecomputation(IPropertiesInPeriodOwner target,
      List<RecomputationType> recomputationTypes, LocalDate recomputeFrom) {

    if (recomputationTypes.isEmpty()) {
      return;
    }
    if (recomputeFrom == null) {
      return;
    }

    List<Person> personToRecompute = Lists.newArrayList();

    if (target instanceof Office) {
      personToRecompute = ((Office) target).getPersons();
    } else if (target instanceof Person) {
      personToRecompute.add((Person) target);
    }

    for (Person person : personToRecompute) {
      if (recomputationTypes.contains(RecomputationType.DAYS)) {
        updatePersonSituation(person.id, recomputeFrom);
      } else if (recomputationTypes.contains(RecomputationType.RESIDUAL_HOURS)
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
    
    log.debug("Lanciato aggiornamento situazione {} da {} a oggi", person.getFullname(), from);

    if (person.getQualification() == null) {
      log.warn("... annullato ricalcolo per {} in quanto priva di qualifica", person.getFullname());
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
      personDaysMap.put(personDay.getDate(), personDay);
    }

    log.trace("... fetch dei dati conclusa, inizio dei ricalcoli.");

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

        if (previous != null) {
          // set previous for progressive
          wrPersonDay.setPreviousForProgressive(Optional.fromNullable(previous));
          // set previous for night stamp
          wrPersonDay.setPreviousForNightStamp(Optional.fromNullable(previous));
        }

        populatePersonDay(wrPersonDay);

        previous = personDay;
        date = date.plusDays(1);
      }

      log.trace("... ricalcolo dei giorni lavorativi conclusa.");
    }
        
    // (3) Ricalcolo dei residui per mese        
    populateContractMonthRecapByPerson(person, new YearMonth(from));

    // (4) Scan degli errori sulle assenze
    absenceService.scanner(person, from);
    
    // (5) Empty vacation cache and async recomputation
    
    absenceService.emptyVacationCache(person, from);
    final Optional<Contract> contract = wrPerson.getCurrentContract();
 
    if (contract.isPresent()) {
      new Job<Void>() {
        @Override
        public void doJob() {
          Verify.verifyNotNull(contract.get().id);          
          Contract currentContract = Contract.findById(contract.get().id);
          Verify.verifyNotNull(currentContract, 
              String.format("currentcontract is null, contract.id = %s", contract.get().id));
          GroupAbsenceType vacationGroup = absenceComponentDao
              .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
          absenceService.buildVacationSituation(currentContract, LocalDate.now().getYear(),
              vacationGroup, Optional.absent(), true);
        }
      }.afterRequest();
    }
    // (6) Controllo se per quel giorno person ha anche un turno associato ed effettuo, i ricalcoli
    
    Optional<PersonShiftDay> psd = personShiftDayDao.byPersonAndDate(person, from);
    if (psd.isPresent()) {
      shiftManager2.checkShiftValid(psd.get());
    }
        
    log.trace("... ricalcolo dei riepiloghi conclusa.");
  }

  /**
   * Il primo giorno di ricalcolo ePAS per la persona. <br> La data più recente fra: 1) from <br> 2)
   * inizio utilizzo software per l'office della persona <br> 3) creazione della persona.
   */
  private LocalDate personFirstDateForEpasComputation(Person person, Optional<LocalDate> from) {

    LocalDate officeLimit = person.getOffice().getBeginDate();

    // Calcolo a partire da
    LocalDate lowerBoundDate = new LocalDate(person.getBeginDate());

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
      pd.getValue().setHoliday(false);
      pd.getValue().setTimeAtWork(0);
      pd.getValue().setProgressive(0);
      pd.getValue().setDifference(0);
      personDayManager.setTicketStatusIfNotForced(pd.getValue(), 
          MealTicketBehaviour.notAllowMealTicket);
      pd.getValue().setStampModificationType(null);
      pd.getValue().save();
      return;
    }

    // Nel caso in cui il personDay non sia successivo a sourceContract imposto i valori a 0
    if (pd.getPersonDayContract().isPresent()
        && pd.getPersonDayContract().get().getSourceDateResidual() != null
        && pd.getValue().getDate().isBefore(pd.getPersonDayContract()
            .get().getSourceDateResidual())) {

      pd.getValue().setHoliday(false);
      pd.getValue().setTimeAtWork(0);
      pd.getValue().setProgressive(0);
      pd.getValue().setDifference(0);
      personDayManager.setTicketStatusIfNotForced(pd.getValue(), 
          MealTicketBehaviour.notAllowMealTicket);
      pd.getValue().setStampModificationType(null);
      pd.getValue().save();
      return;
    }

    // decido festivo / lavorativo
    pd.getValue().setHoliday(personDayManager
        .isHoliday(pd.getValue().getPerson(), pd.getValue().getDate()));
    pd.getValue().save();

    // controllo uscita notturna
    handlerNightStamp(pd);

    log.trace("populatePersonDay {}", pd.getValue());
    Preconditions.checkArgument(pd.getWorkingTimeTypeDay().isPresent(),
        String.format("getWorkingTimeTypeDay di %s non presente per il giorno %s",
            pd.getValue().getPerson().getFullname(), pd.getValue().getDate()));

    LocalTimeInterval lunchInterval = (LocalTimeInterval) configurationManager.configValue(
        pd.getValue().getPerson().getOffice(), EpasParam.LUNCH_INTERVAL, pd.getValue().getDate());

    
    LocalTimeInterval workInterval = null;
    Optional<PersonalWorkingTime> pwt = pd.getPersonalWorkingTime();
    if (pwt.isPresent()) {
      workInterval = new LocalTimeInterval(pwt.get().getTimeSlot().getBeginSlot(), 
          pwt.get().getTimeSlot().getEndSlot());
    } else {
      workInterval = (LocalTimeInterval) configurationManager.configValue(
        pd.getValue().getPerson().getOffice(), EpasParam.WORK_INTERVAL, pd.getValue().getDate());
    }
    
    personDayManager.updateTimeAtWork(pd.getValue(), pd.getWorkingTimeTypeDay().get(),
        pd.isFixedTimeAtWork(), lunchInterval.from, lunchInterval.to, workInterval.from,
        workInterval.to, Optional.absent());
   
    personDayManager.updateDifference(pd.getValue(), pd.getWorkingTimeTypeDay().get(),
        pd.isFixedTimeAtWork(), lunchInterval.from, lunchInterval.to, workInterval.from,
        workInterval.to, Optional.absent());
   
    personDayManager.updateProgressive(pd.getValue(), pd.getPreviousForProgressive());
    
    personDayManager.checkAndManageMandatoryTimeSlot(pd.getValue());

    // controllo problemi strutturali del person day
    if (pd.getValue().getDate().isBefore(LocalDate.now())) {
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

    Stamping lastStampingPreviousDay = wrapperFactory.create(previous).getLastStamping();

    if (lastStampingPreviousDay != null && lastStampingPreviousDay.isIn()) {

      // TODO: controllare, qui esiste un caso limite. Considero pd.date o previous.date?
      LocalTime maxHour = (LocalTime) configurationManager
          .configValue(pd.getValue().getPerson().getOffice(),
          EpasParam.HOUR_MAX_TO_CALCULATE_WORKTIME, pd.getValue().getDate());

      Collections.sort(pd.getValue().getStampings());

      if (pd.getValue().getStampings().size() > 0 
          && pd.getValue().getStampings().get(0).getWay() == WayType.out
          && maxHour.isAfter(pd.getValue().getStampings().get(0).getDate().toLocalTime())) {

        StampModificationType smtMidnight = stampTypeManager.getStampMofificationType(
            StampModificationTypeCode.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT);

        // timbratura chiusura giorno precedente
        Stamping exitStamp = new Stamping(previous, new LocalDateTime(previous.getDate().getYear(),
            previous.getDate().getMonthOfYear(), previous.getDate().getDayOfMonth(), 23, 59));

        exitStamp.setWay(WayType.out);
        exitStamp.setMarkedByAdmin(false);
        exitStamp.setStampModificationType(smtMidnight);
        exitStamp.setNote(
            "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della "
                + "mezzanotte");
        exitStamp.setPersonDay(previous);
        exitStamp.save();
        previous.getStampings().add(exitStamp);
        previous.save();

        populatePersonDay(wrapperFactory.create(previous));

        // timbratura apertura giorno attuale
        Stamping enterStamp =
            new Stamping(
                pd.getValue(), new LocalDateTime(pd.getValue().getDate().getYear(),
                pd.getValue().getDate().getMonthOfYear(), pd.getValue().getDate().getDayOfMonth(), 0, 0));

        enterStamp.setWay(WayType.in);
        enterStamp.setMarkedByAdmin(false);

        enterStamp.setStampModificationType(smtMidnight);

        enterStamp.setNote(
            "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo "
                + "della mezzanotte");

        enterStamp.save();
      }
    }
  }

  /**
   * Costruisce i riepiloghi mensili dei contratti della persona a partire da yearMonthFrom.
   */
  private void populateContractMonthRecapByPerson(Person person, YearMonth yearMonthFrom) {
    
    for (Contract contract : person.getContracts()) {

      IWrapperContract wrContract = wrapperFactory.create(contract);
      DateInterval contractDateInterval = wrContract.getContractDateInterval();
      YearMonth endContractYearMonth = new YearMonth(contractDateInterval.getEnd());

      // Se yearMonthFrom non è successivo alla fine del contratto...
      if (!yearMonthFrom.isAfter(endContractYearMonth)) {

        if (contract.getExtendedVacationPeriods().isEmpty()) {
          log.error("No vacation period {}", contract.toString());
          continue;
        }
        
        LocalDate begin = person.getOffice().getBeginDate();
        LocalDate end = new LocalDate(yearMonthFrom.getYear(), 
            yearMonthFrom.getMonthOfYear(), 1).dayOfMonth().withMaximumValue();
        
        List<Absence> absences = absenceDao.absenceInPeriod(person, begin, end, "91CE");
        List<TimeVariation> list = absences.stream()
            .flatMap(abs -> abs.getTimeVariations().stream()
                .filter(tv -> !tv.getDateVariation().isBefore(begin) 
                    && !tv.getDateVariation().isAfter(end)))
            .collect(Collectors.toList());
        
        
        populateContractMonthRecap(wrContract, Optional.fromNullable(yearMonthFrom), list);
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
    cmr.setYear(yearMonth.getYear());
    cmr.setMonth(yearMonth.getMonthOfYear());
    cmr.setContract(contract.getValue());

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
      Optional<YearMonth> yearMonthFrom, List<TimeVariation> timeVariationList) {

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
        populateContractMonthRecap(contract, Optional.<YearMonth>absent(), timeVariationList);
      }
    } else if (contract.getValue().getSourceDateResidual() != null
        && yearMonthToCompute.isEqual(new YearMonth(contract.getValue().getSourceDateResidual()))) {

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
              yearMonthToCompute, lastDayInYearMonth, otherCompensatoryRest, 
              Optional.fromNullable(timeVariationList));

      recap.get().save();
      contract.getValue().getContractMonthRecaps().add(recap.get());
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
        contract.getValue().getSourceDateResidual().dayOfMonth().withMaximumValue();

    if (lastDayInSourceMonth.isEqual(contract.getValue().getSourceDateResidual())) {
      ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);

      cmr.setRemainingMinutesCurrentYear(
          contract.getValue().getSourceRemainingMinutesCurrentYear());
      cmr.setRemainingMinutesLastYear(contract.getValue().getSourceRemainingMinutesLastYear());
      cmr.setRecoveryDayUsed(contract.getValue().getSourceRecoveryDayUsed());

      if (contract.getValue().getSourceDateMealTicket() != null 
          && contract.getValue().getSourceDateResidual()
          .isEqual(contract.getValue().getSourceDateMealTicket())) {
        cmr.setBuoniPastoDaInizializzazione(contract.getValue().getSourceRemainingMealTicket());
        cmr.setRemainingMealTickets(contract.getValue().getSourceRemainingMealTicket());
      } else {
        // Non hanno significato, il riepilogo dei residui dei buoni pasto
        // inizia successivamente.
        cmr.setBuoniPastoDaInizializzazione(0);
        cmr.setRemainingMealTickets(0);
      }
      cmr.save();
      contract.getValue().getContractMonthRecaps().add(cmr);
      contract.getValue().save();
      return cmr;
    }

    // Caso complesso, combinare inizializzazione e database.

    ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);

    contract.getValue().getContractMonthRecaps().add(cmr);
    cmr.save();

    // Informazioni relative ai residui
    List<Absence> otherCompensatoryRest = Lists.newArrayList();
    contractMonthRecapManager.computeResidualModule(cmr, Optional.<ContractMonthRecap>absent(),
        yearMonthToCompute, new LocalDate().minusDays(1), otherCompensatoryRest, Optional.absent());

    cmr.save();

    contract.getValue().save();
    return cmr;

  }

}