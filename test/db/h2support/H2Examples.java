package db.h2support;

import com.google.inject.Inject;

import db.h2support.base.H2WorkingTimeTypeSupport;
import db.h2support.base.WorkingTimeTypeDefinitions.WorkingDefinition;

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
   * Costruisce e persiste una persona.
   * @param office office
   * @param username username
   * @return persisted entity
   */
  private Person createPerson(Office office, String username) {

    User user = new User();
    user.username = username;
    user.password = "UnaPasswordQualsiasi";
    user.save();
    Person person = new Person();
    person.user = user;
    person.office = office;
    person.save();
    configurationManager.updateConfigurations(person);
    return person;
  }
  
  /**
   * Costruisce e persiste una sede.
   * @param beginDate inizio sede
   * @param name nome 
   * @param codeId codeId
   * @param code code
   * @return persisted entity
   */
  private Office buildOffice(LocalDate beginDate, String name, String codeId, String code) {

    Office office = new Office();
    office.name = name;
    office.beginDate = beginDate;
    office.codeId = codeId;
    office.code = code;
    office.save();
    configurationManager.updateConfigurations(office);
    return office;
  }

  /**
   * Istanza di un dipendente con orario Normale a tempo indeterminato.
   *
   * @param beginContract inizio contratto
   * @return mocked entity
   */
  public Person normalUndefinedEmployee(LocalDate beginContract) {

    final String name = "normalUndefinedEmployee" + beginContract;
    Office office = buildOffice(beginContract, name, name, name);
    WorkingTimeType normal = h2WorkingTimeTypeSupport.getWorkingTimeType(WorkingDefinition.Normal);
    Person person = createPerson(office, name);
    Contract contract = buildContract(person, beginContract, null, null);
    buildContractWorkingTimeType(beginContract, null, contract, normal);
    contract.refresh();
    person.refresh();
    
    return person;
  }
  
  /**
   * Istanza di un dipendente con orario PartTime 50 a tempo indeterminato.
   *
   * @param beginContract inizio contratto
   * @return mocked entity
   */
  public Person partTime50UndefinedEmployee(LocalDate beginContract) {

    final String name = "partTime50UndefinedEmployee" + beginContract;
    Office office = buildOffice(beginContract, name, name, name);
    
    WorkingTimeType normal = h2WorkingTimeTypeSupport
        .getWorkingTimeType(WorkingDefinition.PartTime50);
    Person person = createPerson(office, name);
    Contract contract = buildContract(person, beginContract, null, null);
    buildContractWorkingTimeType(beginContract, null, contract, normal);
    contract.refresh();
    person.refresh();
    
    return person;
  }

  
}
