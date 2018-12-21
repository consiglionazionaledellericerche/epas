package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import controllers.Security;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonDayDao;
import dao.WorkingTimeTypeDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.Web;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.recomputation.RecomputeRecap;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.model.VacationSituation;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultGroup;
import models.base.IPropertyInPeriod;
import models.enumerate.VacationCode;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.libs.F.Promise;
import org.joda.time.LocalDate;
import org.testng.collections.Maps;


@Slf4j
public class StabilizeManager {

  //Data della stabilizzazione: 27/12/2018
  public static final LocalDate firstDayNewContract = new LocalDate(2018,12,27);
  private final List<String> codesToSave = Lists.newArrayList("91", "32", "94", "31", "37");

  private final AbsenceDao absenceDao;
  private final AbsenceManager absenceManager;
  private final AbsenceService absenceService;
  private final PersonDayManager personDayManager;
  private final ConsistencyManager consistencyManager;
  private final AbsenceTypeDao absenceTypeDao;
  private final AbsenceComponentDao absComponentDao;
  private final ContractManager contractManager;
  private final WorkingTimeTypeDao wttDao;
  private final IWrapperFactory wrapperFactory;
  private final PeriodManager periodManager;
  private final PersonDayDao personDayDao;


  @Inject
  public StabilizeManager(AbsenceDao absenceDao, AbsenceManager absenceManager, 
      AbsenceService absenceService, PersonDayManager personDayManager, 
      ConsistencyManager consistencyManager, AbsenceTypeDao absenceTypeDao,
      AbsenceComponentDao absComponentDao, ContractManager contractManager,
      WorkingTimeTypeDao wttDao, IWrapperFactory wrapperFactory, PeriodManager periodManager,
      PersonDayDao personDayDao) {
    this.absenceDao = absenceDao;
    this.absenceManager = absenceManager;
    this.absenceService = absenceService;
    this.personDayManager = personDayManager;
    this.consistencyManager = consistencyManager;
    this.absenceTypeDao = absenceTypeDao;
    this.absComponentDao = absComponentDao;
    this.contractManager = contractManager;
    this.wttDao = wttDao;
    this.wrapperFactory = wrapperFactory;
    this.periodManager = periodManager;
    this.personDayDao = personDayDao;
  }

  /**
   * Metodo che mette da parte le assenze fatte nei giorni che vanno dalla data di inizio del nuovo 
   * contratto alla fine del vecchio contratto.
   * @param contract il contratto su cui ricercare le assenze
   * @param lastDayBeforeNewContract la data da cui andare a ricercare le assenze
   * @return la lista delle assenze nel periodo da lastDayBeforeNewContract a endDate.
   */
  private Map<String, List<LocalDate>> checkAbsenceInContract(Contract contract, 
      LocalDate lastDayBeforeNewContract) {
    
    Map<String, List<LocalDate>> map = Maps.newHashMap();
    
    List<LocalDate> dates = null;
    List<Absence> absences = absenceDao
        .getAbsencesInPeriod(Optional.fromNullable(contract.person), 
            lastDayBeforeNewContract, Optional.fromNullable(contract.endDate), false);
    for (Absence abs : absences) {
      int removed = absenceManager.removeAbsencesInPeriod(contract.person, 
          abs.personDay.date, abs.personDay.date, abs.absenceType);
      if (removed == 0) {
        log.warn("Non è stata rimossa l'assenza {} del giorno {} per {}", 
            abs.absenceType.code, abs.personDay.date, contract.person);
      }
      if (!codesToSave.contains(abs.absenceType.code)) {
        continue;
      }
      if (!map.containsKey(abs.absenceType.code)) {
        dates = Lists.newArrayList();                  
      } else {
        dates = map.get(abs.absenceType.code);                  
      }
      dates.add(abs.personDay.date);
      map.put(abs.absenceType.code, dates);

    }
    return map;
  }

