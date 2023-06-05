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

package controllers;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import helpers.rest.ApiRequestException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.BadgeManager;
import manager.ContractManager;
import manager.OfficeManager;
import manager.PeriodManager;
import manager.UserManager;
import manager.configurations.ConfigurationManager;
import manager.sync.SynchronizationManager;
import models.Contract;
import models.Institute;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;
import synch.perseoconsumers.contracts.ContractPerseoConsumer;
import synch.perseoconsumers.office.OfficePerseoConsumer;
import synch.perseoconsumers.people.PeoplePerseoConsumer;
import synch.perseoconsumers.people.PerseoPerson;
import synch.perseoconsumers.roles.RolePerseoConsumer;

/**
 * Controller per la sincronizzazione dei dati del personale tra ePAS e Perseo.
 *
 * @author Marco Andreini
 *
 */
@Slf4j
@With(Resecure.class)
public class Synchronizations extends Controller {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static ContractDao contractDao;
  @Inject
  static RoleDao roleDao;
  @Inject
  static OfficePerseoConsumer officePerseoConsumer;
  @Inject
  static PeoplePerseoConsumer peoplePerseoConsumer;
  @Inject
  static ContractPerseoConsumer contractPerseoConsumer;
  @Inject
  static RolePerseoConsumer rolePerseoCunsomer;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static UserManager userManager;
  @Inject
  static OfficeManager officeManager;
  @Inject
  static BadgeManager badgeManager;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static ContractManager contractManager;
  @Inject
  static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  static UsersRolesOfficesDao usersRolesOfficesDao;
  @Inject
  static SynchronizationManager synchronizationManager;
  
  /**
   * Gli istituti perseo.
   */
  public static void institutes() {

    List<Institute> perseoInstitutes = null;
    try {
      perseoInstitutes = officePerseoConsumer.perseoInstitutes();
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    List<Institute> institutes = officeDao.institutes(Optional.absent(),
        Optional.absent(), Optional.absent(),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECHNICAL_ADMIN)).list();

    Map<Long, Institute> epasInstituteByPerseoId = Maps.newHashMap();
    Map<Long, Office> epasOfficesByPerseoId = Maps.newHashMap();
    for (Institute institute : institutes) {
      if (institute.getPerseoId() != null) {
        epasInstituteByPerseoId.put(institute.getPerseoId(), institute);
      }
      for (Office office : institute.getSeats()) {
        if (office.getPerseoId() != null) {
          epasOfficesByPerseoId.put(office.getPerseoId(), office);
        }
      }
    }

    render(perseoInstitutes, epasInstituteByPerseoId, epasOfficesByPerseoId);
  }

  /**
   * Gli istituti in epas non sincronizzare.
   */
  public static void oldInstitutes() {

    List<Institute> institutes = officeDao.institutes(Optional.absent(),
        Optional.absent(), Optional.absent(),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECHNICAL_ADMIN)).list();

    Map<String, Institute> perseoInstitutesByCds = null;
    try {
      perseoInstitutesByCds = officePerseoConsumer.perseoInstitutesByCds();
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    Map<String, Office> perseoOfficeByCodeId = Maps.newHashMap();

    if (perseoInstitutesByCds != null) {
      for (Institute institute : perseoInstitutesByCds.values()) {
        for (Office office : institute.getSeats()) {
          perseoOfficeByCodeId.put(office.getCodeId(), office);
        }
      }
    }

    render(institutes, perseoInstitutesByCds, perseoOfficeByCodeId);
  }


