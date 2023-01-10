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

package manager.sync;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dao.PersonDao;
import dao.RoleDao;
import helpers.rest.ApiRequestException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.ContractManager;
import manager.OfficeManager;
import manager.RegistryNotificationManager;
import manager.UserManager;
import manager.configurations.ConfigurationManager;
import models.Contract;
import models.Office;
import models.Person;
import models.Role;
import org.joda.time.LocalDate;
import play.data.validation.Validation;
import synch.perseoconsumers.contracts.ContractPerseoConsumer;
import synch.perseoconsumers.people.PeoplePerseoConsumer;

/**
 * Manager per la sincronizzazione delle informazioni tra ePAS e Perseo.
 *
 * @author Marco Andreini
 */
@Slf4j
public class SynchronizationManager {

  private PeoplePerseoConsumer peoplePerseoConsumer;
  private ContractPerseoConsumer contractPerseoConsumer;
  private PersonDao personDao;
  private UserManager userManager;
  private ContractManager contractManager;
  private RoleDao roleDao;
  private OfficeManager officeManager;
  private RegistryNotificationManager registryNotificationManager;
  private ConfigurationManager configurationManager;

  /**
   * Default constructor, useful for injection.  
   */
  @Inject
  public SynchronizationManager(PeoplePerseoConsumer peoplePerseoConsumer,
      ContractPerseoConsumer contractPerseoConsumer,
      PersonDao personDao, UserManager userManager, 
      ContractManager contractManager, RoleDao roleDao,
      OfficeManager officeManager, RegistryNotificationManager registryNotificationManager) {
    this.peoplePerseoConsumer = peoplePerseoConsumer;
    this.contractPerseoConsumer = contractPerseoConsumer;
    this.personDao = personDao;
    this.userManager = userManager;
    this.contractManager = contractManager;
    this.roleDao = roleDao;
    this.officeManager = officeManager;
    this.registryNotificationManager = registryNotificationManager;
  }

