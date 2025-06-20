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

/**
 * Tipologie implementate di richiesta di assenza.
 *
 * @author Cristian Lucchesi
 *
 */
@Getter
@RequiredArgsConstructor
public enum AbsenceRequestType {
  
  //Richiesta Ferie
  VACATION_REQUEST(true, false, false, true, false, false,
      Optional.of(EpasParam.VACATION_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.VACATION_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.VACATION_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED),
      Optional.of(EpasParam.VACATION_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent(),
      Optional.of(EpasParam.VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED),
      true),
  
  //Riposo compensatvio
  COMPENSATORY_REST(true, false, false, true, false, false,
      Optional.of(EpasParam.COMPENSATORY_REST_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.COMPENSATORY_REST_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.COMPENSATORY_REST_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED),
      Optional.of(EpasParam.COMPENSATORY_REST_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent(),
      Optional.of(EpasParam.VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED),
      true),
  
  //Riposo compensatvio
  PERSONAL_PERMISSION(true, false, false, true, false, false,
      Optional.of(EpasParam.PERSONAL_PERMISSION_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.PERSONAL_PERMISSION_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.PERSONAL_PERMISSION_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED),
      Optional.of(EpasParam.PERSONAL_PERMISSION_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent(),
      Optional.of(EpasParam.VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED),
      false),
  
  //Richiesta ferie anno passato dopo scadenza
//  VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST(true, false, false, true, false, false,
//      Optional.of(EpasParam.VACATION_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
//      Optional.of(EpasParam.VACATION_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
//      Optional.of(EpasParam.VACATION_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED),
//      Optional.of(EpasParam.VACATION_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED),
//      Optional.absent(),
//      Optional.absent(),
//      Optional.of(EpasParam.VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED),
//      true),
  
  //Permessso breve
  SHORT_TERM_PERMIT(false, false, false, false, false, false,
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      false);
  
  public final boolean allDay;
  public final boolean alwaysSkipOfficeHeadApproval;
  public final boolean alwaysSkipManagerApproval;
  public final boolean alwaysSkipAdministrativeApproval;
  public final boolean alwaysSkipOfficeHeadApprovalForManager;
  public final boolean attachmentRequired;
  
  public final Optional<EpasParam> officeHeadApprovalRequiredTopLevel; 
  public final Optional<EpasParam> officeHeadApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> managerApprovalRequiredTopLevel; 
  public final Optional<EpasParam> managerApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> administrativeApprovalRequiredTopLevel;
  public final Optional<EpasParam> administrativeApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> officeHeadApprovalRequiredForManager;
  
  public final boolean canBeInsertedByTopLevelWithoutApproval;
  
}
