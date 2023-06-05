/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package db.h2support;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import db.h2support.base.H2WorkingTimeTypeSupport;
import db.h2support.base.WorkingTimeTypeDefinitions.WorkingDefinition;
import java.util.UUID;
import manager.ContractManager;
import manager.configurations.ConfigurationManager;
import models.Contract;
import models.Office;
import models.Person;
import models.Qualification;
import models.User;
import models.WorkingTimeType;
import org.joda.time.LocalDate;

/**
 * Costruzione rapida di entity per test standard.
 *
 * @author Alessandro Martelli
 *
 */
public class H2Examples {

  public static final long DEFAULT_PERSON_QUALIFICATION = 4L;
  
  private final H2WorkingTimeTypeSupport h2WorkingTimeTypeSupport;
  private final ConfigurationManager configurationManager;
  private final ContractManager contractManager;
  
  /**
   * Injection. 
   */
  @Inject
  public H2Examples(H2WorkingTimeTypeSupport h2WorkingTimeTypeSupport, 
      ConfigurationManager configurationManager, ContractManager contractManager) {
    this.h2WorkingTimeTypeSupport = h2WorkingTimeTypeSupport;
    this.configurationManager = configurationManager;
    this.contractManager = contractManager;
  }

  /**
   * Costruisce e persiste il contratto.
   *
   * @param person person
   * @param beginDate data inizio
   * @param endDate data fine
   * @param endContract terminazione
   * @return persisted entity
   */
  private Contract buildContract(Person person, LocalDate beginDate, Optional<LocalDate> endDate, 
      Optional<LocalDate> endContract, WorkingTimeType workingTimeType) {
    Contract contract = new Contract();
    contract.setPerson(person);
    contract.setBeginDate(beginDate);
    if (endDate.isPresent()) {
      contract.setEndDate(endDate.get());
    }
    if (endContract.isPresent()) {
      contract.setEndContract(endContract.get());
    }
    contractManager.properContractCreate(contract, Optional.of(workingTimeType), false);
    return contract;
  }

  /**
   * Costruisce e persiste una persona.
   *
   * @param office office
   * @param username username
   * @return persisted entity
   */
  private Person createPerson(Office office, String username) {

    User user = new User();
    user.setUsername(username);
    user.updatePassword("UnaPasswordQualsiasi");
    user.save();
    Person person = new Person();
    person.setName("Name " + username);
    person.setSurname("Surname " + username);
    person.setUser(user);
    person.setOffice(office);
    person.setQualification(Qualification.findById(DEFAULT_PERSON_QUALIFICATION));
    person.save();
    configurationManager.updateConfigurations(person);
    return person;
  }
  
  /**
   * Costruisce e persiste una sede.
   *
   * @param beginDate inizio sede
   * @param name nome 
   * @param codeId codeId
   * @param code code
   * @return persisted entity
   */
  private Office buildOffice(LocalDate beginDate, String name, String codeId, String code) {

    Office office = new Office();
    office.setName(name);
    office.setBeginDate(beginDate);
    office.setCodeId(codeId);
    office.setCode(code);
    office.save();
    configurationManager.updateConfigurations(office);
    return office;
  }

  /**
   * Istanza di un dipendente con orario Normale.
   *
   * @param beginContract inizio contratto
   * @return mocked entity
   */
  public Person normalEmployee(LocalDate beginContract, 
      Optional<LocalDate> expireContract) {

    final String name = "normalUndefinedEmployee" + beginContract + UUID.randomUUID();
    Office office = buildOffice(beginContract, name, name, name);
    WorkingTimeType normal = h2WorkingTimeTypeSupport.getWorkingTimeType(WorkingDefinition.Normal);
    Person person = createPerson(office, name);
    Contract contract = 
        buildContract(person, beginContract, expireContract, Optional.absent(), normal);
    contract.refresh();
    person.refresh();
    
    return person;
  }
  
  /**
   * Istanza di un dipendente con orario PartTime 50.
   *
   * @param beginContract inizio contratto
   * @return mocked entity
   */
  public Person partTime50Employee(LocalDate beginContract) {

    final String name = "partTime50UndefinedEmployee" + beginContract + UUID.randomUUID();
    Office office = buildOffice(beginContract, name, name, name);
    
    WorkingTimeType normal = h2WorkingTimeTypeSupport
        .getWorkingTimeType(WorkingDefinition.PartTime50);
    Person person = createPerson(office, name);
    Contract contract = 
        buildContract(person, beginContract, Optional.absent(), Optional.absent(), normal);
    contract.refresh();
    person.refresh();
    
    return person;
  }

  
}
