package manager.flows;

import com.google.common.base.Verify;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import java.util.List;
import javax.inject.Inject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import manager.configurations.ConfigurationManager;
import models.Person;
import models.Role;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestType;
import org.apache.commons.compress.utils.Lists;
import org.joda.time.LocalDate;

/**
 * Operazioni sulle richiesta di assenza.
 * 
 * @author cristian
 *
 */
public class AbsenceRequestManager {


  private ConfigurationManager configurationManager;
  private UsersRolesOfficesDao uroDao;
  private RoleDao roleDao;

  @Data
  @RequiredArgsConstructor
  @ToString
  public class AbsenceRequestConfiguration {
    final Person person;
    final AbsenceRequestType type;
    boolean officeHeadApprovalRequired;
    boolean managerApprovalRequired;
    boolean administrativeApprovalRequired;
    boolean allDay;
  }

  /**
   * Inizializzazione con injection dei vari componenti necessari.
   * 
   * @param configurationManager Manager delle configurazioni
   * @param uroDao UsersRolesOfficesDao
   * @param roleDao RoleDao
   */
  @Inject
  public AbsenceRequestManager(ConfigurationManager configurationManager,
      UsersRolesOfficesDao uroDao, RoleDao roleDao) {
    this.configurationManager = configurationManager;
    this.uroDao = uroDao;
    this.roleDao = roleDao;
  }

  /**
   * Verifica che gruppi ed eventuali responsabile di sede siano presenti per
   * poter richiedere il tipo di assenza. 
   * 
   * @param requestType il tipo di assenza da controllare
   * @param person la persona per cui controllare il tipo di assenza
   * @return la lista degli eventuali problemi riscontrati.
   */
  public List<String> checkconfiguration(AbsenceRequestType requestType, Person person) {
    Verify.verifyNotNull(requestType);
    Verify.verifyNotNull(person);
    
    val problems = Lists.<String>newArrayList();
    val config = getConfiguration(requestType, person);
       
    if (config.isManagerApprovalRequired() && person.personInCharge == null) {
      problems.add(
          String.format("Approvazione del responsabile richiesta. "
              + "Il dipendente %s non ha impostato nessun responsabile.", person.getFullname())); 
    }
    
    if (config.isAdministrativeApprovalRequired() 
        && uroDao.getUsersWithRoleOnOffice(
            roleDao.getRoleByName(Role.PERSONNEL_ADMIN), person.office).isEmpty()) {
      problems.add(
          String.format("Approvazione dell'amministratore del personale richiesta. "
              + "L'ufficio %s non ha impostato nessun amministratore del personale.",
              person.office.getName())); 
    }
    
    if (config.isOfficeHeadApprovalRequired() 
        && uroDao.getUsersWithRoleOnOffice(
            roleDao.getRoleByName(Role.SEAT_SUPERVISOR), person.office).isEmpty()) {
      problems.add(
          String.format("Approvazione del responsabile di sede richiesta. "
              + "L'ufficio %s non ha impostato nessun responsabile di sede.",
              person.office.getName())); 
    }
    return problems;
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

  /**
   * Approvazione di una richista di assenza.
   * @param absenceRequest la richiesta di assenza. 
   * @param approver la persona che effettua l'approvazione.
   * @return la lista di eventuali problemi riscontrati durante l'approvazione.
   */
  public List<String> approval(AbsenceRequest absenceRequest, Person approver) {
    List<String> problems = Lists.newArrayList();
    if (absenceRequest.managerApprovalRequired && absenceRequest.managerApproved == null) {
      if (!absenceRequest.person.personInCharge.equals(approver)) {
        problems.add(
            String.format("Questa richiesta non può essere approvata dal responsabile"
                + " di gruppo {}", approver.getFullname()));
      }
    }
    return problems;
  }
}
