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

package models.flows.enumerate;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import manager.configurations.EpasParam;

@Getter
@RequiredArgsConstructor
public enum CompetenceRequestType {

  //Richiesta straordinario
  CHANGE_REPERIBILITY_REQUEST(false, false,
      Optional.of(EpasParam.CHANGE_REPERIBILITY_REQUEST_EMPLOYEE_APPROVAL_REQUIRED),
      Optional.of(EpasParam.CHANGE_REPERIBILITY_REQUEST_REPERIBILITY_MANAGER_APPROVAL_REQUIRED));  
  
  public final boolean alwaysSkipEmployeeApproval;
  public final boolean alwaysSkipReperibilityManagerApproval;
  
   
  public final Optional<EpasParam> employeeApprovalRequired;
  public final Optional<EpasParam> reperibilityManagerApprovalRequired;

  
  /*
   * TODO: Qui si potranno inserire anche le richieste di cambio turno e straordinario
   */
}
