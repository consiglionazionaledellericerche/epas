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

package manager.flows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import models.flows.CompetenceRequest;
import models.flows.CompetenceRequestEvent;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import play.data.binding.NoBinding;
import play.test.UnitTest;

/**
 * Verifica che lo stato del flusso e il valore approvato di una nuova richiesta di competenza
 * siano gestiti solo dal sistema.
 */
public class CompetenceRequestManagerTest extends UnitTest {

  private CompetenceRequestManager competenceRequestManager;

  @Before
  public void createManager() {
    competenceRequestManager = new CompetenceRequestManager(null, null, null, null, null, null,
        null, null, null, null, null, null, null, null);
  }

  private CompetenceRequest requestWithFlowState() {
    CompetenceRequest competenceRequest = new CompetenceRequest();
    competenceRequest.setValueRequested(1);
    competenceRequest.setValue(200);
    competenceRequest.setFirstApproved(LocalDateTime.now());
    competenceRequest.setEmployeeApproved(LocalDateTime.now());
    competenceRequest.setManagerApproved(LocalDateTime.now());
    competenceRequest.setOfficeHeadApproved(LocalDateTime.now());
    competenceRequest.setFlowStarted(true);
    competenceRequest.setFlowEnded(true);
    competenceRequest.getEvents().add(CompetenceRequestEvent.builder().build());
    return competenceRequest;
  }

  @Test
  public void newRequestIsNotApproved() {
    CompetenceRequest competenceRequest = requestWithFlowState();
    assertTrue(competenceRequest.isFullyApproved());

    competenceRequestManager.initNewRequest(competenceRequest);

    assertNull(competenceRequest.getFirstApproved());
    assertNull(competenceRequest.getEmployeeApproved());
    assertNull(competenceRequest.getManagerApproved());
    assertNull(competenceRequest.getOfficeHeadApproved());
    assertFalse(competenceRequest.isFlowStarted());
    assertFalse(competenceRequest.isFlowEnded());
    assertTrue(competenceRequest.getEvents().isEmpty());
    assertFalse(competenceRequest.isFullyApproved());
  }

  @Test
  public void valueIsNotTakenFromRequest() {
    CompetenceRequest competenceRequest = requestWithFlowState();

    competenceRequestManager.initNewRequest(competenceRequest);

    assertNull(competenceRequest.getValue());
    assertEquals(Integer.valueOf(1), competenceRequest.getValueRequested());
  }

  @Test
  public void newRequestDoesNotCompleteFlow() {
    CompetenceRequest competenceRequest = requestWithFlowState();
    competenceRequest.setFlowEnded(false);

    competenceRequestManager.initNewRequest(competenceRequest);

    assertFalse(competenceRequestManager.checkAndCompleteFlow(competenceRequest));
  }

  @Test
  public void flowFieldsAreNotBound() throws NoSuchFieldException, NoSuchMethodException {
    for (String name : new String[] {"value", "firstApproved", "firstApprovalRequired",
        "employeeApproved", "managerApproved", "officeHeadApproved", "employeeApprovalRequired",
        "managerApprovalRequired", "officeHeadApprovalRequired", "events", "flowStarted",
        "flowEnded"}) {
      // Play legge le annotazioni di binding dal setter, quando presente.
      Field field = CompetenceRequest.class.getDeclaredField(name);
      Method setter = CompetenceRequest.class.getMethod(
          "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1), field.getType());
      assertTrue(name, setter.isAnnotationPresent(NoBinding.class));
    }
  }
}
