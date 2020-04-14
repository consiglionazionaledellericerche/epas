package manager.service.contracts;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.common.base.Optional;
import dao.AbsenceDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.PersonDayManager;
import manager.recaps.recomputation.RecomputeRecap;
import manager.services.absences.AbsenceCertificationService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.WorkingTimeType;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.definitions.DefaultAbsenceType;
import org.joda.time.LocalDate;
import play.data.validation.Validation;
import play.db.jpa.JPA;


@Slf4j
public class ContractService {

  private final AbsenceDao absenceDao;
  private final PersonDayManager personDayManager;
  private final AbsenceCertificationService absenceCertificationService;
  private final AbsenceComponentDao absenceComponentDao;
  private final ConsistencyManager consistencyManager;
  private final IWrapperFactory wrapperFactory;
  

  /**
   * Injection.
   * @param absenceDao il dao sulle assenze
   * @param personDayManager il manager del personDay
   * @param absenceCertificationService il servizio di recupero assenze da attestati
   * @param absenceComponentDao il dao sul componente delle assenze
   * @param consistencyManager il manager che fa i conti
   * @param wrapperFactory il wrapperFactory che consente di incapsulare gli oggetti in 
   *     qualcosa di più corposo contenente metodi aggiuntivi
   */
  @Inject
  public ContractService(AbsenceDao absenceDao, PersonDayManager personDayManager,
      AbsenceCertificationService absenceCertificationService, 
      AbsenceComponentDao absenceComponentDao, ConsistencyManager consistencyManager,
      IWrapperFactory wrapperFactory) {
    this.absenceDao = absenceDao;
    this.personDayManager = personDayManager;
    this.absenceCertificationService = absenceCertificationService;
    this.absenceComponentDao = absenceComponentDao;
    this.consistencyManager = consistencyManager;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * La mappa con associazione data-lista tipi di assenza.
   * @param person la persona per cui si recuperano le assenze
   * @param from da quando recuperarle
   * @param to (opzionale) fino a quando recuperarle
   * @return la mappa contenente l'associazione data-lista di tipi assenza.
   */
  public final List<Absence> getAbsencesInContract(Person person, 
      LocalDate from, Optional<LocalDate> to) {
    
    return absenceDao.absenceInPeriod(person, from, to);
    
  }

  /**
   * Cancella le assenze per person da from a to.
   * @param person la persona per cui cancellare le assenze
   * @param from la data da cui cancellare le assenze
   * @param to (opzionale) la data fino a cui cancellare le assenze
   * @return la quantità di assenze cancellate.
   */
  public Long deleteAbsencesInPeriod(Person person, LocalDate from, Optional<LocalDate> to) {
    return absenceDao.deleteAbsencesInPeriod(person, from, to);
  }

  /**
   * Si collega ad Attestati e scarica le assenze dell'anno di updateFrom.
   * Poi le persiste ignorando quelle esistenti.
   * @param person la persona il cui contratto è stato splittato
   * @param updateFrom la data da cui far partire i ricalcoli
   */
  public void saveAbsenceOnNewContract(Person person, LocalDate updateFrom) {
    LocalDate beginYear = updateFrom.monthOfYear().withMinimumValue()
        .dayOfMonth().withMinimumValue();
    List<Absence> absences = absenceCertificationService
        .absencesToPersist(person, updateFrom.getYear());
    absences.sort(Comparator.comparing(Absence::getAbsenceDate));
    for (Absence absence : absences) {
      JPA.em().flush(); //potrebbero esserci dei doppioni, per sicurezza flusho a ogni assenza.
      if (!absenceComponentDao
          .findAbsences(person, absence.getAbsenceDate(), absence.absenceType.code).isEmpty()) {
        continue;
      }

      PersonDay personDay = personDayManager
          .getOrCreateAndPersistPersonDay(person, absence.getAbsenceDate());
      absence.personDay = personDay;
      personDay.absences.add(absence);
      absence.save();
      personDay.save();
      if (absence.absenceType.code.equals(DefaultAbsenceType.A_91.certificationCode) 
          && !absence.getAbsenceDate().isBefore(beginYear)) {
        IWrapperPerson wrPerson = wrapperFactory.create(person);
        Optional<Contract> contract = wrPerson.getCurrentContract();
        if (contract.isPresent()) {
          contract.get().sourceDateRecoveryDay = absence.getAbsenceDate();
          contract.get().sourceRecoveryDayUsed++;
          contract.get().save();
        }
      }
      if (personDay.date.isBefore(updateFrom)) {
        updateFrom = personDay.date;
      }
    }

    JPA.em().flush();
    consistencyManager.updatePersonSituation(person.id, updateFrom);
  }
  
  /**
   * Ritorna il nuovo contratto con i parametri passati.
   * @param person la persona di cui si sta creando il contratto
   * @param dateToSplit la data da cui ripartire col nuovo contratto
   * @param wtt l'orario di lavoro
   * @param previousInterval l'intervallo precedente
   * @return il nuovo contratto con le informazioni di base per crearlo.
   */
  public Contract createNewContract(Person person, LocalDate dateToSplit, 
      Optional<WorkingTimeType> wtt, DateInterval previousInterval) {
    Contract newContract = new Contract();
    newContract.person = person;
    newContract.beginDate = dateToSplit;
    newContract.endDate = !DateUtility.isInfinity(previousInterval.getEnd()) 
        ? previousInterval.getEnd() : null;
    return newContract;
  }
  
}
