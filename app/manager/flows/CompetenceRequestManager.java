package manager.flows;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import models.Person;
import models.flows.CompetenceRequest;
import models.flows.enumerate.CompetenceRequestType;

public class CompetenceRequestManager {

  @Data
  @RequiredArgsConstructor
  @ToString
  public class CompetenceRequestConfiguration {
    final Person person;
    final CompetenceRequestType type;
    boolean employeeApprovalRequired;
    boolean officeHeadApprovalRequired;
    boolean managerApprovalRequired;
    boolean administrativeApprovalRequired;    
  }
  
  public void configure(CompetenceRequest competenceRequest) {
    
  }
}
