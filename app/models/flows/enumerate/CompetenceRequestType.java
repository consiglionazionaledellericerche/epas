package models.flows.enumerate;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import manager.configurations.EpasParam;

@Getter
@RequiredArgsConstructor
public enum CompetenceRequestType {

//Richiesta Ferie
  OVERTIME_REQUEST(false, true, true,
      Optional.of(EpasParam.OVERTIME_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.OVERTIME_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED),
      Optional.absent());  
  
  public final boolean alwaysSkipOfficeHeadApproval;
  public final boolean alwaysSkipManagerApproval;
  public final boolean alwaysSkipAdministrativeApproval;  
   
  public final Optional<EpasParam> officeHeadApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> managerApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> administrativeApprovalRequiredTechnicianLevel;
  
}
