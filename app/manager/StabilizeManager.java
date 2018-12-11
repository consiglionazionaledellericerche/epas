package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperPerson;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Contract;
import models.absences.Absence;
import models.absences.AbsenceType;
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
  
  @Inject
  public StabilizeManager(AbsenceDao absenceDao, AbsenceManager absenceManager, 
      AbsenceService absenceService, PersonDayManager personDayManager, 
      ConsistencyManager consistencyManager, AbsenceTypeDao absenceTypeDao) {
    this.absenceDao = absenceDao;
    this.absenceManager = absenceManager;
    this.absenceService = absenceService;
    this.personDayManager = personDayManager;
    this.consistencyManager = consistencyManager;
    this.absenceTypeDao = absenceTypeDao;
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
      if (!map.containsKey(abs.absenceType.code)) {
        dates = Lists.newArrayList();                  
      } else {
        dates = map.get(abs.absenceType.code);                  
      }
      dates.add(abs.personDay.date);
      map.put(abs.absenceType.code, dates);
      int removed = absenceManager.removeAbsencesInPeriod(contract.person, 
          abs.personDay.date, abs.personDay.date, abs.absenceType);
      if (removed == 0) {
        log.warn("Non è stata rimossa l'assenza {} del giorno {} per {}", 
            abs.absenceType.code, abs.personDay.date, contract.person);
      }
    }
    return map;
  }
  
  private void putAbsencesInNewContract(Contract contract, Map<String, List<LocalDate>> map) {
    
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
    
    Optional<Contract> contract = wrPerson.getCurrentContract();
    if (contract.isPresent()) {
      //prima di terminare il contratto devo recuperare tutte le assenze dal 26 dicembre in poi sul 
      //contratto, salvarle in una mappa e poi eliminarle dal contratto
      LocalDate lastDayBeforeNewContract = new LocalDate(27,12,2018);
      Map<String, List<LocalDate>> absencesToRecreate = 
          checkAbsenceInContract(contract.get(), lastDayBeforeNewContract);
      contract.get().endContract = lastDayBeforeNewContract;
      contract.get().save();
      //Creo il nuovo contratto con l'inizializzazione
      Contract newContract = new Contract();
      newContract.beginDate = lastDayBeforeNewContract;
      newContract.onCertificate = true;
      newContract.person = wrPerson.getValue();
      newContract.sourceDateMealTicket = lastDayBeforeNewContract;
      newContract.sourceDateResidual = lastDayBeforeNewContract;
      newContract.sourceDateVacation = lastDayBeforeNewContract;
      newContract.sourcePermissionUsed = permessi;
      newContract.sourceRemainingMealTicket = buoniPasto;
      newContract.sourceRemainingMinutesCurrentYear = residuoOrario;
      newContract.sourceVacationCurrentYearUsed = ferieAnnoPresente;
      newContract.sourceVacationLastYearUsed = ferieAnnoPassato;
      newContract.save();
      log.info("Creato nuovo contratto a {} a partire dalla data {}", 
          wrPerson.getValue().fullName(), lastDayBeforeNewContract);
      //Riposiziono le assenze precedentemente recuperate
      for (Map.Entry<String, List<LocalDate>> entry : absencesToRecreate.entrySet()) {
        Optional<AbsenceType> type = absenceTypeDao.getAbsenceTypeByCode(entry.getKey());
        for (LocalDate date : entry.getValue()) {
          InsertReport insertReport = absenceService.insert(wrPerson.getValue(), groupAbsenceType, date, date,
              type, justifiedType, null, null, false, absenceManager);
        }
      }
      consistencyManager.updatePersonSituation(wrPerson.getValue().id, lastDayBeforeNewContract);
    }
  }
}
