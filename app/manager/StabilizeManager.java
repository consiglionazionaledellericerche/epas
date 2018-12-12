package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import controllers.Security;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.WorkingTimeTypeDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.Web;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.recomputation.RecomputeRecap;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.model.VacationSituation;
import models.Contract;
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
import play.data.validation.Validation;
import play.db.jpa.JPA;
import org.joda.time.LocalDate;
import org.testng.collections.Maps;


@Slf4j
public class StabilizeManager {

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
  

  @Inject
  public StabilizeManager(AbsenceDao absenceDao, AbsenceManager absenceManager, 
      AbsenceService absenceService, PersonDayManager personDayManager, 
      ConsistencyManager consistencyManager, AbsenceTypeDao absenceTypeDao,
      AbsenceComponentDao absComponentDao, ContractManager contractManager,
      WorkingTimeTypeDao wttDao, IWrapperFactory wrapperFactory, PeriodManager periodManager) {
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
    List<String> codesToSave = Lists.newArrayList("32", "94", "31", "37");
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
        groupAbsenceType =
            absComponentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name());

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
            consistencyManager.updatePersonSituation(wrPerson.getValue().id, date);
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
      LocalDate lastDayBeforeNewContract = new LocalDate(2018,12,27);
      Map<String, List<LocalDate>> absencesToRecreate = 
          checkAbsenceInContract(contract.get(), lastDayBeforeNewContract);
      Contract newContract = createNewContract(contract, lastDayBeforeNewContract, wrPerson);
      initializeNewContract(newContract, permessi, buoniPasto, residuoOrario, 
          ferieAnnoPresente, ferieAnnoPassato, lastDayBeforeNewContract);
      log.info("Inizializzato il nuovo contratto id {}", wrPerson.getValue().fullName());
      //Riposiziono le assenze precedentemente recuperate
      putAbsencesInNewContract(wrPerson, absencesToRecreate);      
    }
  }

  private Contract createNewContract(Optional<Contract> contract, 
      LocalDate lastDayBeforeNewContract, IWrapperPerson wrPerson) {

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
    //Creo il nuovo contratto con l'inizializzazione
    Contract newContract = new Contract();
    newContract.beginDate = lastDayBeforeNewContract;
    newContract.onCertificate = true;
    newContract.person = wrPerson.getValue();

    WorkingTimeType wtt = wttDao.workingTypeTypeByDescription("Normale", Optional.absent());
    contractManager.properContractCreate(newContract, Optional.fromNullable(wtt), true);
    return newContract;
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
  public void initializeNewContract(Contract newContract, Integer permessi, 
      Integer buoniPasto, Integer residuoOrario, Integer ferieAnnoPresente, 
      Integer ferieAnnoPassato, LocalDate lastDayBeforeNewContract) {

    contractManager.setSourceContractProperly(newContract);
    GroupAbsenceType vacationGroup = absComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    VacationSituation vacationSituation = 
        absenceService.buildVacationSituation(newContract, 
        lastDayBeforeNewContract.getYear(), vacationGroup, Optional.absent(), true);
    newContract.sourceDateMealTicket = lastDayBeforeNewContract;
    newContract.sourceDateResidual = lastDayBeforeNewContract;
    newContract.sourceDateVacation = lastDayBeforeNewContract;
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
}
