package db.h2support;

import com.google.inject.Inject;

import db.h2support.base.AbsenceDefinitions.WorkingDefinition;
import db.h2support.base.H2WorkingTimeTypeSupport;

import manager.configurations.ConfigurationManager;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.User;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

/**
 * Costruzione rapida di entity per test standard.
 * 
 * @author alessandro
 *
 */
public class H2Examples {

  private static String DEFAULT_USERNAME = "DefaultUserName";
  private static String DEFAULT_USERPASS = "DefaultUserPAss";
  private static String DEFAULT_OFFICENAME = "DefaultOfficeName";
  
  private final H2WorkingTimeTypeSupport h2WorkingTimeTypeSupport;
  private final ConfigurationManager configurationManager;
  
  @Inject
  public H2Examples(H2WorkingTimeTypeSupport h2WorkingTimeTypeSupport, 
      ConfigurationManager configurationManager) {
    this.h2WorkingTimeTypeSupport = h2WorkingTimeTypeSupport;
    this.configurationManager = configurationManager;
  }

  /**
   * Costruisce e persiste il contractWorkingTimeType.
   * @param beginDate data inizio
   * @param endDate data fine
   * @param contract contratto
   * @param workingTimeType workingTimeType
   * @return persisted entity
   */
  private ContractWorkingTimeType buildContractWorkingTimeType(LocalDate beginDate,
      LocalDate endDate, Contract contract, WorkingTimeType workingTimeType) {
    
    ContractWorkingTimeType contractWorkingTimeType = new ContractWorkingTimeType();
    contractWorkingTimeType.contract = contract;
    contractWorkingTimeType.workingTimeType = workingTimeType;
    contractWorkingTimeType.beginDate = beginDate;
    contractWorkingTimeType.endDate = endDate;
    contractWorkingTimeType.save();
    return contractWorkingTimeType;
  }
  

  /**
   * Costruisce e persiste il contratto.
   * @param person person
   * @param beginDate data inizio
   * @param endDate data fine
   * @param endContract terminazione
   * @return persisted entity
   */
  private Contract buildContract(Person person, LocalDate beginDate, LocalDate endDate, 
      LocalDate endContract) {
    Contract contract = new Contract();
    contract.person = person;
    contract.beginDate = beginDate;
    contract.endDate = endDate;
    contract.endContract = endContract;
    contract.save();
    return contract;
  }

  /**
   * Costruisce e persiste la persona di default.
   * @param office office
   * @return persisted entity
   */
  private Person createDefaultPerson(Office office) {

    User user = new User();
    user.username = DEFAULT_USERNAME;
    user.password = DEFAULT_USERPASS;
    user.save();
    Person person = new Person();
    person.user = user;
    person.office = office;
    person.save();
    configurationManager.updateConfigurations(person);
    return person;
  }

  /**
   * Istanza di un dipendente con orario Normale a tempo indeterminato.
   *
   * @param beginContract inizio contratto
   * @return mocked entity
   */
  public Person normalUndefinedEmployee(LocalDate beginContract) {

    //Move to office h2 support
    Office office = new Office();
    office.name = DEFAULT_OFFICENAME;
    office.beginDate = beginContract;
    office.codeId = "0"; //const
    office.code = "0";
    office.save();
    configurationManager.updateConfigurations(office);
    
    WorkingTimeType normal = h2WorkingTimeTypeSupport.getWorkingTimeType(WorkingDefinition.Normal);
    Person person = createDefaultPerson(office);
    Contract contract = buildContract(person, beginContract, null, null);
    buildContractWorkingTimeType(beginContract, null, contract, normal);
    contract.refresh();
    person.refresh();
    
    return person;
  }

  
}
