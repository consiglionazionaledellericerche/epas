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
import java.time.LocalDateTime;
import models.base.InformationRequest;
import models.informationrequests.IllnessRequest;
import models.informationrequests.InformationRequestEvent;
import org.junit.Before;
import org.junit.Test;
import play.data.binding.NoBinding;
import play.test.UnitTest;

/**
 * Verifica che lo stato del flusso di una nuova richiesta di flusso informativo sia gestito solo
 * dal sistema.
 */
public class InformationRequestManagerTest extends UnitTest {

  private InformationRequestManager informationRequestManager;

  @Before
  public void createManager() {
    informationRequestManager = new InformationRequestManager(null, null, null, null, null);
  }

  private IllnessRequest requestWithFlowState() {
    IllnessRequest request = new IllnessRequest();
    request.setStartAt(LocalDateTime.now().minusYears(1));
    request.setEndTo(LocalDateTime.now());
    request.setOfficeHeadApproved(LocalDateTime.now());
    request.setAdministrativeApproved(LocalDateTime.now());
    request.setManagerApproved(LocalDateTime.now());
    request.setAdministrativeApprovalRequired(true);
    request.setManagerApprovalRequired(true);
    request.setFlowStarted(true);
    request.setFlowEnded(true);
    request.getEvents().add(InformationRequestEvent.builder().build());
    return request;
  }

  @Test
  public void newRequestIsNotApproved() {
    IllnessRequest request = requestWithFlowState();
    assertTrue(request.isFullyApproved());

    informationRequestManager.initNewRequest(request);

    assertNull(request.getEndTo());
    assertNull(request.getOfficeHeadApproved());
    assertNull(request.getAdministrativeApproved());
    assertNull(request.getManagerApproved());
    assertFalse(request.isFlowStarted());
    assertFalse(request.isFlowEnded());
    assertTrue(request.getEvents().isEmpty());
    assertFalse(request.isFullyApproved());
    assertTrue(request.getStartAt().isAfter(LocalDateTime.now().minusMinutes(1)));
  }

  @Test
  public void flowFieldsAreNotBound() throws NoSuchFieldException, NoSuchMethodException {
    for (String name : new String[] {"startAt", "endTo", "officeHeadApproved",
        "administrativeApproved", "managerApproved", "officeHeadApprovalRequired",
        "administrativeApprovalRequired", "managerApprovalRequired", "flowStarted", "flowEnded",
        "events"}) {
      // Play legge le annotazioni di binding dal setter, quando presente.
      Field field = InformationRequest.class.getDeclaredField(name);
      Method setter = InformationRequest.class.getMethod(
          "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1), field.getType());
      assertTrue(name, setter.isAnnotationPresent(NoBinding.class));
    }
  }
}
