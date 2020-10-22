package models.flows.enumerate;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import manager.configurations.EpasParam;

/**
 * Tipologie implementate di richiesta di assenza.
 * 
 * @author cristian
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
      Optional.of(EpasParam.VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED)),
  
  //Riposo compensatvio
  COMPENSATORY_REST(true, false, false, true, false, false,
      Optional.of(EpasParam.COMPENSATORY_REST_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.COMPENSATORY_REST_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.COMPENSATORY_REST_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED),
      Optional.of(EpasParam.COMPENSATORY_REST_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent(),
      Optional.of(EpasParam.VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED)),
  
  //Riposo compensatvio
  PERSONAL_PERMISSION(true, false, false, true, false, false,
      Optional.of(EpasParam.PERSONAL_PERMISSION_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.PERSONAL_PERMISSION_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.PERSONAL_PERMISSION_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED),
      Optional.of(EpasParam.PERSONAL_PERMISSION_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent(),
      Optional.of(EpasParam.VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED)),
  
  //Richiesta ferie anno passato dopo scadenza
  VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST(true, false, false, true, false, false,
      Optional.of(EpasParam.VACATION_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.VACATION_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.VACATION_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED),
      Optional.of(EpasParam.VACATION_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED),
      Optional.absent(),
      Optional.absent(),
      Optional.of(EpasParam.VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED)),
  
  //Permessso breve
  SHORT_TERM_PERMIT(false, false, false, false, false, false,
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      Optional.absent(),
      Optional.absent());
  
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
  
}