  /**
   * Sincronizza le persone di un ufficio presenti nell'anagrafica principale con 
   * quelle di ePAS.
   *
   * @param office l'ufficio di cui sincronizzare le persone.
   */
  public SyncResult syncPeopleInOffice(Office office, boolean alsoContracts) {
    log.debug("Sincronizzazione delle persone presenti nell'ufficio {}.",
        office.getName());
    val result = new SyncResult();
    if (office.getPerseoId() == null) {
      result.setFailed();
      result.add(
          String.format("Impossibile sincronizzare l'ufficio {} perché non ha un id anagrafica"
          + "esterno impostato", office.getLabel()));
      return result;
    }
    val perseoPeople = peoplePerseoConsumer
        .perseoPeopleByPerseoId(Optional.fromNullable(office.getPerseoId()));
    log.debug("Trovate {} persone in anagrafica associate all'ufficio {}.",
        perseoPeople.size(), office.getName());

    List<Person> epasPeople = personDao.listFetched(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();

    log.debug("Trovate {} persone in ePAS associate all'ufficio {}.",
        epasPeople.size(), office.getName());

    val epasPeopleByPerseoId = epasPeople.stream().filter(p -> p.getPerseoId() != null)
        .collect(Collectors.toMap(p -> p.getPerseoId(), p -> p));

    val epasPeopleByNumber = epasPeople.stream().filter(p -> p.getNumber() != null)
        .collect(Collectors.toMap(p -> p.getNumber(), p -> p));
    
    for (Person perseoPerson : perseoPeople.values()) {
      //Se il perseoId e la matricola non sono presenti per l'ufficio corrente 
      //allora viene creata o trasferita la persona
      if (!epasPeopleByPerseoId.containsKey(perseoPerson.getPerseoId()) 
          && !epasPeopleByNumber.containsKey(perseoPerson.getNumber())) {
        result.add(createOrTransferPerson(perseoPerson, office));
      } else {
        Person personToSync = null;
        if (epasPeopleByPerseoId.containsKey(perseoPerson.getPerseoId())) {
          personToSync = epasPeopleByPerseoId.get(perseoPerson.getPerseoId());
        } else {
          personToSync = epasPeopleByNumber.get(perseoPerson.getNumber());
        }
        result.add(
            syncPersonWithPersonRegistry(personToSync, perseoPerson));
      }
    }

    log.debug("Terminata la sincronizzazione delle persone presenti nell'ufficio {}.",
        office.getName());
    return result;
  }

  /**
   * Crea una persona in ePAS associandola alla sede se non è presente in ePAS.
   * Se la persona è presente in ePAS gli cambia l'assegnazione della sede.
   */
  public SyncResult createOrTransferPerson(Person perseoPerson, Office office) {
    log.debug("Persona {} (matricola:{} perseoId:{}) non associata in ePAS all'ufficio {}.",
        perseoPerson.fullName(), perseoPerson.getNumber(),
        perseoPerson.getPerseoId(), office.getName());

    val syncResult = new SyncResult();

    Person epasPerson = personDao.getPersonByPerseoId(perseoPerson.getPerseoId());
    if (epasPerson == null) {
      epasPerson = personDao.getPersonByNumber(perseoPerson.getNumber());
    }

    if (epasPerson != null) {
      log.info("La persona {} (matricola = {}) è presente in ePAS ed associata alla sede {}."
          + "Effettuo il cambio di sede verso {}.",
          epasPerson.getFullname(), epasPerson.getNumber(), 
          epasPerson.getOffice().getName(), office);

      Office oldOffice = epasPerson.getOffice(); 
      epasPerson.setOffice(office);
      epasPerson.save();

      registryNotificationManager.notifyPersonHasChangedOffice(epasPerson, oldOffice);
      return syncResult.add(
          String.format(
              "Effettuato il campo di sede per %s. Vecchia sede %s, nuova sede %s.",
              epasPerson.fullName(), oldOffice.getName(), epasPerson.getOffice().getName()));
    }

    // join dell'office (in automatico ancora non c'è...)
    perseoPerson.setOffice(office);
    perseoPerson.setBeginDate(LocalDate.now().withDayOfMonth(1).withMonthOfYear(1).minusDays(1));
    val validation = Validation.current.get(); 
    validation.valid(perseoPerson);
    if (Validation.hasErrors()) {
      // notifica che perseo ci ha mandato un oggetto che in epas non può essere accettato!
      log.info("L'importazione della persona con perseoId={} ha comportato errori di "
          + "validazione nella persona. errors={}.",
          perseoPerson.getPerseoId(), validation.errorsMap());
      Validation.clear();
      syncResult.setSuccess(false);
      return syncResult.add(
          String.format("Tentativo di importazione della persona con perseoId = %s fallito per "
              + "errori di validazione dei suoi dati.", 
              perseoPerson.getPerseoId()));
    }

    // Creazione!
    val newPerson = personCreator(perseoPerson);
    if (!newPerson.isPresent()) {
      // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
      log.info("L'importazione della persone con perseoId={} ha comportato errori di "
          + "validazione nella persona. errors={}.",
          perseoPerson.getPerseoId(), validation.errorsMap());
      Validation.clear();

      return syncResult.add(
          String.format("Tentativo di importazione della persona con perseoId = %s fallito per "
              + "errori di validazione dei suoi dati.", 
              perseoPerson.getPerseoId()));
    } else {

      log.info("Creata la nuova persona {}, matricola = {}, ufficio = {}",
          newPerson.get().getFullname(), newPerson.get().getNumber(), 
          newPerson.get().getOffice().getName());

      syncResult.add(importContracts(newPerson.get()));

      registryNotificationManager.notifyNewPerson(newPerson.get());
      return syncResult.add(
          String.format("Creata la nuova persona %s, matricola = %s, ufficio = %s",
              newPerson.get().getFullname(), newPerson.get().getNumber(), 
              newPerson.get().getOffice().getName()));
    }

  }

  /**
   * Crea utente, persona e ruoli necessari.
   */
  public Optional<Person> personCreator(Person person) {

    try {
      person.setUser(userManager.createUser(person));
      person.save();

      Role employee = roleDao.getRoleByName(Role.EMPLOYEE);
      officeManager.setUro(person.getUser(), person.getOffice(), employee);
      person.save();
      configurationManager.updateConfigurations(person);
    } catch (Exception ex) {
      return Optional.absent();
    }

    return Optional.fromNullable(person);
  }

  /**
   * Sincronizza i dati della persona indicata con quelli presenti 
   * nell'anagrafica.
   * Vengono sincronizzati i dati di base della persona ed i suoi contratti.
   *
   * @param person la persona da sincronizzare
   * @return il risultato della sincronizzazione
   */
  public SyncResult syncPerson(Person person) {
    Verify.verifyNotNull(person);
    val syncResult = new SyncResult();
    if (person.getPerseoId() == null) {
      return syncResult.setFailed()
          .add(String.format("Impossibile sincronizzare i dati di %s, "
              + "id anagrafica esterna non presente", person.getFullname()));
    }
    val registryPerson = peoplePerseoConsumer.perseoPersonByPerseoId(person.getPerseoId());
    if (!registryPerson.isPresent()) {
      return syncResult.setFailed()
          .add(String.format("Impossibile sincronizzare i dati di %s, "
              + "persona con id anagrafica = %s non presente in anagrafica", 
              person.getFullname(), person.getPerseoId()));      
    }
    syncResult.add(syncPersonWithPersonRegistry(person, registryPerson.get()));
    syncResult.add(syncContracts(person));
    return syncResult;
  }

  /**
   * Aggiorna la persona presente in ePAS con i dati della persona prelevata dall'anagrafica.
   *
   * @return una stringa con la descrizione dei cambiamenti se ci sono stati. empty altrimenti.
   */
  public SyncResult syncPersonWithPersonRegistry(Person epasPerson, Person registryPerson) {

    val syncResult = new SyncResult();

    if (!epasPerson.getName().equals(registryPerson.getName())) {
      syncResult.add(
          String.format("Cambiato nome da %s a %s", epasPerson.getName(), registryPerson.getName()));
      epasPerson.setName(registryPerson.getName());
    }
    if (!epasPerson.getSurname().equals(registryPerson.getSurname())) {
      syncResult.add(String.format("Cambiato cognome da %s a %s", 
          epasPerson.getSurname(), registryPerson.getSurname()));
      epasPerson.setSurname(registryPerson.getSurname());
    }
    if ((epasPerson.getNumber() == null && registryPerson.getNumber() != null) 
        || epasPerson.getNumber() != null && registryPerson.getNumber() != null 
        && !epasPerson.getNumber().equals(registryPerson.getNumber())) {
      epasPerson.setNumber(registryPerson.getNumber());
      syncResult.add(String.format("Assegnato il numero di matricola %s a %s", 
          epasPerson.getNumber(), epasPerson.getFullname()));
    }
    if (epasPerson.getPerseoId() == null 
        || !epasPerson.getPerseoId().equals(registryPerson.getPerseoId())) {
      epasPerson.setPerseoId(registryPerson.getPerseoId());
      syncResult.add(String.format("Assegnato il campo perseoId %s a %s", 
          epasPerson.getPerseoId(), epasPerson.getFullname()));
    }
    if (epasPerson.getQualification() == null 
        || !epasPerson.getQualification().equals(registryPerson.getQualification())) {
      epasPerson.setQualification(registryPerson.getQualification());
      syncResult.add(String.format("Cambiata qualifica a %s per %s", 
          epasPerson.getQualification(), epasPerson.getFullname()));
    }
    if (!syncResult.getMessages().isEmpty()) {
      epasPerson.save();
    }
    return syncResult;
  }


  /**
   * Effettua l'importazione di un contratto di una perseo dall'anagrafica.
   *
   * @param perseoContractId l'id in anagrafica del contratto da importare
   * @param epasPersonId l'id della persona in ePAS
   */
  public SyncResult importContract(String perseoContractId, Long epasPersonId) {
    Person person = personDao.getPersonById(epasPersonId);
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.getPerseoId());

    val syncResult = new SyncResult();
    Optional<Contract> contractInPerseo = Optional.absent();
    try {
      contractInPerseo = contractPerseoConsumer.perseoContractPerseoId(perseoContractId, person);
    } catch (ApiRequestException ex) {
      log.warn("Problemi nell'importazione del contratto di {}", 
          person.getFullname(), ex);

      return syncResult.setFailed()
          .add(
              String.format("Problemi nell'importazione dei dati del contratto di %s. "
                  + "Impossibile importare il contratto: %s", 
                  person.getFullname(), ex.toString()));
    }

    if (contractInPerseo.isPresent()) {
      // Salvare il contratto.
      if (!contractManager.properContractCreate(contractInPerseo.get(), Optional.absent(), false)) {
        return 
            syncResult.setFailed()
            .add(String.format("Il contratto di %s non può essere importato a causa di errori",
                person.getFullname()));
      }
      log.info("Importato con successo il contratto di {}. {}", 
          person.getFullname(), contractInPerseo.get());
      return syncResult.add(
          String.format("Contratto di %s importato con successo dall'anagrafica", 
              person.toString()));
    }

    log.warn("Problemi nell'importazione del contratto di {}. "
        + "Contratto non presente in anagrafica.", person.getFullname());    
    return syncResult.setFailed()
        .add(String.format("Contratto di {} non presente in anagrafica", person.getFullname()));
  }

