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

package controllers;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.flows.InformationRequestManager;
import models.enumerate.InformationType;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.InformationRequestEventType;
import models.informationrequests.IllnessRequest;
import models.informationrequests.InformationRequestEvent;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
import org.joda.time.LocalDateTime;
import dao.InformationRequestDao;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle richieste di flusso informativo dei dipendenti.
 *
 * @author dario
 */
@Slf4j
@With(Resecure.class)
public class InformationRequests extends Controller {
  
  @Inject
  static InformationRequestManager informationRequestManager;
  @Inject
  static InformationRequestDao informationRequestDao;

  public static void teleworks() {
    list(InformationType.TELEWORK_INFORMATION);
  }
  
  public static void illness() {
    list(InformationType.ILLNESS_INFORMATION);
  }
  
  public static void serviceExit() {
    list(InformationType.SERVICE_INFORMATION);
  }
  
  public static void teleworksToApprove() {
    listToApprove(InformationType.TELEWORK_INFORMATION);
  }
  
  public static void illnessToApprove() {
    listToApprove(InformationType.ILLNESS_INFORMATION);
  }
  
  public static void serviceExitToApprove() {
    listToApprove(InformationType.SERVICE_INFORMATION);
  }
  
  /**
   * Ritorna la lista delle richieste del tipo passato come parametro.
   * @param type il tipo di flusso informativo da richiedere
   */
  public static void list(InformationType type) {
    Verify.verifyNotNull(type);

    val currentUser = Security.getUser().get();
    if (currentUser.person == null) {
      flash.error("L'utente corrente non ha associata una persona, non può vedere le proprie "
          + "richieste di assenza");
      Application.index();
      return;
    }
    val person = currentUser.person;
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue().minusMonths(1);
    log.debug("Prelevo le richieste di tipo {} per {} a partire da {}", type, person,
        fromDate);
    val config = informationRequestManager.getConfiguration(type, person);
    List<TeleworkRequest> teleworks = Lists.newArrayList();
    List<ServiceRequest> services = Lists.newArrayList();
    List<IllnessRequest> illness = Lists.newArrayList();
    switch (type) {
      case TELEWORK_INFORMATION:
        //teleworks = informationRequestDao
        break;
      case ILLNESS_INFORMATION:
        break;
      case SERVICE_INFORMATION:
        break;
        default:
          break;
    }
    render(teleworks, services, illness);
  }
  
  public static void listToApprove(InformationType type) {
    
  }
}
