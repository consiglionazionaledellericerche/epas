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

  /**
   * Crea il contratto precedente a tempo indeterminato (piani ferie 26+4 per tre anni e 28+4
   * senza fine), lo chiude senza toccare i piani ferie e crea il contratto attuale collegato.
   */
  private Contract buildCurrentContract() {
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
    current.setPreviousContract(previous);
    current.save();
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
}