  /**
   * Effettua l'importazione dei contratti di una persona dall'anagrafica.
   *
   * @param person la persona in ePAS di cui importare i contratti.
   */
  public SyncResult importContracts(Person person) {
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.getPerseoId());

    val syncResult = new SyncResult();
    List<Contract> contractsInRegistry = Lists.newArrayList();
    try {
      contractsInRegistry = 
          contractPerseoConsumer.fetchRegistryContractsByRegistry(person.getPerseoId(), person);
    } catch (ApiRequestException ex) {
      log.warn("Problemi nell'importazione dei contratti di {}", 
          person.getFullname(), ex);
      return syncResult.setFailed()
          .add(
              String.format("Problemi nell'importazione dei dati del contratto di %s. "
                  + "Impossibile importare il contratto: %s", 
                  person.getFullname(), ex.toString()));
    }

    contractsInRegistry.stream().forEach(registryContract -> {
      // Salvare il contratto.
      if (!contractManager.properContractCreate(registryContract, Optional.absent(), false)) {
        syncResult.setFailed()
            .add(String.format(
            "Il contratto di %s non può essere importato a causa di errori",
            person.getFullname()));
      }
      log.info("Importato con successo il contratto di {}. {}", 
          person.getFullname(), registryContract);
      syncResult.add(
          String.format("Contratto di %s importato con successo dall'anagrafica", 
              person.toString()));
    });

