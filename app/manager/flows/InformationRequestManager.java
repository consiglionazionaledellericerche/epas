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

package manager.flows;

import javax.inject.Inject;
import org.joda.time.LocalDate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.flows.AbsenceRequestManager.AbsenceRequestConfiguration;
import models.Person;
import models.enumerate.InformationType;
import models.flows.enumerate.AbsenceRequestType;

@Slf4j
public class InformationRequestManager {

  private ConfigurationManager configurationManager;
  /**
   * DTO per la configurazione delle InformationRequest.
   */
  @Data
  @RequiredArgsConstructor
  @ToString
  public class InformationRequestConfiguration {
    final Person person;
    final InformationType type;
    boolean officeHeadApprovalRequired;
  }
  
  @Inject
  public InformationRequestManager(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
  }
  
  /**
   * Verifica quali sono le approvazioni richiesta per questo tipo di assenza per questa persona.
   *
   * @param requestType il tipo di richiesta di assenza
   * @param person la persona.
   *
   * @return la configurazione con i tipi di approvazione necessari.
   */
  public InformationRequestConfiguration getConfiguration(InformationType requestType,
      Person person) {
    val informationRequestConfiguration = new InformationRequestConfiguration(person, requestType);


    if (requestType.alwaysSkipOfficeHeadApproval) {
      informationRequestConfiguration.officeHeadApprovalRequired = false;
    } else {
      if (person.isTopQualification()
          && requestType.officeHeadApprovalRequiredTopLevel.isPresent()) {
        informationRequestConfiguration.officeHeadApprovalRequired =
            (Boolean) configurationManager.configValue(person.office,
                requestType.officeHeadApprovalRequiredTopLevel.get(), LocalDate.now());
      }
      if (!person.isTopQualification()
          && requestType.officeHeadApprovalRequiredTechnicianLevel.isPresent()) {
        informationRequestConfiguration.officeHeadApprovalRequired =
            (Boolean) configurationManager.configValue(person.office,
                requestType.officeHeadApprovalRequiredTechnicianLevel.get(), LocalDate.now());
      }

    }
    
    return informationRequestConfiguration;
  }
}
