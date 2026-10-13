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

package models.informationrequests;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import models.base.InformationRequest;
import models.enumerate.InformationType;
import org.junit.Test;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.test.UnitTest;

/**
 * Verifica che lo stato del flusso delle richieste di flusso informativo non venga mai legato dai
 * parametri della richiesta HTTP.
 */
public class InformationRequestBindingTest extends UnitTest {

  private static Object bind(String name, Class<?> clazz, Map<String, String[]> params) {
    return Binder.bind(ParamNode.convert(params), name, clazz, clazz, null);
  }

  private static Map<String, String[]> flowParams(String name, String... others) {
    ImmutableMap.Builder<String, String[]> params = ImmutableMap.<String, String[]>builder()
        .put(name + ".officeHeadApprovalRequired", new String[] {"false"})
        .put(name + ".administrativeApprovalRequired", new String[] {"true"})
        .put(name + ".managerApprovalRequired", new String[] {"true"})
        .put(name + ".flowStarted", new String[] {"true"})
        .put(name + ".flowEnded", new String[] {"true"});
    for (int i = 0; i < others.length; i += 2) {
      params.put(name + "." + others[i], new String[] {others[i + 1]});
    }
    return params.build();
  }

  private static void assertFlowNotBound(InformationRequest request) {
    assertTrue(request.isOfficeHeadApprovalRequired());
    assertFalse(request.isAdministrativeApprovalRequired());
    assertFalse(request.isManagerApprovalRequired());
    assertFalse(request.isFlowStarted());
    assertFalse(request.isFlowEnded());
    assertNull(request.getOfficeHeadApproved());
    assertFalse(request.isFullyApproved());
  }

  @Test
  public void serviceRequestFlowStateIsNotBound() {
    ServiceRequest request = (ServiceRequest) bind("serviceRequest", ServiceRequest.class,
        flowParams("serviceRequest", "reason", "motivo",
            "informationType", InformationType.SERVICE_INFORMATION.name()));

    assertEquals("motivo", request.getReason());
    assertEquals(InformationType.SERVICE_INFORMATION, request.getInformationType());
    assertFlowNotBound(request);
  }

  @Test
  public void illnessRequestFlowStateIsNotBound() {
    IllnessRequest request = (IllnessRequest) bind("illnessRequest", IllnessRequest.class,
        flowParams("illnessRequest", "name", "nome"));

    assertEquals("nome", request.getName());
    assertFlowNotBound(request);
  }

  @Test
  public void parentalLeaveRequestFlowStateIsNotBound() {
    ParentalLeaveRequest request = (ParentalLeaveRequest) bind("parentalLeaveRequest",
        ParentalLeaveRequest.class, flowParams("parentalLeaveRequest",
            "informationType", InformationType.PARENTAL_LEAVE_INFORMATION.name()));

    assertEquals(InformationType.PARENTAL_LEAVE_INFORMATION, request.getInformationType());
    assertFlowNotBound(request);
  }
}