    if (contractsInRegistry.isEmpty()) {
      log.warn("Problemi nell'importazione del contratto di {}. "
          + "Contratto non presente in anagrafica.", person.getFullname());    
      syncResult.setFailed()
          .add(String.format("Contratto di {} non presente in anagrafica", person.getFullname()));

    }
    return syncResult;

  }

  /**
   * Sincronizza i contratti presenti in ePAS con quelli presenti in anagrafica.
   *
   * @param person la persona di cui sincronizzare i contratti.
   * @return il risultato della sincronizzazione.
   */
  public SyncResult syncContracts(Person person) {
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.getPerseoId());

    val syncResult = new SyncResult();
    List<Contract> contractsInRegistry = Lists.newArrayList();
    try {
      contractsInRegistry = 
          contractPerseoConsumer.fetchRegistryContractsByRegistry(person.getPerseoId(), person);
    } catch (ApiRequestException ex) {
      log.warn("Problemi nell'importazione dei contratti di {}", 
          person.getFullname(), ex);
      return syncResult.setFailed()
          .add(String.format("Problemi nell'importazione dei dati del contratto di %s. "
              + "Impossibile importare il contratto: %s", 
              person.getFullname(), ex.toString()));
    }

    if (contractsInRegistry.isEmpty() && person.getContracts().isEmpty()) {
      log.debug("Non ci sono contratti presenti per {} ne in anagrafica ne in ePAS."
          + "Niente da sincronizzare.", person.getFullname());
      return syncResult;
    }

    contractsInRegistry.forEach(contractInRegistry -> {
      log.debug("Verifico il matching del contratto {}", contractInRegistry);
      val matchingContracts = matchingContracts(contractInRegistry, person);
      if (matchingContracts.isPresent()) {
        log.debug("Trovata corrispondenza in ePAS del contratto {}",
            contractInRegistry);
        syncResult.add(syncContract(
            matchingContracts.get().getRegistryContract(), 
            matchingContracts.get().getEpasContract()));        
      } else {
        // Salvare il contratto.
        log.debug("Contratto {} di {} non presente in ePAS, procedo alla creazione",
            contractInRegistry, person.getFullname());
        if (!contractManager.properContractCreate(contractInRegistry, Optional.absent(), false)) { 
          syncResult.setFailed()
              .add(String.format("Il contratto di %s non può essere importato a causa di errori",
              person.getFullname()));
        } else {
          log.info("Creato in ePAS un nuovo contratto per {}. {}", 
              person.getFullname(), contractInRegistry);
        }
      }
    });
    return syncResult;
  }

  /**
   * Verifica se tra i contratti passati c'è n'è uno che corrisponde ai contratti
   * della persona.
   *
   * @param contract il contratti dall'anagrafica
   * @param person persona di cui verificare i contratti
   * @return i Contratti che corrispondono al match.
   */
  private Optional<MatchingContracts> matchingContracts(
      Contract contract, Person person) {
    Verify.verifyNotNull(contract);
    Verify.verifyNotNull(person);
    MatchingContracts matchingContracts = new MatchingContracts();

    java.util.Optional<Contract> matchingContract = 
        person.getContracts().stream().filter(
            c ->
            c.getPerson().id.equals(contract.getPerson().id) 
            && 
            (c.getPerseoId() != null && c.getPerseoId().equals(contract.getPerseoId())) 
            || 
            (c.getBeginDate() != null && c.getBeginDate().equals(contract.getBeginDate())
            ))
        .findAny();
    if (matchingContract.isPresent()) {
      matchingContracts.setRegistryContract(contract);
      matchingContracts.setEpasContract(matchingContract.get());
    }

    if (matchingContracts.isEmpty()) {
      return Optional.absent();
    }
    return Optional.of(matchingContracts);
  }

  /**
   * DTO per contenere due contratti consistenti tra ePAS e Perseo.
   */
  @Data
  public class MatchingContracts {
    private Contract registryContract = null;
    private Contract epasContract = null;

    private boolean isEmpty() {
      return registryContract == null || epasContract == null;
    }
  }

  private SyncResult syncContract(Contract registryContract, Contract epasContract) {
    val syncResult = new SyncResult();
    log.debug("Inizio sincronizzazione contratti. Registry contract = {}. ePAS contract = {}",
        registryContract, epasContract);
    if (!matching(registryContract.getBeginDate(), epasContract.getBeginDate())) {
      epasContract.setBeginDate(registryContract.getBeginDate());
      syncResult.add(
          String.format("Aggiornata la data di inizio del contratto di %s a %s",
              epasContract.getPerson().getFullname(), epasContract.getBeginDate()));
    }
    if (!matching(registryContract.getEndDate(), epasContract.getEndDate())) {
      epasContract.setEndDate(registryContract.getEndDate());
      syncResult.add(
          String.format("Aggiornata la data di fine del contratto di %s a %s",
              epasContract.getPerson().getFullname(), epasContract.getEndDate()));
    }
    if (!matching(registryContract.getEndContract(), epasContract.getEndContract())) {
      epasContract.setEndContract(registryContract.getEndContract());
      syncResult.add(
          String.format("Aggiornata la data di terminazione contratto di %s a %s",
              epasContract.getPerson().getFullname(), epasContract.getEndContract()));
    }
    if (registryContract.getPerseoId() != null 
        && !registryContract.getPerseoId().equals(epasContract.getPerseoId())) {
      epasContract.setPerseoId(registryContract.getPerseoId());
      syncResult.add(
          String.format("Aggiornato l'identificato anagrafico del contratto di %s a %s",
              epasContract.getPerson().getFullname(), epasContract.getPerseoId()));
    }
    
    if (syncResult.getMessages().size() > 0) {
      if (contractManager.properContractUpdate(epasContract, null, false)) {
        log.info("Aggiornato il contratto di {}. {}", 
            epasContract.getPerson().getFullname(), syncResult);
        epasContract.save();     
      } else {
        syncResult.setFailed()
            .add(String.format("Il contratto di %s non può essere aggiornato a causa di errori.",
            epasContract.getPerson().getFullname()));        
      }      
    }
    
    return syncResult;
  }
  
  private boolean matching(LocalDate firstDate, LocalDate secondDate) {
    return (firstDate != null && firstDate.equals(secondDate)) 
        || (firstDate == null && secondDate == null);
  }
}
