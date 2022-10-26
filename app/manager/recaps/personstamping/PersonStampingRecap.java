/*
 * Copyright (C) 2021 Consiglio Nazionale delle Ricerche
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package manager.recaps.personstamping;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import controllers.Resecure;
import controllers.Security;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
import manager.PersonManager;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.AbsenceToRecoverDto;
import models.enumerate.StampTypes;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.mvc.Scope.Session;

/**
 * Oggetto che modella il contenuto della vista contenente il tabellone timbrature. Gerarchia:
 * PersonStampingRecap (tabella mese) -> PersonStampingDayRecap (riga giorno) -> StampingTemplate
 * (singola timbratura)
 *
 * @author Alessandro Martelli
 */
@Slf4j
public class PersonStampingRecap {

  private static final int MIN_IN_OUT_COLUMN = 2;

  public Person person;
  public int year;
  public int month;

  public boolean currentMonth = false;

  // Informazioni sui permessi della persona
  public boolean canEditStampings = false;

  // Informazioni sul mese
  public int numberOfCompensatoryRestUntilToday = 0;
  public int basedWorkingDays = 0;
  public int totalWorkingTime = 0;
  public int positiveResidualInMonth = 0;

  // I riepiloghi di ogni giorno
  public List<PersonStampingDayRecap> daysRecap = Lists.newArrayList();

  // I riepiloghi codici sul mese
  public Set<StampModificationType> stampModificationTypeSet = Sets.newHashSet();
  public Set<StampTypes> stampTypeSet = Sets.newHashSet();
  public Map<AbsenceType, Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();
  public List<Absence> absenceList = Lists.newArrayList();

  // I riepiloghi mensili (uno per ogni contratto attivo nel mese)
  public List<IWrapperContractMonthRecap> contractMonths = Lists.newArrayList();

  // Le informazioni su eventuali assenze a recupero (es.: 91CE)
  public boolean absenceToRecoverYet = false;
  public List<AbsenceToRecoverDto> absencesToRecoverList = Lists.newArrayList();

  // Template
  public int numberOfInOut = 0;

  public int getNumberOfMealTicketToUse() {
    return contractMonths.stream().map(cm -> cm.getValue().getBuoniPastoUsatiNelMese())
        .reduce(Integer::sum).orElse(0);
  }