  /**
   * Metodo che riposiziona le assenze sul nuovo contratto T.I.
   * @param wrPerson il wrapper contenente le informazioni della persona
   * @param map la mappa contenente i codici di assenza e la lista di date in cui mettere quei 
   *     codici
   */
  private void putAbsencesInNewContract(IWrapperPerson wrPerson, Map<String, List<LocalDate>> map) {
    if (map.isEmpty()) {
      return;
    }
    Optional<GroupAbsenceType> groupAbsenceType = null;
    for (Map.Entry<String, List<LocalDate>> entry : map.entrySet()) {
      Optional<AbsenceType> type = absenceTypeDao.getAbsenceTypeByCode(entry.getKey());
      if (type.isPresent()) {
        JustifiedType justifiedType = 
            absComponentDao.getOrBuildJustifiedType(JustifiedTypeName.all_day);
        if (type.get().code.equals("91")) {
          groupAbsenceType =
              absComponentDao.groupAbsenceTypeByName(DefaultGroup.RIPOSI_CNR.name());
        } else {
          groupAbsenceType =
              absComponentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name());
        }

        for (LocalDate date : entry.getValue()) {

          InsertReport insertReport = absenceService.insert(wrPerson.getValue(), 
              groupAbsenceType.get(), date, date, type.get(), justifiedType, null, 
              null, false, absenceManager);
          if (!insertReport.absencesToPersist.isEmpty()) {
            for (Absence absence : insertReport.absencesToPersist) {
              PersonDay personDay = personDayManager
                  .getOrCreateAndPersistPersonDay(wrPerson.getValue(), absence.getAbsenceDate());
              absence.personDay = personDay;
              personDay.absences.add(absence);
              absence.save();
              personDay.save();
            } 
            JPA.em().flush();
          }
        }
      }

    }
  }

  /**
   * Metodo che chiude il vecchio contratto di uno stabilizzando e crea il nuovo impostando
   * come inizializzazione i dati rilevanti presi dal precedente contratto.
   * @param wrPerson il wrapper della persona da stabilizzare
   * @param residuoOrario il residuo orario dal vecchio contratto
   * @param buoniPasto i buoni pasto dal vecchio contratto
   * @param ferieAnnoPassato le ferie anno passato usate
   * @param ferieAnnoPresente le ferie anno corrente usate
   * @param permessi i permessi legge usati
   */
  public void stabilizePerson(IWrapperPerson wrPerson, Integer residuoOrario, 
      Integer buoniPasto, Integer ferieAnnoPassato, Integer ferieAnnoPresente, Integer permessi) {
    log.debug("eccoci");
    Optional<Contract> contract = wrPerson.getCurrentContract();
    if (contract.isPresent()) {
      //prima di terminare il contratto devo recuperare tutte le assenze dal 26 dicembre in poi sul 
      //contratto, salvarle in una mappa e poi eliminarle dal contratto
      int minutesToAdd = 0;
      
      Map<String, List<LocalDate>> absencesToRecreate = 
          checkAbsenceInContract(contract.get(), firstDayNewContract);
      
      log.info("Rimosse assenze per {}. Assenze: {}", 
          wrPerson.getValue().getFullname(), absencesToRecreate);
      
      //Controllo quando sarebbe avvenuto il cambio di piano ferie
      List<VacationPeriod> vpList = contract.get().vacationPeriods;
      LocalDate beginDate = null;
      for (VacationPeriod vp : vpList) {
        if (vp.vacationCode.name.equals("28+4")) {
          beginDate = vp.beginDate;          
        }
      }
      final VacationCode code = VacationCode.CODE_28_4;
      
      //Creo il nuovo contratto
      Contract newContract = createNewContract(contract, firstDayNewContract, wrPerson);
            
      //Inizializzo il nuovo contratto
      if (absencesToRecreate.containsKey("91")) {
        minutesToAdd = absencesToRecreate.get("91").size() * 432;
      }
      residuoOrario = residuoOrario + minutesToAdd;      
      initializeNewContract(newContract, permessi, buoniPasto, residuoOrario, 
          ferieAnnoPresente, ferieAnnoPassato, firstDayNewContract);
      log.info("Inizializzato il nuovo contratto id {}", wrPerson.getValue().fullName());
      
      //Sistemo il piano ferie al contratto se necessario
      if (beginDate != null) {
        if (beginDate.isBefore(newContract.beginDate)) {
          beginDate = newContract.beginDate;        
        }
        changeVacationPeriod(newContract, code, beginDate);
      }            
      
      //Riposiziono le assenze precedentemente recuperate
      putAbsencesInNewContract(wrPerson, absencesToRecreate);
      
      new Job<Void>() {
        @Override
        public void doJob() {
          consistencyManager.updatePersonSituation(wrPerson.getValue().id, 
              new LocalDate(firstDayNewContract.dayOfMonth().withMinimumValue()));          
        }
      }.afterRequest();

    }
  }

  /**
   * Metodo che crea il nuovo contratto.
   * @param contract il vecchio contratto 
   * @param lastDayBeforeNewContract la data da cui far partire il nuovo contratto
   * @param wrPerson il wrapper della persona
   * @return il nuovo contratto per la persona contenuta nel wrapper.
   */
  private Contract createNewContract(Optional<Contract> contract, 
      LocalDate lastDayBeforeNewContract, IWrapperPerson wrPerson) {

    log.debug("Inizio chiusura contratto attuale e creazione nuovo contratto per {}", 
        wrPerson.getValue().getFullname());
    IWrapperContract wrappedContract = wrapperFactory.create(contract.get());
    // Salvo la situazione precedente
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();

    // Attribuisco il nuovo stato al contratto per effettuare il controllo incrociato


    contract.get().endContract = lastDayBeforeNewContract.minusDays(1);

    DateInterval newInterval = wrappedContract.getContractDatabaseInterval();
    RecomputeRecap recomputeRecap = periodManager.buildTargetRecap(previousInterval, newInterval,
        wrappedContract.initializationMissing());

    if (recomputeRecap.recomputeFrom != null) {
      contractManager.properContractUpdate(contract.get(), recomputeRecap.recomputeFrom, false);
    } else {
      contractManager.properContractUpdate(contract.get(), LocalDate.now(), false);
    }
    log.info("Terminato contratto {} per {}", contract.get(), wrPerson.getValue().getFullname());
    
    //Creo il nuovo contratto con l'inizializzazione
    Contract newContract = new Contract();
    newContract.beginDate = lastDayBeforeNewContract;
    newContract.onCertificate = true;
    newContract.person = wrPerson.getValue();
    newContract.save();
    contractManager.recomputeContract(newContract,
        Optional.fromNullable(lastDayBeforeNewContract), false, false);

    WorkingTimeType wtt = wttDao.workingTypeTypeByDescription("Normale", Optional.absent());
    contractManager.properContractCreate(newContract, Optional.fromNullable(wtt), true);
    log.info("Creato nuovo contratto {} per {}", newContract, wrPerson.getValue().getFullname());
    return newContract;
  }
  
  /**
   * Metodo che modifica i vacationPeriods.
   * @param newContract il nuovo contratto per cui cambiare i vacationPeriods
   * @param code il vacationCode da inserire
   * @param beginDate la data da cui far partire il vacationPeriod
   */
  private void changeVacationPeriod(Contract newContract, VacationCode code, 
      LocalDate beginDate) {    
    
    VacationPeriod vp = new VacationPeriod();
    vp.beginDate = beginDate;
    vp.vacationCode = code;
    vp.endDate = newContract.endDate;
    vp.contract = newContract;
    //vp.merge();
    IWrapperContract wrappedContract = wrapperFactory.create(newContract);
    
    //Modifico il piano ferie

    List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(vp, false);
    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
            Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
            periodRecaps, Optional.fromNullable(newContract.sourceDateResidual));
    recomputeRecap.initMissing = wrappedContract.initializationMissing();
    
    periodManager.updatePeriods(vp, true);
    
  }

  /**
   * Metodo che imposta l'inizializzazione sul nuovo contratto.
   * @param newContract il nuovo contratto da inizializzare
   * @param permessi i permessi legge da inizializzare
   * @param buoniPasto i buoni pasto da inizializzare
   * @param residuoOrario il residuo orario da inizializzare
   * @param ferieAnnoPresente le ferie dell'anno corrente fatte
   * @param ferieAnnoPassato le ferie dell'anno passato fatte
   * @param lastDayBeforeNewContract la data a cui impostare l'inizializzazione
   */
  private void initializeNewContract(Contract newContract, Integer permessi, 
      Integer buoniPasto, Integer residuoOrario, Integer ferieAnnoPresente, 
      Integer ferieAnnoPassato, LocalDate lastDayBeforeNewContract) {
    log.debug("Inizializzo nuovo contratto...");
    contractManager.setSourceContractProperly(newContract);    
    
    newContract.sourceDateMealTicket = lastDayBeforeNewContract;
    newContract.sourceDateResidual = lastDayBeforeNewContract;
    newContract.sourceDateVacation = lastDayBeforeNewContract;
    GroupAbsenceType vacationGroup = absComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    VacationSituation vacationSituation = 
        absenceService.buildVacationSituation(newContract, 
            lastDayBeforeNewContract.getYear(), vacationGroup, Optional.absent(), true);
    newContract.sourcePermissionUsed = vacationSituation.permissionsCached.total - permessi;
    newContract.sourceRemainingMealTicket = buoniPasto;
    newContract.sourceRemainingMinutesCurrentYear = residuoOrario;
    newContract.sourceVacationCurrentYearUsed = 
        vacationSituation.currentYearCached.total - ferieAnnoPresente;
    newContract.sourceVacationLastYearUsed = ferieAnnoPassato;
    newContract.save();

    absenceService.emptyVacationCache(newContract);

    contractManager.properContractUpdate(newContract, lastDayBeforeNewContract, true);
  }
  
  /**
   * Metodo privato per il calcolo delle differenze da aggiungere/rimuovere rispetto 
   *    al residuo calcolato al giorno della stabilizzazione.
   * @param wrPerson il wrapper della persona
   * @return l'aggiustamento del residuo dovuto al giorno in cui viene lanciata la 
   *        procedura di stabilizzazione.
   */
  public int adjustResidual(IWrapperPerson wrPerson) {
    if (!LocalDate.now().isAfter(firstDayNewContract)) {
      return 0;
    } else {
      int adjustedResidual = 0;
      LocalDate temp = firstDayNewContract;
      while (!temp.isAfter(LocalDate.now())) {
        Optional<PersonDay> pd = personDayDao.getPersonDay(wrPerson.getValue(), temp);
        if (pd.isPresent()) {
          adjustedResidual = adjustedResidual + pd.get().difference;
        }
        temp = temp.plusDays(1);
      }      
      
      return adjustedResidual;
    }
  }
}
