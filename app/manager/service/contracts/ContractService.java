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

package manager.service.contracts;

import com.google.common.base.Optional;
import dao.AbsenceDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.PersonDayManager;
import manager.services.absences.AbsenceCertificationService;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.WorkingTimeType;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.definitions.DefaultAbsenceType;
import org.joda.time.LocalDate;
import play.db.jpa.JPA;


/**
 * Service per i Contratti.
 */
@Slf4j
public class ContractService {

  private final AbsenceDao absenceDao;
  private final PersonDayManager personDayManager;
  private final AbsenceCertificationService absenceCertificationService;
  private final ConsistencyManager consistencyManager;
  private final IWrapperFactory wrapperFactory;
  private final AbsenceService absenceService;
  private final AbsenceManager absenceManager;


  /**
   * Injection.
   *
   * @param absenceDao il dao sulle assenze
   * @param personDayManager il manager del personDay
   * @param absenceCertificationService il servizio di recupero assenze da attestati
   * @param consistencyManager il manager che fa i conti
   * @param wrapperFactory il wrapperFactory che consente di incapsulare gli oggetti in 
   *     qualcosa di più corposo contenente metodi aggiuntivi
   */
  @Inject
  public ContractService(AbsenceDao absenceDao, PersonDayManager personDayManager,
      AbsenceCertificationService absenceCertificationService, 
      ConsistencyManager consistencyManager,
      IWrapperFactory wrapperFactory, AbsenceService absenceService, 
      AbsenceManager absenceManager) {
    this.absenceDao = absenceDao;
    this.personDayManager = personDayManager;
    this.absenceCertificationService = absenceCertificationService;
    this.consistencyManager = consistencyManager;
    this.wrapperFactory = wrapperFactory;
    this.absenceService = absenceService;
    this.absenceManager = absenceManager;
  }

  /**
   * La mappa con associazione data-lista tipi di assenza.
   *
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
   * Si collega ad Attestati e scarica le assenze dell'anno di updateFrom.
   * Poi le persiste ignorando quelle esistenti.
   *
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
   *
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

  /**
   * Ripristina le assenze salvate in precedenza sul nuovo contratto.
   *
   * @param absences la lista di assenze da ripristinare
   */
  public void resetAbsences(List<Absence> absences) {
    GroupAbsenceType groupAbsenceType = null;
    AbsenceType absenceType = null;
    JustifiedType justifiedType;
    for (Absence abs : absences) {
      if (abs.absenceType.defaultTakableGroup() == null) {
        continue;
      }
      justifiedType = abs.justifiedType;
      Integer hours = abs.justifiedMinutes != null ? abs.justifiedMinutes / 60 : null;
      Integer minutes = abs.justifiedMinutes != null ? abs.justifiedMinutes % 60 : null;
      InsertReport insertReport = absenceService.insert(abs.personDay.person, 
          abs.absenceType.defaultTakableGroup(), 
          abs.personDay.date, abs.personDay.date,
          absenceType, justifiedType, hours, minutes, false, absenceManager);
      if (insertReport.absencesToPersist.isEmpty()) {
        insertReport = absenceService.insert(abs.personDay.person, 
            abs.absenceType.defaultTakableGroup(), 
            abs.personDay.date, abs.personDay.date,
            abs.absenceType, justifiedType, hours, minutes, true, absenceManager);
      }

      absenceManager.saveAbsences(insertReport, abs.personDay.person, abs.personDay.date, null, 
          justifiedType, groupAbsenceType);
      log.info("Salvata assenza {} nel giorno {} per {}", abs.absenceType.code, 
          abs.personDay.date, abs.personDay.person);
    }

  }

}