  /**
   * Costruisce l'oggetto contenente tutte le informazioni da renderizzare nella pagina tabellone
   * timbrature.
   *
   * @param personDayManager personDayManager
   * @param personDayDao personDayDao
   * @param personManager personManager
   * @param stampingDayRecapFactory stampingDayRecapFactory
   * @param wrapperFactory wrapperFactory
   * @param year year
   * @param month month
   * @param person person
   * @param considerExitingNow se considerare nel calcolo l'uscita in questo momento
   */
  public PersonStampingRecap(PersonDayManager personDayManager, PersonDayDao personDayDao,
      PersonManager personManager, PersonStampingDayRecapFactory stampingDayRecapFactory,
      IWrapperFactory wrapperFactory, int year, int month, Person person,
      boolean considerExitingNow) {

    // DATI DELLA PERSONA
    if (Session.current() != null && Security.getUser() != null && Security.getUser().isPresent()) {
      canEditStampings = Resecure.check("Stampings.edit", null);
    }
    final long start = System.currentTimeMillis();
    log.trace("inizio creazione nuovo PersonStampingRecap. Person = {}, year = {}, month = {}",
        person.getFullname(), year, month);
    this.person = person;
    this.month = month;
    this.year = year;

    if (new YearMonth(year, month).equals(new YearMonth(LocalDate.now()))) {
      this.currentMonth = true;
    }

    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate end = begin.dayOfMonth().withMaximumValue();


    List<PersonDay> personDays =
        personDayDao.getPersonDayInPeriod(person, begin, Optional.fromNullable(end));


    this.numberOfInOut =
        Math.max(MIN_IN_OUT_COLUMN, personDayManager.getMaximumCoupleOfStampings(personDays));

    // ******************************************************************************************
    // DATI MENSILI
    // ******************************************************************************************
    List<Contract> monthContracts =
        wrapperFactory.create(person).orderedMonthContracts(year, month);

    for (Contract contract : monthContracts) {
      Optional<ContractMonthRecap> cmr =
          wrapperFactory.create(contract).getContractMonthRecap(new YearMonth(year, month));

      if (cmr.isPresent()) {

        this.contractMonths.add(wrapperFactory.create(cmr.get()));
      }
    }

    // ******************************************************************************************
    // DATI SINGOLI GIORNI
    // ******************************************************************************************

    // Lista person day contente tutti i giorni fisici del mese
    List<PersonDay> totalPersonDays =
        personDayManager.getTotalPersonDayInMonth(personDays, person, year, month);

    LocalDate today = LocalDate.now();

    long startDayRecaps = System.currentTimeMillis();
    for (PersonDay pd : totalPersonDays) {
      personDayManager.setValidPairStampings(pd.stampings);

      PersonStampingDayRecap dayRecap = stampingDayRecapFactory.create(pd, this.numberOfInOut,
          considerExitingNow, Optional.fromNullable(monthContracts));
      this.daysRecap.add(dayRecap);

      this.totalWorkingTime += pd.timeAtWork;

      if (pd.stampModificationType != null && !pd.date.isAfter(today)) {

        stampModificationTypeSet.add(pd.stampModificationType);
      }

      for (Stamping stamp : pd.stampings) {
        if (stamp.stampType != null) {
          stampTypeSet.add(stamp.stampType);
        }
        if (stamp.markedByAdmin) {
          StampModificationType smt = stampingDayRecapFactory.stampTypeManager
              .getStampMofificationType(StampModificationTypeCode.MARKED_BY_ADMIN);
          stampModificationTypeSet.add(smt);
        }
        if (stamp.markedByEmployee) {
          StampModificationType smt = stampingDayRecapFactory.stampTypeManager
              .getStampMofificationType(StampModificationTypeCode.MARKED_BY_EMPLOYEE);
          stampModificationTypeSet.add(smt);
        }
        if (stamp.markedByTelework) {
          StampModificationType smt = stampingDayRecapFactory.stampTypeManager
              .getStampMofificationType(StampModificationTypeCode.MARKED_BY_TELEWORK);
          stampModificationTypeSet.add(smt);
        }
        if (stamp.stampModificationType != null) {
          if (stamp.stampModificationType.code
              .equals(StampModificationTypeCode.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getCode())) {
            stampModificationTypeSet.add(stamp.stampModificationType);
          }
        }
      }
    }
    log.trace("terminato calcolo dayRecaps in {} ms", System.currentTimeMillis() - startDayRecaps);

    // Riattivarlo...
    this.positiveResidualInMonth = 0;
    // this.positiveResidualInMonth = wrapperFactory.create(person)
    // .getPositiveResidualInMonth(this.year, this.month);

    this.numberOfCompensatoryRestUntilToday =
        personManager.numberOfCompensatoryRestUntilToday(person, year, month);

    this.basedWorkingDays = personManager.basedWorkingDays(personDays, monthContracts, end);
    this.absenceCodeMap = personManager.countAbsenceCodes(totalPersonDays);
    this.absenceList = personManager.listAbsenceCodes(totalPersonDays);
    LocalDate from = person.office.getBeginDate();
    List<Absence> list = personManager.absencesToRecover(person, from, LocalDate.now(),
        JustifiedTypeName.recover_time);
    this.absencesToRecoverList = personManager.dtoList(list);

    if (list.isEmpty()) {
      this.absenceToRecoverYet = false;
    } else {
      this.absenceToRecoverYet = true;
    }
    log.debug("fine creazione nuovo PersonStampingRecap in {} ms. Person = {}, year = {}, "
        + "month = {}", System.currentTimeMillis() - start, person.getFullname(), year, month);
  }
  
}