package models.flows.enumerate;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import manager.configurations.EpasParam;

@Getter
@RequiredArgsConstructor
public enum CompetenceRequestType {

//Richiesta straordinario
  CHANGE_REPERIBILITY_REQUEST(true, false,
      Optional.of(EpasParam.CHANGE_REPERIBILITY_REQUEST_EMPLOYEE_APPROVAL_REQUIRED),
      Optional.of(EpasParam.CHANGE_REPERIBILITY_REQUEST_REPERIBILITY_MANAGER_APPROVAL_REQUIRED));  
  
  public final boolean alwaysSkipEmployeeApproval;
  public final boolean alwaysSkipReperibilityManagerApproval;
  
   
  public final Optional<EpasParam> employeeApprovalRequired;
  public final Optional<EpasParam> reperibilityManagerApprovalRequired;

  
  /**
   * TODO: Qui si potranno inserire anche le richieste di cambio turno e straordinario
   */
}
