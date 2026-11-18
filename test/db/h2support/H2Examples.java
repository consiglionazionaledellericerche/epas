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
import models.Institute;
import models.Office;
import models.Person;
import models.PersonConfiguration;
import models.Qualification;
import models.Role;
import models.User;
import models.UsersRolesOffices;
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
  public static final String DEFAULT_PASSWORD = "UnaPasswordQualsiasi";
  
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
    user.updatePassword(DEFAULT_PASSWORD);
    user.save();
    Person person = new Person();
    person.setName("Name " + username);
    person.setSurname("Surname " + username);
    person.setUser(user);
    person.setQualification(Qualification.findById(DEFAULT_PERSON_QUALIFICATION));
    person.save();
    person.changeOffice(office, new LocalDate(2000, 1, 1)).save();
    configurationManager.updateConfigurations(person);
    return person;
  }
  
  /**
   * Costruisce e persiste una sede, con il proprio istituto (richiesto dalle regole dei
   * permessi sulla sede).
   *
   * @param beginDate inizio sede
   * @param name nome 
   * @param codeId codeId
   * @param code code
   * @return persisted entity
   */
  private Office buildOffice(LocalDate beginDate, String name, String codeId, String code) {

    Institute institute = new Institute();
    institute.setName(name);
    institute.setCds(code);
    institute.save();

    Office office = new Office();
    office.setInstitute(institute);
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
   * Istanza di un dipendente con orario Normale e data di attivazione pari all'inizio del
   * contratto, da cui decorrono anche i suoi parametri di configurazione.
   *
   * @param beginContract inizio contratto e data di attivazione
   * @return persisted entity
   */
  public Person activeNormalEmployee(LocalDate beginContract) {
    Person person = normalEmployee(beginContract, Optional.absent());
    person.setBeginDate(beginContract);
    person.save();
    for (PersonConfiguration configuration : person.getPersonConfigurations()) {
      configuration.setBeginDate(beginContract);
      configuration.save();
    }
    return person;
  }

  /**
   * Utente, associato ad una persona della sede, con il ruolo indicato sulla sede.
   * La password è {@link #DEFAULT_PASSWORD}.
   *
   * @param office sede
   * @param roleName nome del ruolo (es. {@link Role#REGISTRY_MANAGER}), creato se assente
   * @return persisted entity
   */
  public User userWithOfficeRole(Office office, String roleName) {
    Role role = Role.find("byName", roleName).first();
    if (role == null) {
      role = new Role();
      role.setName(roleName);
      role.save();
    }
    Person person = createPerson(office, roleName + UUID.randomUUID());
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(person.getUser());
    uro.setOffice(office);
    uro.setRole(role);
    uro.save();
    return person.getUser();
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

  /**
   * Istanza di un dipendente con orario part time verticale (lavorativi lunedì, martedì e
   * mercoledì) e data di attivazione pari all'inizio del contratto, da cui decorrono anche i
   * suoi parametri di configurazione.
   *
   * @param beginContract inizio contratto e data di attivazione
   * @return persisted entity
   */
  public Person activeVerticalPartTimeEmployee(LocalDate beginContract) {
    final String name = "verticalPartTimeEmployee" + beginContract + UUID.randomUUID();
    Office office = buildOffice(beginContract, name, name, name);
    WorkingTimeType verticalPartTime = h2WorkingTimeTypeSupport
        .getWorkingTimeType(WorkingDefinition.VerticalPartTime60);
    Person person = createPerson(office, name);
    Contract contract =
        buildContract(person, beginContract, Optional.absent(), Optional.absent(),
            verticalPartTime);
    contract.refresh();
    person.refresh();
    person.setBeginDate(beginContract);
    person.save();
    for (PersonConfiguration configuration : person.getPersonConfigurations()) {
      configuration.setBeginDate(beginContract);
      configuration.save();
    }
    return person;
  }

}
