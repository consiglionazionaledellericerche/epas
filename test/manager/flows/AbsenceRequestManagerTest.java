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
import models.flows.AbsenceRequest;
import models.flows.AbsenceRequestEvent;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import play.data.binding.NoBinding;
import play.test.UnitTest;

/**
 * Verifica che lo stato del flusso di una nuova richiesta di assenza sia gestito solo dal
 * sistema.
 */
public class AbsenceRequestManagerTest extends UnitTest {

  private AbsenceRequestManager absenceRequestManager;

  @Before
  public void createManager() {
    absenceRequestManager = new AbsenceRequestManager(null, null, null, null, null, null, null,
        null, null, null, null, null, null, null);
  }

  private AbsenceRequest requestWithFlowState() {
    AbsenceRequest absenceRequest = new AbsenceRequest();
    absenceRequest.setManagerApproved(LocalDateTime.now());
    absenceRequest.setAdministrativeApproved(LocalDateTime.now());
    absenceRequest.setOfficeHeadApproved(LocalDateTime.now());
    absenceRequest.setFlowStarted(true);
    absenceRequest.setFlowEnded(true);
    absenceRequest.getEvents().add(AbsenceRequestEvent.builder().build());
    return absenceRequest;
  }

  @Test
  public void newRequestIsNotApproved() {
    AbsenceRequest absenceRequest = requestWithFlowState();
    assertTrue(absenceRequest.isFullyApproved());

    absenceRequestManager.initNewRequest(absenceRequest);

    assertNull(absenceRequest.getManagerApproved());
    assertNull(absenceRequest.getAdministrativeApproved());
    assertNull(absenceRequest.getOfficeHeadApproved());
    assertFalse(absenceRequest.isFlowStarted());
    assertFalse(absenceRequest.isFlowEnded());
    assertTrue(absenceRequest.getEvents().isEmpty());
    assertNull(absenceRequest.getAttachment());
    assertFalse(absenceRequest.isFullyApproved());
  }

  @Test
  public void newRequestDoesNotCompleteFlow() {
    AbsenceRequest absenceRequest = requestWithFlowState();
    absenceRequest.setFlowEnded(false);

    absenceRequestManager.initNewRequest(absenceRequest);

    assertFalse(absenceRequestManager.checkAndCompleteFlow(absenceRequest).isPresent());
  }

  @Test
  public void flowFieldsAreNotBound() throws NoSuchFieldException, NoSuchMethodException {
    for (String name : new String[] {"managerApproved", "administrativeApproved",
        "officeHeadApproved", "managerApprovalRequired", "administrativeApprovalRequired",
        "officeHeadApprovalRequired", "officeHeadApprovalForManagerRequired", "events",
        "flowStarted", "flowEnded", "attachment"}) {
      // Play legge le annotazioni di binding dal setter, quando presente.
      Field field = AbsenceRequest.class.getDeclaredField(name);
      Method setter = AbsenceRequest.class.getMethod(
          "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1), field.getType());
      assertTrue(name, setter.isAnnotationPresent(NoBinding.class));
    }
  }
}
