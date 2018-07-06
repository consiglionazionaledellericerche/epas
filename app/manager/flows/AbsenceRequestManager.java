package manager.flows;

import com.google.common.base.Verify;
import javax.inject.Inject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.val;
import manager.configurations.ConfigurationManager;
import models.Person;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestType;
import org.joda.time.LocalDate;

/**
 * Operazioni sulle richiesta di assenza.
 * 
 * @author cristian
 *
 */
public class AbsenceRequestManager {


  private ConfigurationManager configurationManager;
  
  @Data
  @RequiredArgsConstructor
  public class AbsenceRequestConfiguration {
    final Person person;
    final AbsenceRequestType type;
    boolean officeHeadApprovalRequired;
    boolean managerApprovalRequired;
    boolean administrativeApprovalRequired;
    boolean allDay;
  }
  
  @Inject
  public AbsenceRequestManager(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
  }
  
  /**
   * Verifica quali sono le approvazioni richiesta per questo tipo di assenza
   * per questa persona.
   * 
   * @param requestType il tipo di richiesta di assenza
   * @param person la persona.
   * 
   * @return la configurazione con i tipi di approvazione necessari.
   */
  public AbsenceRequestConfiguration getConfiguration(
      AbsenceRequestType requestType, Person person) {
    val absenceRequestConfiguration = new AbsenceRequestConfiguration(person, requestType);
    
    if (requestType.alwaysSkipAdministrativeApproval) {
      absenceRequestConfiguration.administrativeApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.administrativeApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.administrativeApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.administrativeApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.administrativeApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.administrativeApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }
    
    if (requestType.alwaysSkipManagerApproval) {
      absenceRequestConfiguration.managerApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.managerApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.managerApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.managerApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.managerApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.managerApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }    
    if (requestType.alwaysSkipOfficeHeadApproval) {
      absenceRequestConfiguration.officeHeadApprovalRequired = false;
    } else {
      if (person.isTopQualification() 
          && requestType.officeHeadApprovalRequiredTopLevel.isPresent()) {
        absenceRequestConfiguration.officeHeadApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.officeHeadApprovalRequiredTopLevel.get(), 
                LocalDate.now());  
      }
      if (!person.isTopQualification() 
          && requestType.officeHeadApprovalRequiredTechnicianLevel.isPresent()) {
        absenceRequestConfiguration.officeHeadApprovalRequired = 
            (Boolean) configurationManager.configValue(
                person.office, requestType.officeHeadApprovalRequiredTechnicianLevel.get(), 
                LocalDate.now());  
      }
    }
    absenceRequestConfiguration.allDay = requestType.allDay;
    return absenceRequestConfiguration;
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

    val config = getConfiguration(absenceRequest.type, absenceRequest.person);

    absenceRequest.officeHeadApprovalRequired = config.officeHeadApprovalRequired;
    absenceRequest.managerApprovalRequired = config.managerApprovalRequired;
    absenceRequest.administrativeApprovalRequired = config.administrativeApprovalRequired;
  }
  
}
