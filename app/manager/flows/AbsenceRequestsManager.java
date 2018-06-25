package manager.flows;

import com.google.common.base.Verify;
import javax.inject.Inject;
import lombok.val;
import manager.configurations.ConfigurationManager;
import models.flows.AbsenceRequest;
import org.joda.time.LocalDate;

/**
 * Operazioni sulle richiesta di assenza.
 * 
 * @author cristian
 *
 */
public class AbsenceRequestsManager {


  private ConfigurationManager configurationManager;
  
  @Inject
  public AbsenceRequestsManager(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
  }
  
  /**
   * Imposta nella richiesta di assenza i tipi di approvazione necessari
   * in funzione del tipo di assenza e della configurazione specifica della
   * sede del dipendente.
   * 
   * @param absenceRequest la richiesta di assenza.
   */
  public void configure(AbsenceRequest absenceRequest) {
    Verify.verifyNotNull(absenceRequest.type);
    Verify.verifyNotNull(absenceRequest.person);
    
    val requestType = absenceRequest.type;
    val person = absenceRequest.person;
    
    if (requestType.alwaysSkipAdministrativeApproval) {
      absenceRequest.administrativeApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.administrativeApprovalRequiredTopLevel.isPresent()) {
        absenceRequest.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.administrativeApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.administrativeApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequest.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.administrativeApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }
    
    if (requestType.alwaysSkipManagerApproval) {
      absenceRequest.managerApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.managerApprovalRequiredTopLevel.isPresent()) {
        absenceRequest.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.managerApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.managerApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequest.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.managerApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }    
    if (requestType.alwaysSkipOfficeHeadApproval) {
      absenceRequest.officeHeadApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.officeHeadApprovalRequiredTopLevel.isPresent()) {
        absenceRequest.officeHeadApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.officeHeadApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.officeHeadApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequest.officeHeadApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.officeHeadApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }
  }
}
