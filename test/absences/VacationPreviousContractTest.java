/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package absences;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.ContractDao;
import dao.absences.AbsenceComponentDao;
import db.h2support.H2Examples;
import db.h2support.base.H2WorkingTimeTypeSupport;
import db.h2support.base.WorkingTimeTypeDefinitions.WorkingDefinition;
import java.util.List;
import manager.ContractManager;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Contract;
import models.Person;
import models.VacationPeriod;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;
import models.enumerate.VacationCode;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Ferie di un contratto collegato al precedente, quando i piani ferie del contratto precedente
 * vanno oltre la sua data di fine (contratto creato a tempo indeterminato e poi chiuso senza
 * accorciare i piani ferie). Prima della correzione l'inserimento falliva con l'errore
 * critico TwoPeriods.
 */
@StaticInject
public class VacationPreviousContractTest extends UnitTest {

  private static final LocalDate PREVIOUS_BEGIN = new LocalDate(2023, 9, 18);
  private static final LocalDate PREVIOUS_END = new LocalDate(2024, 9, 17);
  private static final LocalDate CURRENT_BEGIN = new LocalDate(2024, 9, 18);
  private static final LocalDate CURRENT_END = new LocalDate(2027, 1, 31);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static H2WorkingTimeTypeSupport h2WorkingTimeTypeSupport;
  @Inject
  private static ContractManager contractManager;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static ContractDao contractDao;

  /**
   * Crea il contratto precedente a tempo indeterminato (piani ferie 26+4 per tre anni e 28+4
   * senza fine), lo chiude senza toccare i piani ferie e crea il contratto attuale collegato.
   */
  private Contract buildCurrentContract() {
    Contract current = buildUnlinkedCurrentContract();
    current.setPreviousContract(contractDao.getPreviousContract(current).get());
    current.save();
    current.getPerson().refresh();
    return current;
  }

  /**
   * Come {@link #buildCurrentContract()} ma senza collegare il contratto attuale al precedente.
   */
  private Contract buildUnlinkedCurrentContract() {
    Person person = h2Examples.normalEmployee(PREVIOUS_BEGIN, Optional.absent());
    Contract previous = person.getContracts().get(0);
    previous.setEndDate(PREVIOUS_END);
    previous.save();

    Contract current = new Contract();
    current.setPerson(person);
    current.setBeginDate(CURRENT_BEGIN);
    current.setEndDate(CURRENT_END);
    assertTrue(contractManager.properContractCreate(current,
        Optional.of(h2WorkingTimeTypeSupport.getWorkingTimeType(WorkingDefinition.Normal)),
        false));
    person.refresh();
    return current;
  }

  @Test
  public void extendedVacationPeriodsAreTrimmedToPreviousContractEnd() {
    Contract current = buildCurrentContract();

    List<VacationPeriod> extended = current.getExtendedVacationPeriods();
    for (VacationPeriod vp : extended) {
      if (vp.getContract().equals(current.getPreviousContract())) {
        assertFalse(vp.getBeginDate().isAfter(PREVIOUS_END));
        assertNotNull(vp.getEndDate());
        assertFalse(vp.getEndDate().isAfter(PREVIOUS_END));
      }
    }
    // 26+4 del contratto attuale + 26+4 del precedente (il 28+4 del precedente è scartato)
    assertEquals(2, extended.size());

    // i piani ferie persistiti del contratto precedente non vengono modificati
    boolean openEnded28 = false;
    for (VacationPeriod vp : current.getPreviousContract().getVacationPeriods()) {
      if (vp.getVacationCode() == VacationCode.CODE_28_4 && vp.getEndDate() == null) {
        openEnded28 = true;
      }
    }
    assertTrue(openEnded28);
  }

  @Test
  public void vacationInsertWithDirtyPreviousContractPeriods() {
    absenceService.enumInitializator();
    Contract current = buildCurrentContract();

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    JustifiedType allDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    AbsenceType absenceType = absenceComponentDao
        .absenceTypeByCode(DefaultAbsenceType.A_32.getCode()).get();

    InsertReport insertReport = absenceService.insert(current.getPerson(), vacationGroup,
        new LocalDate(2026, 12, 10), null, absenceType, allDay, null, null, false, null);

    assertTrue(insertReport.criticalErrors.isEmpty());
    assertEquals(1, insertReport.howManySuccess());
  }

  @Test
  public void mergeVacationPeriodsIgnoresPeriodsAfterPreviousContractEnd() {
    Contract current = buildUnlinkedCurrentContract();

    assertTrue(contractManager.applyPreviousContractLink(current, true));
    current = contractDao.getContractById(current.getId());
    current.refresh();

    // il 28+4 parte dopo tre anni dall'inizio del 26+4 del contratto precedente, non dal primo
    // giorno del contratto attuale (il 28+4 del precedente inizia dopo la sua fine)
    LocalDate expected28 = PREVIOUS_BEGIN.plusYears(3);
    VacationPeriod twentysix = null;
    VacationPeriod twentyeight = null;
    for (VacationPeriod vp : current.getVacationPeriods()) {
      if (vp.getVacationCode() == VacationCode.CODE_26_4) {
        twentysix = vp;
      } else if (vp.getVacationCode() == VacationCode.CODE_28_4) {
        twentyeight = vp;
      }
    }
    assertNotNull(twentysix);
    assertEquals(CURRENT_BEGIN, twentysix.getBeginDate());
    assertEquals(expected28.minusDays(1), twentysix.getEndDate());
    assertNotNull(twentyeight);
    assertEquals(expected28, twentyeight.getBeginDate());
    assertEquals(CURRENT_END, twentyeight.getEndDate());
  }

  @Test
  public void fixVacationPeriodsAfterContractEnd() {
    Contract current = buildCurrentContract();
    Contract previous = current.getPreviousContract();
    assertTrue(contractDao.getContractsWithVacationPeriodsAfterEnd().contains(previous));

    assertTrue(contractManager.fixVacationPeriodsAfterContractEnd() >= 1);

    previous.refresh();
    assertEquals(1, previous.getVacationPeriods().size());
    VacationPeriod vp = previous.getVacationPeriods().get(0);
    assertEquals(VacationCode.CODE_26_4, vp.getVacationCode());
    assertEquals(PREVIOUS_BEGIN, vp.getBeginDate());
    assertEquals(PREVIOUS_END, vp.getEndDate());

    // eseguito di nuovo non trova più il contratto
    assertFalse(contractDao.getContractsWithVacationPeriodsAfterEnd().contains(previous));
  }
}