  /**
   * Lega l'istituto epas al perseoId. Da utilizzare manualmente con cautela!!!
   *
   * @param epasInstituteId id Istituto
   * @param perseoId Perseo id dell'istituto.
   */
  public static void joinInstitute(Long epasInstituteId, Long perseoId) {
    Optional<Institute> institute = officeDao.instituteById(epasInstituteId);
    Verify.verify(institute.isPresent());
    Verify.verifyNotNull(perseoId);

    Optional<Institute> instituteInPerseo = Optional.absent();

    try {
      instituteInPerseo = officePerseoConsumer
          .perseoInstituteByInstitutePerseoId(perseoId);
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    if (instituteInPerseo.isPresent()) {
      //copy ( TODO: update method)
      institute.get().setPerseoId(instituteInPerseo.get().getPerseoId());
      institute.get().setCds(instituteInPerseo.get().getCds());
      institute.get().setCode(instituteInPerseo.get().getCode());
      institute.get().setName(instituteInPerseo.get().getName());
      institute.get().save();

      log.info("Associato istituto={} al perseoId={}", institute.get(), perseoId);
      flash.success("Operazione effettuata correttamente");
    }

    oldInstitutes();
  }

  /**
   * Lega la sede epas al perseoId. Da utilizzare manualmente con cautela!!!
   */
  public static void joinOffice(Long epasOfficeId, Long perseoId) {
    Office office = officeDao.getOfficeById(epasOfficeId);
    Verify.verifyNotNull(office);
    Verify.verifyNotNull(perseoId);

    Optional<Institute> instituteWithThatSeat = Optional.absent();
    try {
      instituteWithThatSeat = officePerseoConsumer.perseoInstituteByOfficePerseoId(perseoId);
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
      oldInstitutes();
    }

    if (instituteWithThatSeat.isPresent()) {
      Office perseoOffice = instituteWithThatSeat.get().getSeats().iterator().next();

      //copy ( TODO: update method)
      office.setPerseoId(perseoOffice.getPerseoId());
      office.setCode(perseoOffice.getCode());
      office.setName(perseoOffice.getName());
      office.setAddress(perseoOffice.getAddress());

      office.save();

      log.info("Associata sede={} al perseoId={}", office, perseoId);
      flash.success("Operazione effettuata correttamente");
    }

    oldInstitutes();
  }

  /**
   * Importa la sede da perseo, (e l'istituto se non è già presente).
   *
   * @param seatPerseoId Perseo ID della sede.
   */
  public static void importOffice(Long seatPerseoId) {

    //Prendere da perseo quella sede.
    Optional<Institute> instituteWithThatSeat = Optional.absent();
    try {
      instituteWithThatSeat = officePerseoConsumer.perseoInstituteByOfficePerseoId(seatPerseoId);
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    if (!instituteWithThatSeat.isPresent()) {
      flash.error("Niente da fare :(((.");
      institutes();
    }
    Office seat = instituteWithThatSeat.get().getSeats().iterator().next();

    // Salvataggio istituto
    Optional<Institute> institute = officeDao.byCds(instituteWithThatSeat.get().getCds());
    if (!institute.isPresent()) {

      //Istituto non presente
      Validation.valid("instituteWithThatSeat", instituteWithThatSeat.get());
      if (Validation.hasErrors()) {
        // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
        log.info("L'importazione della sede con perseoId={} ha comportato errori di validazione "
            + "nel suo istituto. errors={}.", seatPerseoId, validation.errorsMap());
        flash.error("La sede selezionata non può essere importata a causa di errori. [%s]",
            validation.errorsMap());
        institutes();
      }

      instituteWithThatSeat.get().save();
      institute = instituteWithThatSeat;
    } else {

      //Istituto già presente, aggiungo la nuova sede alla sua lista e sistemo la relazione.
      institute.get().getSeats().add(seat);
      seat.setInstitute(institute.get());
    }

    // TODO: spostare in un creator epas che venga utilizzato anche nelle crud
    // (finchè non spariranno).
    seat.setBeginDate(new LocalDate(LocalDate.now().getYear() - 1, 12, 31));
    periodManager.updatePropertiesInPeriodOwner(seat);

    //Salvataggio sede
    validation.valid(seat);
    if (Validation.hasErrors()) {
      // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
      log.info("L'importazione della sede con perseoId={} ha comportato errori di validazione "
          + "nella sede. errors={}.", seatPerseoId, validation.errorsMap());
      flash.error("La sede selezionata non può essere importata a causa di errori. [%s]",
          validation.errorsMap());
      institutes();
    }

    seat.save();

    // Configurazione iniziale di default ...
    configurationManager.updateConfigurations(seat);

    // Importato correttamente importo tutte le persone e tutti i contratti ...
    managerImportAllPersonInOffice(seat);
    JPAPlugin.closeTx(false);
    JPAPlugin.startTx(false);
    JPA.em().merge(seat);
    managerImportAllActiveContractsInOffice(seat);
    JPAPlugin.closeTx(false);
    JPAPlugin.startTx(false);

    // Importazione dei ruoli ...
    List<UsersRolesOffices> uroList = rolePerseoCunsomer.perseoUsersRolesOffices(seat);
    for (UsersRolesOffices uro : uroList) {
      if (!uro.isPersistent()) {
        uro.save();
      }
    }

    flash.success("La sede %s è stata importata con successo da Perseo!", seat.toString());

    institutes();
  }

  /**
   * Sincronizzazione persone.
   *
   * @param officeId sede
   */
  public static void people(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    if (office.getPerseoId() == null) {
      flash.error("Selezionare una sede già sincronizzata... "
          + "%s non lo è ancora.", office.toString());
      institutes();
    }
    Map<Long, Person> perseoPeople = new HashMap<>(0);
    Map<Long, Person> epasSynchronizedPeople = new HashMap<>(0);
    Map<Long, List<Contract>> perseoPeopleContract = new HashMap<>(0);
    Map<Long, IWrapperPerson> epasWrapperedPeople = new HashMap<>(0);
    Map<Long, Set<String>> epasPeopleUros = new HashMap<>(0);
    Map<Long, Set<String>> perseoPeopleUros = new HashMap<>(0);
    try {
      perseoPeople = peoplePerseoConsumer
          .perseoPeopleByPerseoId(Optional.of(office.getPerseoId()));
      epasSynchronizedPeople = personDao.mapSynchronized(Optional.of(office));
      perseoPeopleContract = contractPerseoConsumer
          .perseoPeopleContractsMap(Optional.of(office));

      for (IWrapperPerson person : personDao.list(Optional.of(office)).list().stream()
          .map(wrapperFunctionFactory.person()).collect(Collectors.toList())) {
        epasWrapperedPeople.put(person.getValue().id, person);
      }

      // Tutti i ruoli epas formato Map<perseoPersonId, Set<String>> Contenente tutti gli
      // i ruoli (amministrativi) che ogni persona dell'office ha (anche in altri office).
      epasPeopleUros = usersRolesOfficesDao
          .getEpasRoles(Optional.fromNullable(office));
      perseoPeopleUros = rolePerseoCunsomer.perseoRoles(Optional.of(office));
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    render(office, perseoPeople, epasSynchronizedPeople, perseoPeopleContract, epasWrapperedPeople,
        epasPeopleUros, perseoPeopleUros);
  }

  /**
   * Le persone in epas non sincronizzate.
   */
  public static void oldPeople(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    if (office.getPerseoId() == null) {
      flash.error("Per sincronizzare le persone occorre che la sede sia anch'essa sincronizzata");
      oldInstitutes();
    }

    List<Person> people = personDao.listFetched(Optional.absent(), Sets.newHashSet(office),
        false, null, null, false).list();

    List<IWrapperPerson> wrapperedPeople = FluentIterable.from(people)
        .transform(wrapperFunctionFactory.person()).toList();

    Map<String, Person> perseoPeopleByNumber = Maps.newHashMap();
    if (office.getPerseoId() != null) {
      try {
        perseoPeopleByNumber =
            peoplePerseoConsumer.perseoPeopleByNumber(Optional.of(office.getPerseoId()));
      } catch (ApiRequestException ex) {
        flash.error("%s", ex);
      }
    }
    render(wrapperedPeople, perseoPeopleByNumber, office);
  }

  /**
   * Esegue il join per una persona specifica.
   *
   * @param epasPersonId id della Persona
   * @param perseoId Perseo id da legare alla persona specificata.
   */
  public static void joinPerson(Long epasPersonId, Long perseoId) {

    Person person = personDao.getPersonById(epasPersonId);
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(perseoId);

    Optional<Person> personInPerseo = Optional.absent();
    try {
      personInPerseo = peoplePerseoConsumer.perseoPersonByPerseoId(perseoId);
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    if (personInPerseo.isPresent()) {
      join(person, personInPerseo.get());
      flash.success("Operazione effettuata correttamente");
    }

    oldPeople(person.getOffice().id);
  }

  // TODO: spostare nell'updater?

  /**
   * Permette la join tra la persona presente su ePAS e quella presente su Perseo 
   * (anagrafica).
   *
   * @param epasPerson Person presente su ePas
   * @param perseoPerson Person prelevata da Perseo.
   */
  private static void join(Person epasPerson, Person perseoPerson) {
    //copy ( TODO: update method)

    if (perseoPerson.getQualification() == null) {
      log.info("Impossibile associare la persona {} con qualifica nulla perseoId={}",
          epasPerson, epasPerson.getPerseoId());
      return;
    }

    epasPerson.setName(perseoPerson.getName());
    epasPerson.setSurname(perseoPerson.getSurname());
    epasPerson.setNumber(perseoPerson.getNumber());
    // per adesso le email non combaciano @iit.cnr.it vs @cnr.it
    epasPerson.setEmail(perseoPerson.getEmail());
    epasPerson.setEppn(perseoPerson.getEmail());
    epasPerson.setQualification(perseoPerson.getQualification());
    epasPerson.setPerseoId(perseoPerson.getPerseoId());
    epasPerson.save();
    log.info("Associata persona={} al perseoId={}", epasPerson, epasPerson.getPerseoId());
  }

  /**
   * Esegue il join per tutte le persone non ancora associate (se esiste un suggerimento perseo via
   * matricola).
   *
   * @param epasOfficeId id dell'ufficio.
   */
  public static void joinAllPersonInOffice(Long epasOfficeId) {

    Office office = officeDao.getOfficeById(epasOfficeId);
    notFoundIfNull(office);

    List<Person> people = personDao.listFetched(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();

    Map<String, Person> perseoPeopleByNumber = null;
    if (office.getPerseoId() == null) {
      flash.error("Impossibile effettuare la join delle persone per ufficio {}. "
          + "Id anagrafica esterna non presente.",
          office.getLabel());
      oldPeople(office.id);
      return;
    }
    try {
      perseoPeopleByNumber = peoplePerseoConsumer
          .perseoPeopleByNumber(Optional.fromNullable(office.getPerseoId()));
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    if (!people.isEmpty() && perseoPeopleByNumber != null) {
      int synced = 0;
      for (Person person : people) {
        if (person.getPerseoId() == null) {
          Person perseoPerson = perseoPeopleByNumber.get(person.getNumber());
          if (perseoPerson != null) {
            join(person, perseoPerson);
            synced++;
          }
        }
      }
      flash.success("Sincronizzate correttamente %d persone", synced);
    }
    oldPeople(office.id);
  }

  /**
   * Posso importare una persona da perseo.. purchè non esista già una persona con quella
   * matricola.
   */
  public static void importPerson(Long perseoId) {

    //Prendere da perseo quella persona.
    Optional<Person> personInPerseo = Optional.absent();
    try {
      personInPerseo = peoplePerseoConsumer.perseoPersonByPerseoId(perseoId);
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    Optional<Office> office = Optional.absent();

    if (personInPerseo.isPresent()) {
      // Caricare dalla persona l'office.
      office = officeDao.byPerseoId(personInPerseo.get().getPerseoOfficeId());
      if (!office.isPresent()) {
        flash.error("L'ufficio di appartenenza della persona selezionata non è "
            + "ancora sincronizzato con Perseo");
        people(null);
      }

      personInPerseo.get().setOffice(office.get());
      personInPerseo.get().setBeginDate(LocalDate.now().withDayOfMonth(1)
          .withMonthOfYear(1).minusDays(1));

      validation.valid(personInPerseo.get());
      if (Validation.hasErrors()) {
        // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
        log.info("L'importazione della persone con perseoId={} ha comportato errori di validazione "
            + "nella persona. errors={}.", perseoId, validation.errorsMap());
        flash.error("La persona selezionata non può essere importata a causa di errori. [%s]",
            validation.errorsMap());
        people(office.get().id);
      }

      // Creazione!
      if (!synchronizationManager.personCreator(personInPerseo.get()).isPresent()) {
        flash.error("La persona selezionata non può essere importata a causa di errori.");
      } else {
        flash.success("La persona %s è stata importata con successo da Perseo!",
            personInPerseo.get().toString());
      }
    }

    people(office.get().id);
  }

  /**
   * Importa tutte le persone di perseo non ancora presenti in epas.
   */
  private static boolean managerImportAllPersonInOffice(Office office) {
    Verify.verifyNotNull(office);
    Verify.verifyNotNull(office.getPerseoId());
    Map<Long, Person> perseoPeopleByPerseoId = null;
    try {
      perseoPeopleByPerseoId = peoplePerseoConsumer
          .perseoPeopleByPerseoId(Optional.fromNullable(office.getPerseoId()));
    } catch (ApiRequestException ex) {
      ex.printStackTrace();
    }

    List<Person> people = personDao.listFetched(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();

    // questa operazione si può fare solo se non ci sono persone in epas
    // attualmente non associate a perseo.

    Map<Long, Person> epasPeopleByPerseoId = Maps.newHashMap();
    for (Person person : people) {
      if (person.getPerseoId() != null) {
        epasPeopleByPerseoId.put(person.getPerseoId(), person);
      } else {
        return false;
      }
    }

    for (Person perseoPerson : perseoPeopleByPerseoId.values()) {
      if (!epasPeopleByPerseoId.containsKey(perseoPerson.getPerseoId())) {

        log.debug("Provo name:{} matricola:{} qualifica:{} perseoId:{}",
            perseoPerson.fullName(), perseoPerson.getNumber(),
            perseoPerson.getQualification(), perseoPerson.getPerseoId());

        // join dell'office (in automatico ancora non c'è...)
        perseoPerson.setOffice(office);
        perseoPerson.setBeginDate(LocalDate.now().withDayOfMonth(1)
            .withMonthOfYear(1).minusDays(1));
        validation.valid(perseoPerson);
        if (Validation.hasErrors()) {
          // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
          log.info("L'importazione della persona con perseoId={} ha comportato errori di "
                  + "validazione nella persona. errors={}.",
              perseoPerson.getPerseoId(), validation.errorsMap());
          Validation.clear();
          continue;
        }

        // Creazione!
        if (!synchronizationManager.personCreator(perseoPerson).isPresent()) {
          // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
          log.info("L'importazione della persone con perseoId={} ha comportato errori di "
                  + "validazione nella persona. errors={}.",
              perseoPerson.getPerseoId(), validation.errorsMap());
          Validation.clear();
          continue;
        }
      }
    }
    return true;

  }

  /**
   * Importa tutte le persone presenti nell'ufficio con id epasOfficeId.
   *
   * @param epasOfficeId id dell'ufficio.
   */
  public static void importAllPersonInOffice(Long epasOfficeId) {

    Office office = officeDao.getOfficeById(epasOfficeId);
    notFoundIfNull(office);

    boolean result = managerImportAllPersonInOffice(office);

    if (!result) {
      flash.error("Per fare questa operazione tutte le persone già esistenti della sede "
          + "devono essere correttamente sincronizzate. Operazione annullata.");
      people(office.id);
    }

    flash.success("Operazione effettuata correttamente");
    people(office.id);

  }

  /**
   * I contratti attivi in epas non sincronizzati.
   */
  public static void oldActiveContracts(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    //La mappa di tutti i contratti attivi delle persone sincronizzate epas.
    @SuppressWarnings("deprecation")
    Map<Long, Contract> activeContractsEpasByPersonPerseoId =
        contractPerseoConsumer.activeContractsEpasByPersonPerseoId(office);

    Map<Long, Contract> perseoDepartmentActiveContractsByPersonPerseoId = Maps.newHashMap();

    if (office.getPerseoId() == null) {
      flash.error("Selezionare una sede già sincronizzata... "
          + "%s non lo è ancora.", office.toString());
    } else {
      //Costruisco la mappa di tutti i contratti attivi perseo per le persone sincronizzate epas.
      try {
        perseoDepartmentActiveContractsByPersonPerseoId = contractPerseoConsumer
            .perseoDepartmentActiveContractsByPersonPerseoId(office.getPerseoId(), office);
      } catch (ApiRequestException ex) {
        flash.error("%s", ex);
      }
    }
    render(activeContractsEpasByPersonPerseoId, perseoDepartmentActiveContractsByPersonPerseoId,
        office);
  }

  /**
   * Esegue il join per una persona specifica.
   *
   * @param epasContractId id del Contratto su ePas
   * @param perseoId id del Contratto su Perseo.
   */
  public static void joinContract(Long epasContractId, String perseoId) {

    Contract contract = contractDao.getContractById(epasContractId);
    Verify.verifyNotNull(contract);
    Verify.verifyNotNull(perseoId);

    Optional<Contract> contractInPerseo = Optional.absent();
    try {
      contractInPerseo = contractPerseoConsumer
          .perseoContractPerseoId(perseoId, contract.getPerson());
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }
    if (contractInPerseo.isPresent()) {
      joinUpdateContract(contract, contractInPerseo.get());
      flash.success("Operazione effettuata correttamente");
    }

    oldActiveContracts(contract.getPerson().getOffice().id);
  }

  /**
   * Associa tutti i contratti attivi.
   *
   * @param officeId officeId
   */
  public static void joinAllActiveContractsInOffice(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    //La mappa di tutti i contratti attivi delle persone sincronizzate epas.
    @SuppressWarnings("deprecation")
    Map<Long, Contract> activeContractsEpasByPersonPerseoId =
        contractPerseoConsumer.activeContractsEpasByPersonPerseoId(office);

    Map<Long, Contract> perseoDepartmentActiveContractsByPersonPerseoId = Maps.newHashMap();

    if (office.getPerseoId() == null) {
      flash.error("Selezionare una sede già sincronizzata... "
          + "%s non lo è ancora.", office.toString());
    } else {
      //Costruisco la mappa di tutti i contratti attivi perseo per le persone sincronizzate epas.
      try {
        perseoDepartmentActiveContractsByPersonPerseoId = contractPerseoConsumer
            .perseoDepartmentActiveContractsByPersonPerseoId(office.getPerseoId(), office);
      } catch (ApiRequestException ex) {
        flash.error("%s", ex);
      }
    }

    for (Contract epasContract : activeContractsEpasByPersonPerseoId.values()) {
      if (epasContract.getPerseoId() == null) {
        Contract perseoContract =
            perseoDepartmentActiveContractsByPersonPerseoId
              .get(epasContract.getPerson().getPerseoId());
        if (perseoContract != null) {
          joinUpdateContract(epasContract, perseoContract);
        }
      }
    }

    flash.success("Operazione effettuata correttamente");
    oldActiveContracts(office.id);
  }

  // TODO: spostare nell'updater?

  /**
   * Permette l'aggiornamento del contratto tra la persona presente su ePAS e quella 
   * presente su Perseo (anagrafica).
   *
   * @param epasContract contratto presente su ePas
   * @param perseoContract contratto Prelevato da Perseo.
   */
  private static void joinUpdateContract(Contract epasContract, Contract perseoContract) {
    //copy ( TODO: update method)

    epasContract.setBeginDate(perseoContract.getBeginDate());
    if (perseoContract.isTemporaryMissing() && perseoContract.getEndDate() == null) {
      // TODO caso particolare
    } else {
      epasContract.setEndDate(perseoContract.getEndDate());
    }
    epasContract.setEndContract(perseoContract.getEndContract());
    epasContract.setTemporaryMissing(perseoContract.isTemporaryMissing());
    epasContract.setPerseoId(perseoContract.getPerseoId());

    // TODO: update periods e ricalcoli!!!

    epasContract.save();

    log.info("Associata contratto={} al perseoId={}",
        epasContract, epasContract.getPerseoId());
  }

  /**
   * Posso importare un contratto da perseo... purchè la sua persona sia sincronizzata e non
   * conflitti con le date dei contratti epas.
   */
  public static void importContract(String perseoId, Long epasPersonId) {

    Person person = personDao.getPersonById(epasPersonId);
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.getPerseoId());
    
    val syncResult = synchronizationManager.importContract(perseoId, epasPersonId);
    if (syncResult.isSuccess()) {
      flash.success(syncResult.toString());
    } else {
      flash.error(syncResult.toString());
    }

    people(person.getOffice().id);
  }

  /**
   * Importa tutti i contratti attivi delle persone sincronizzate della sede.
   */
  private static Optional<Exception> managerImportAllActiveContractsInOffice(Office office) {

    //La mappa di tutti i contratti attivi delle persone sincronizzate epas.
    @SuppressWarnings("deprecation")
    Map<Long, Contract> activeContractsEpasByPersonPerseoId =
        contractPerseoConsumer.activeContractsEpasByPersonPerseoId(office);

    //Costruisco la mappa di tutti i contratti attivi perseo per le persone sincronizzate epas.
    Map<Long, Contract> perseoDepartmentActiveContractsByPersonPerseoId = null;
    try {
      perseoDepartmentActiveContractsByPersonPerseoId = contractPerseoConsumer
          .perseoDepartmentActiveContractsByPersonPerseoId(office.getPerseoId(), office);
    } catch (ApiRequestException ex) {
      return Optional.of(ex);
    }

    if (perseoDepartmentActiveContractsByPersonPerseoId != null) {
      for (Contract perseoContract : perseoDepartmentActiveContractsByPersonPerseoId.values()) {
        Contract epasContract =
            activeContractsEpasByPersonPerseoId.get(perseoContract.getPerson().getPerseoId());
        if (epasContract != null) {
          continue;
        }
        // Salvare il contratto.
        if (!contractManager.properContractCreate(perseoContract, Optional.absent(), false)) {
          flash.error("Problemi nel salvare il contratto con id = %s.", perseoContract.id);
          log.warn("Impossibil e importare il contratto con id = {}", perseoContract.id);
          // TODO segnalare il conflitto
        }
      }
    }

    return Optional.absent();
  }

  /**
   * Posso importare un contratto da perseo... purchè la sua persona sia sincronizzata e non
   * conflitti con le date dei contratti epas.
   */
  public static void importAllContractsInOffice(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    Optional<Exception> errors = managerImportAllActiveContractsInOffice(office);
    if (errors.isPresent()) {
      flash.error("%s", errors.get());
    } else {
      flash.error("Tutti i contratti importabili sono stati importati.");
    }

    people(office.id);
  }

  /**
   * Azzera la sincronizzazione del'istituto, delle sedi, delle persone e dei contratti.
   *
   * @param instituteId istituto
   */
  public static void unjoinInstitute(Long instituteId) {

    Optional<Institute> instituteOpt = officeDao.instituteById(instituteId);
    if (!instituteOpt.isPresent()) {
      notFound();
    }
    Institute institute = instituteOpt.get();
    institute.setPerseoId(null);
    institute.save();
    for (Office office : institute.getSeats()) {

      office.setPerseoId(null);
      office.save();
      for (Person person : office.getPersons()) {

        person.setPerseoId(null);
        person.save();
        for (Contract contract : person.getContracts()) {
          contract.setPerseoId(null);
          contract.save();
        }
      }
    }

    flash.success("Istituto %s desincronizzato.", institute.getName());
    oldInstitutes();
  }

  /**
   * Mostra i badge associati ad un determinato ufficio.
   */
  public static void badges(Long officeId) {
    if (officeId == null) {
      badRequest("officeId non valido");
    }

    final Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    final List<Person> people = office.getPersons();
    render(office, people);
  }

  /**
   * Importa tutti i badge non ancora presenti su ePAS e sincronizza
   * quelli già esistenti.
   */
  public static void importBadges(Long officeId) {

    if (officeId == null) {
      badRequest("officeId non valido");
    }

    final Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    
    try {
      val badges = badgeManager.importBadges(office);
      if (!badges.isEmpty()) {
        flash.success("Importati correttamente %s badge", badges.size());
      }
    } catch (RuntimeException e) {
      flash.error("Errore durante l'importazione dei badge, contattare "
          + "l'amministratore di ePAS");
    }

    badges(office.id);
  }

  /**
   * Mostra la form per la sincronizzazione del campo eppn.
   */
  public static void eppn(Long officeId) {
    if (officeId == null) {
      badRequest("officeId non valido");
    }

    final Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    final List<Person> people = office.getPersons();
    render(office, people);
  }

  /**
   * Sincronizza il parametro eppn delle persone della sede con id officeId.
   *
   * @param officeId l'identificativo della sede
   */
  public static void syncEppn(Long officeId) {

    if (officeId == null) {
      badRequest("officeId non valido");
    }

    final Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    if (office.getPerseoId() == null) {
      badRequest(
          String.format("Id Anagrafica Esterna non presente per l'ufficio %s, "
              + "impossibile completare la richiesta", office.getName()));
    }
    List<PerseoPerson> people = new ArrayList<>(0);
    try {
      people = peoplePerseoConsumer.perseoPeople(Optional.fromNullable(office.getPerseoId())).get();
    } catch (InterruptedException | ExecutionException e) {
      flash.error(
          "Errore nell'import delle persone dall'anagrafica per l'ufficio con PerseoId %s: %s",
          office.getPerseoId(), e.getMessage());
      eppn(office.id);
    }

    people.forEach(p -> {
      Person person = personDao.getPersonByPerseoId(p.id);
      if (person == null) {
        log.warn("Sincronizzazione Eppn: persona con perseoId={} non presente", p.id);
      } else if (p.eppn != null) {
        person.setEppn(p.eppn);
        person.save();
        log.info("Sincronizzato eppn per la persona {}", person);
      }
    });
    flash.success("Sincronizzazione campo Eppn Terminata con Successo", people.size(),
        office.getPersons().size());

    eppn(office.id);
  }

  /**
   * Sincronizza le persone presenti in un ufficio, prelevando le persone
   * assegnate all'ufficio dall'anagrafica esterna.
   *
   * @param id l'id dell'ufficio da sincronizzare.
   */
  public void syncPeopleInOffice(Long id) {
    Office office = Office.findById(id);
    notFoundIfNull(office);
    val result = synchronizationManager.syncPeopleInOffice(office, false);
    if (result.isSuccess()) {
      renderText(
          String.format("Assegnazione personale dell'ufficio %s sincronizzata", 
              office.getName()));
    } else {
      renderText(
          String.format("Sincronizzazione personale ufficio %s KO: %s",
              office.getName(), Joiner.on(",").join(result.getMessages())));
    }
  }

  /**
   * Sincronizza i dati di una persona presente in ePAS con quelli 
   * dell'anagrafica.
   *
   * @param id id in ePAS della persona da sincronizzare.
   */
  public static void syncPerson(Long id) {
    Verify.verifyNotNull(id);
    Person person = Person.findById(id);
    Verify.verifyNotNull(person);
    val result = synchronizationManager.syncPerson(person);
    if (result.isSuccess()) {
      flash.success("%s: ", person.getFullname(), result.toString());
    } else {
      flash.error("%s: %s", person.getFullname(), result.toString());
    }
    people(person.getOffice().id);    
  }
}
