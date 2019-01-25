package controllers;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.ContractManager;
import manager.OfficeManager;
import manager.PeriodManager;
import manager.UserManager;
import manager.configurations.ConfigurationManager;

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
import synch.perseoconsumers.roles.RolePerseoConsumer;

@Slf4j
@With({Resecure.class})
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
  static ConfigurationManager configurationManager;
  @Inject
  static ContractManager contractManager;
  @Inject
  static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  static UsersRolesOfficesDao usersRolesOfficesDao;

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

    List<Institute> institutes = officeDao.institutes(Optional.<String>absent(),
        Optional.<String>absent(), Optional.absent(),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECHNICAL_ADMIN)).list();

    Map<Long, Institute> epasInstituteByPerseoId = Maps.newHashMap();
    Map<Long, Office> epasOfficesByPerseoId = Maps.newHashMap();
    for (Institute institute : institutes) {
      if (institute.perseoId != null) {
        epasInstituteByPerseoId.put(institute.perseoId, institute);
      }
      for (Office office : institute.seats) {
        if (office.perseoId != null) {
          epasOfficesByPerseoId.put(office.perseoId, office);
        }
      }
    }

    render(perseoInstitutes, epasInstituteByPerseoId, epasOfficesByPerseoId);
  }

  /**
   * Gli istituti in epas non sincronizzare.
   */
  public static void oldInstitutes() {

    List<Institute> institutes = officeDao.institutes(Optional.<String>absent(),
        Optional.<String>absent(), Optional.absent(),
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
        for (Office office : institute.seats) {
          perseoOfficeByCodeId.put(office.codeId, office);
        }
      }
    }

    render(institutes, perseoInstitutesByCds, perseoOfficeByCodeId);
  }


  /**
   * Lega l'istituto epas al perseoId. Da utilizzare manualmente con cautela!!!
   *
   * @param epasInstituteId id Istituto
   * @param perseoId        Perseo id dell'istituto.
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
      institute.get().perseoId = instituteInPerseo.get().perseoId;
      institute.get().cds = instituteInPerseo.get().cds;
      institute.get().code = instituteInPerseo.get().code;
      institute.get().name = instituteInPerseo.get().name;
      institute.get().save();

      log.info("Associato istituto={} al perseoId={}", institute.get().toString(), perseoId);
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
      Office perseoOffice = instituteWithThatSeat.get().seats.iterator().next();

      //copy ( TODO: update method)
      office.perseoId = perseoOffice.perseoId;
      office.code = perseoOffice.code;
      office.name = perseoOffice.name;
      office.address = perseoOffice.address;

      office.save();

      log.info("Associata sede={} al perseoId={}", office.toString(), perseoId);
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
    Office seat = instituteWithThatSeat.get().seats.iterator().next();

    // Salvataggio istituto
    Optional<Institute> institute = officeDao.byCds(instituteWithThatSeat.get().cds);
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
      institute.get().seats.add(seat);
      seat.institute = institute.get();
    }

    // TODO: spostare in un creator epas che venga utilizzato anche nelle crud
    // (finchè non spariranno).
    seat.beginDate = new LocalDate(LocalDate.now().getYear() - 1, 12, 31);
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
   * @param officeId sede
   */
  public static void people(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    if (office.perseoId == null) {
      flash.error("Selezionare una sede già sincronizzata... "
          + "%s non lo è ancora.", office.toString());
      institutes();
    }

    Map<Long, Person> perseoPeople = peoplePerseoConsumer
        .perseoPeopleByPerseoId(Optional.of(office.perseoId));

    Map<Long, Person> epasSynchronizedPeople = personDao.mapSynchronized(Optional.of(office));

    Map<Long, List<Contract>> perseoPeopleContract = contractPerseoConsumer
        .perseoPeopleContractsMap(Optional.of(office));

    Map<Long, IWrapperPerson> epasWrapperedPeople = Maps.newHashMap();
    for (IWrapperPerson person : FluentIterable.from(personDao
        .list(Optional.of(office)).list()).transform(wrapperFunctionFactory.person()).toList()) {
      epasWrapperedPeople.put(person.getValue().id, person);
    }


    // Tutti i ruoli epas formato Map<perseoPersonId, Set<String>> Contenente tutti gli 
    // i ruoli (amministrativi) che ogni persona dell'office ha (anche in altri office).

    Map<Long, Set<String>> epasPeopleUros = usersRolesOfficesDao
        .getEpasRoles(Optional.fromNullable(office));

    Map<Long, Set<String>> perseoPeopleUros = rolePerseoCunsomer.perseoRoles(Optional.of(office));

    render(office, perseoPeople, epasSynchronizedPeople, perseoPeopleContract, epasWrapperedPeople,
        epasPeopleUros, perseoPeopleUros);

  }

  /**
   * Le persone in epas non sincronizzare.
   */
  public static void oldPeople(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    if (office.perseoId == null) {
      flash.error("Per sincronizzare le persone occorre che la sede sia anch'essa sincronizzata");
      oldInstitutes();
    }

    List<Person> people = personDao.listFetched(Optional.<String>absent(), Sets.newHashSet(office),
        false, null, null, false).list();

    List<IWrapperPerson> wrapperedPeople = FluentIterable.from(people)
        .transform(wrapperFunctionFactory.person()).toList();

    Map<String, Person> perseoPeopleByNumber = null;
    try {
      perseoPeopleByNumber = 
          peoplePerseoConsumer.perseoPeopleByNumber(Optional.of(office.perseoId));
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    render(wrapperedPeople, perseoPeopleByNumber, office);
  }

  /**
   * Esegue il join per una persona specifica.
   *
   * @param epasPersonId id della Persona
   * @param perseoId     Perseo id da legare alla persona specificata.
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

    oldPeople(person.office.id);
  }

  // TODO: spostare nell'updater?

  /**
   * @param epasPerson   Person presente su ePas
   * @param perseoPerson Person prelevata da Perseo.
   */
  private static void join(Person epasPerson, Person perseoPerson) {
    //copy ( TODO: update method)

    if (perseoPerson.qualification == null) {
      log.info("Impossibile associare la persona {} con qualifica nulla perseoId={}",
          epasPerson.toString(), epasPerson.perseoId);
      return;
    }

    epasPerson.name = perseoPerson.name;
    epasPerson.surname = perseoPerson.surname;
    epasPerson.number = perseoPerson.number;
    // per adesso le email non combaciano @iit.cnr.it vs @cnr.it
    epasPerson.email = perseoPerson.email; 
    epasPerson.eppn = perseoPerson.email;
    epasPerson.qualification = perseoPerson.qualification;
    epasPerson.perseoId = perseoPerson.perseoId;
    epasPerson.save();
    log.info("Associata persona={} al perseoId={}", epasPerson.toString(), epasPerson.perseoId);
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

    List<Person> people = personDao.listFetched(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();

    Map<String, Person> perseoPeopleByNumber = null;
    try {
      perseoPeopleByNumber = peoplePerseoConsumer
          .perseoPeopleByNumber(Optional.fromNullable(office.perseoId));
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    if (!people.isEmpty() && perseoPeopleByNumber != null) {
      int synced = 0;
      for (Person person : people) {
        if (person.perseoId == null) {
          Person perseoPerson = perseoPeopleByNumber.get(person.number);
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
      office = officeDao.byPerseoId(personInPerseo.get().perseoOfficeId);
      if (!office.isPresent()) {
        flash.error("L'ufficio di appartenenza della persona selezionata non è "
            + "ancora sincronizzato con Perseo");
        people(null);
      }

      personInPerseo.get().office = office.get();
      personInPerseo.get().beginDate = 
          LocalDate.now().withDayOfMonth(1).withMonthOfYear(1).minusDays(1);

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
      if (!personCreator(personInPerseo.get()).isPresent()) {
        flash.error("La persona selezionata non può essere importata a causa di errori.");
      } else {
        flash.success("La persona %s è stata importata con successo da Perseo!",
            personInPerseo.get().toString());
      }
    }

    people(office.get().id);
  }

  /**
   * Da spostare in un updater.
   */
  private static Optional<Person> personCreator(Person person) {

    try {
      person.user = userManager.createUser(person);
      person.save();

      Role employee = Role.find("byName", Role.EMPLOYEE).first();
      officeManager.setUro(person.user, person.office, employee);
      person.save();
    } catch (Exception ex) {
      return Optional.<Person>absent();
    }

    return Optional.fromNullable(person);
  }

  /**
   * Importa tutte le persone di perseo non ancora presenti in epas.
   */
  private static boolean managerImportAllPersonInOffice(Office office) {

    Map<Long, Person> perseoPeopleByPerseoId = null;
    try {
      perseoPeopleByPerseoId = peoplePerseoConsumer
          .perseoPeopleByPerseoId(Optional.fromNullable(office.perseoId));
    } catch (ApiRequestException ex) {
      ex.printStackTrace();
    }

    List<Person> people = personDao.listFetched(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();

    // questa operazione si può fare solo se non ci sono persone in epas
    // attualmente non associate a perseo.

    Map<Long, Person> epasPeopleByPerseoId = Maps.newHashMap();
    for (Person person : people) {
      if (person.perseoId != null) {
        epasPeopleByPerseoId.put(person.perseoId, person);
      } else {
        return false;
      }
    }

    for (Person perseoPerson : perseoPeopleByPerseoId.values()) {
      if (epasPeopleByPerseoId.get(perseoPerson.perseoId) == null) {

        log.info("Provo name:{} matricola:{} qualifica:{} perseoId:{}", 
            perseoPerson.fullName(), perseoPerson.number,
            perseoPerson.qualification, perseoPerson.perseoId);

        // join dell'office (in automatico ancora non c'è...)
        perseoPerson.office = office;
        perseoPerson.beginDate = 
            LocalDate.now().withDayOfMonth(1).withMonthOfYear(1).minusDays(1);
        validation.valid(perseoPerson);
        if (Validation.hasErrors()) {
          // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
          log.info("L'importazione della persone con perseoId={} ha comportato errori di "
              + "validazione nella persona. errors={}.", 
              perseoPerson.perseoId, validation.errorsMap());
          Validation.clear();
          continue;
        }

        // Creazione!
        if (!personCreator(perseoPerson).isPresent()) {
          // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
          log.info("L'importazione della persone con perseoId={} ha comportato errori di "
              + "validazione nella persona. errors={}.", 
              perseoPerson.perseoId, validation.errorsMap());
          Validation.clear();
          continue;
        }
      }
    }
    return true;

  }

  /**
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

    if (office.perseoId == null) {
      flash.error("Selezionare una sede già sincronizzata... "
          + "%s non lo è ancora.", office.toString());
    } else {
      //Costruisco la mappa di tutti i contratti attivi perseo per le persone sincronizzate epas.
      try {
        perseoDepartmentActiveContractsByPersonPerseoId = contractPerseoConsumer
            .perseoDepartmentActiveContractsByPersonPerseoId(office.perseoId, office);
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
   * @param perseoId       id del Contratto su Perseo.
   */
  public static void joinContract(Long epasContractId, Long perseoId) {

    Contract contract = contractDao.getContractById(epasContractId);
    Verify.verifyNotNull(contract);
    Verify.verifyNotNull(perseoId);

    Optional<Contract> contractInPerseo = Optional.absent();
    try {
      contractInPerseo = contractPerseoConsumer
          .perseoContractPerseoId(perseoId, contract.person);
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }
    if (contractInPerseo.isPresent()) {
      joinUpdateContract(contract, contractInPerseo.get());
      flash.success("Operazione effettuata correttamente");
    }

    oldActiveContracts(contract.person.office.id);
  }

  /**
   * Associa tutti i contratti attivi.
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

    if (office.perseoId == null) {
      flash.error("Selezionare una sede già sincronizzata... "
          + "%s non lo è ancora.", office.toString());
    } else {
      //Costruisco la mappa di tutti i contratti attivi perseo per le persone sincronizzate epas.
      try {
        perseoDepartmentActiveContractsByPersonPerseoId = contractPerseoConsumer
            .perseoDepartmentActiveContractsByPersonPerseoId(office.perseoId, office);
      } catch (ApiRequestException ex) {
        flash.error("%s", ex);
      }
    }

    for (Contract epasContract : activeContractsEpasByPersonPerseoId.values()) {
      if (epasContract.perseoId == null) {
        Contract perseoContract =
            perseoDepartmentActiveContractsByPersonPerseoId.get(epasContract.person.perseoId);
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
   * @param epasContract   contratto presente su ePas
   * @param perseoContract contratto Prelevato da Perseo.
   */
  private static void joinUpdateContract(Contract epasContract, Contract perseoContract) {
    //copy ( TODO: update method)

    epasContract.beginDate = perseoContract.beginDate;
    if (perseoContract.isTemporaryMissing && perseoContract.endDate == null) {
      // TODO caso particolare
    } else {
      epasContract.endDate = perseoContract.endDate;
    }
    epasContract.endContract = perseoContract.endContract;
    epasContract.isTemporaryMissing = perseoContract.isTemporaryMissing;
    epasContract.perseoId = perseoContract.perseoId;

    // TODO: update periods e ricalcoli!!!

    epasContract.save();

    log.info("Associata contratto={} al perseoId={}",
        epasContract.toString(), epasContract.perseoId);
  }

  /**
   * Posso importare un contratto da perseo... purchè la sua persona sia sincronizzata e non
   * conflitti con le date dei contratti epas.
   */
  public static void importContract(Long perseoId, Long epasPersonId) {

    Person person = personDao.getPersonById(epasPersonId);
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.perseoId);

    Optional<Contract> contractInPerseo = Optional.absent();
    try {
      contractInPerseo = contractPerseoConsumer.perseoContractPerseoId(perseoId, person);
    } catch (ApiRequestException ex) {
      flash.error("%s", ex);
    }

    if (contractInPerseo.isPresent()) {
      // Salvare il contratto.
      if (!contractManager.properContractCreate(contractInPerseo.get(), Optional.absent(), false)) {
        flash.error("Il contratto non può essere importato a causa di errori");
        people(person.office.id);
      }
      flash.success("Contratto di %s importato con successo da Perseo!", person.toString());
    }

    people(person.office.id);
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
          .perseoDepartmentActiveContractsByPersonPerseoId(office.perseoId, office);
    } catch (ApiRequestException ex) {
      return Optional.of(ex);
    }

    if (perseoDepartmentActiveContractsByPersonPerseoId != null) {
      for (Contract perseoContract : perseoDepartmentActiveContractsByPersonPerseoId.values()) {
        Contract epasContract = 
            activeContractsEpasByPersonPerseoId.get(perseoContract.person.perseoId);
        if (epasContract != null) {
          continue;
        }
        // Salvare il contratto.
        if (!contractManager.properContractCreate(perseoContract, Optional.absent(), false)) {
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
    institute.perseoId = null;
    institute.save();
    for (Office office : institute.seats) {

      office.perseoId = null;
      office.save();
      for (Person person : office.persons) {

        person.perseoId = null;
        person.save();
        for (Contract contract : person.contracts) {
          contract.perseoId = null;
          contract.save();
        }
      }
    }

    flash.success("Istituto %s desincronizzato.", institute.name);
    oldInstitutes();
  }
  
}
