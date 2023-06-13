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


/**
 * Attributi relative ad una tipologia di richiesta di mensile di competenze
 * da attribuire ad un dipendente.
 *
 * @author Dario Tagliaferri
 */

package models.flows.enumerate;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import manager.configurations.EpasParam;

@Getter
@RequiredArgsConstructor
public enum CompetenceRequestType {

//Richiesta straordinario
  OVERTIME_REQUEST(true, false, false, true,
      Optional.absent(),
      Optional.of(EpasParam.OVERTIME_REQUEST_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.OVERTIME_REQUEST_MANAGER_APPROVAL_REQUIRED),
      Optional.absent()),
//Richiesta straordinario
  CHANGE_REPERIBILITY_REQUEST(false, true, false, true,
      Optional.of(EpasParam.CHANGE_REPERIBILITY_REQUEST_EMPLOYEE_APPROVAL_REQUIRED),
      Optional.of(EpasParam.CHANGE_REPERIBILITY_REQUEST_REPERIBILITY_MANAGER_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent());  
  
  
  public final boolean alwaysSkipEmployeeApproval;
  public final boolean alwaysSkipOfficeHeadApproval;
  public final boolean alwaysSkipManagerApproval;
  public final boolean alwaysSkipAdministrativeApproval;  
     
  public final Optional<EpasParam> employeeApprovalRequired;
  public final Optional<EpasParam> officeHeadApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> managerApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> administrativeApprovalRequiredTechnicianLevel;
    
 
}
