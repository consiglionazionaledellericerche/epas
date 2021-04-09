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

package models.enumerate;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import manager.configurations.EpasParam;

@Getter
@RequiredArgsConstructor
public enum InformationType {

  ILLNESS_INFORMATION(true, false,
      Optional.of(EpasParam.ILLNESS_INFORMATION_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.ILLNESS_INFORMATION_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.ILLNESS_INFORMATION_I_III_ADMINISTRATIVE_APPROVAL_REQUIRED),
      Optional.of(EpasParam.ILLNESS_INFORMATION_IV_VIII_ADMINISTRATIVE_APPROVAL_REQUIRED)),
  TELEWORK_INFORMATION(false, true,
      Optional.of(EpasParam.TELEWORK_INFORMATION_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.TELEWORK_INFORMATION_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent()),
  SERVICE_INFORMATION(false, true,
      Optional.of(EpasParam.SERVICE_INFORMATION_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.SERVICE_INFORMATION_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent());
  
  public final boolean alwaysSkipOfficeHeadApproval;
  public final boolean alwaysSkipAdministrativeApproval;
  public final Optional<EpasParam> officeHeadApprovalRequiredTopLevel;
  public final Optional<EpasParam> officeHeadApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> administrativeApprovalRequiredTopLevel;
  public final Optional<EpasParam> administrativeApprovalRequiredTechnicianLevel;

}