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

package models.flows;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.CompetenceRequestType;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.test.UnitTest;

/**
 * Verifica che lo stato del flusso delle richieste non venga mai legato dai parametri della
 * richiesta HTTP.
 */
public class FlowRequestBindingTest extends UnitTest {

  private static final String NOW = "01/01/2026 10:00:00";

  private static Object bind(String name, Class<?> clazz, Map<String, String[]> params) {
    return Binder.bind(ParamNode.convert(params), name, clazz, clazz, null);
  }

  @Test
  public void absenceRequestFlowStateIsNotBound() {
    Map<String, String[]> params = ImmutableMap.<String, String[]>builder()
        .put("absenceRequest.type", new String[] {AbsenceRequestType.VACATION_REQUEST.name()})
        .put("absenceRequest.note", new String[] {"nota"})
        .put("absenceRequest.startAt", new String[] {NOW})
        .put("absenceRequest.managerApproved", new String[] {NOW})
        .put("absenceRequest.administrativeApproved", new String[] {NOW})
        .put("absenceRequest.officeHeadApproved", new String[] {NOW})
        .put("absenceRequest.managerApprovalRequired", new String[] {"false"})
        .put("absenceRequest.administrativeApprovalRequired", new String[] {"false"})
        .put("absenceRequest.officeHeadApprovalRequired", new String[] {"false"})
        .put("absenceRequest.officeHeadApprovalForManagerRequired", new String[] {"false"})
        .put("absenceRequest.flowStarted", new String[] {"true"})
        .put("absenceRequest.flowEnded", new String[] {"true"})
        .build();

    AbsenceRequest absenceRequest =
        (AbsenceRequest) bind("absenceRequest", AbsenceRequest.class, params);

    assertEquals(AbsenceRequestType.VACATION_REQUEST, absenceRequest.getType());
    assertEquals("nota", absenceRequest.getNote());
    assertEquals(new LocalDateTime(2026, 1, 1, 10, 0), absenceRequest.getStartAt());
    assertNull(absenceRequest.getManagerApproved());
    assertNull(absenceRequest.getAdministrativeApproved());
    assertNull(absenceRequest.getOfficeHeadApproved());
    assertTrue(absenceRequest.isManagerApprovalRequired());
    assertTrue(absenceRequest.isAdministrativeApprovalRequired());
    assertTrue(absenceRequest.isOfficeHeadApprovalRequired());
    assertTrue(absenceRequest.isOfficeHeadApprovalForManagerRequired());
    assertFalse(absenceRequest.isFlowStarted());
    assertFalse(absenceRequest.isFlowEnded());
    assertFalse(absenceRequest.isFullyApproved());
  }

  @Test
  public void competenceRequestFlowStateAndValueAreNotBound() {
    Map<String, String[]> params = ImmutableMap.<String, String[]>builder()
        .put("competenceRequest.type",
            new String[] {CompetenceRequestType.OVERTIME_REQUEST.name()})
        .put("competenceRequest.note", new String[] {"nota"})
        .put("competenceRequest.startAt", new String[] {NOW})
        .put("competenceRequest.valueRequested", new String[] {"1"})
        .put("competenceRequest.value", new String[] {"200"})
        .put("competenceRequest.firstApproved", new String[] {NOW})
        .put("competenceRequest.employeeApproved", new String[] {NOW})
        .put("competenceRequest.managerApproved", new String[] {NOW})
        .put("competenceRequest.officeHeadApproved", new String[] {NOW})
        .put("competenceRequest.employeeApprovalRequired", new String[] {"false"})
        .put("competenceRequest.managerApprovalRequired", new String[] {"false"})
        .put("competenceRequest.officeHeadApprovalRequired", new String[] {"false"})
        .put("competenceRequest.flowStarted", new String[] {"true"})
        .put("competenceRequest.flowEnded", new String[] {"true"})
        .build();

    CompetenceRequest competenceRequest =
        (CompetenceRequest) bind("competenceRequest", CompetenceRequest.class, params);

    assertEquals(CompetenceRequestType.OVERTIME_REQUEST, competenceRequest.getType());
    assertEquals("nota", competenceRequest.getNote());
    assertEquals(new LocalDateTime(2026, 1, 1, 10, 0), competenceRequest.getStartAt());
    assertEquals(Integer.valueOf(1), competenceRequest.getValueRequested());
    assertNull(competenceRequest.getValue());
    assertNull(competenceRequest.getFirstApproved());
    assertNull(competenceRequest.getEmployeeApproved());
    assertNull(competenceRequest.getManagerApproved());
    assertNull(competenceRequest.getOfficeHeadApproved());
    assertTrue(competenceRequest.isEmployeeApprovalRequired());
    assertTrue(competenceRequest.isManagerApprovalRequired());
    assertTrue(competenceRequest.isOfficeHeadApprovalRequired());
    assertFalse(competenceRequest.isFlowStarted());
    assertFalse(competenceRequest.isFlowEnded());
    assertFalse(competenceRequest.isFullyApproved());
  }
}
