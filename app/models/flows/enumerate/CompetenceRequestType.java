package models.flows.enumerate;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import manager.configurations.EpasParam;

@Getter
@RequiredArgsConstructor
public enum CompetenceRequestType {

//Richiesta straordinario
  OVERTIME_REQUEST(true, false, true, true,
      Optional.of(EpasParam.OVERTIME_REQUEST_EMPLOYEE_APPROVAL_REQUIRED),
      Optional.of(EpasParam.OVERTIME_REQUEST_OFFICE_HEAD_APPROVAL_REQUIRED),
      Optional.of(EpasParam.OVERTIME_REQUEST_MANAGER_APPROVAL_REQUIRED),
      Optional.absent());  
  
  public final boolean alwaysSkipEmployeeApproval;
  public final boolean alwaysSkipOfficeHeadApproval;
  public final boolean alwaysSkipManagerApproval;
  public final boolean alwaysSkipAdministrativeApproval;  
   
  public final Optional<EpasParam> employeeApprovalRequired;
  public final Optional<EpasParam> officeHeadApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> managerApprovalRequiredTechnicianLevel;
  public final Optional<EpasParam> administrativeApprovalRequiredTechnicianLevel;
  
  /**
   * TODO: Qui si potranno inserire anche le richieste di cambio turno e reperibilità
   */
}
