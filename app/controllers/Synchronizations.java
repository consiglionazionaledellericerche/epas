package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import jobs.FixUserPermission;

import lombok.extern.slf4j.Slf4j;

import manager.ContractManager;
import manager.OfficeManager;
import manager.PeriodManager;
import manager.UserManager;

import models.Contract;
import models.Institute;
import models.Office;
import models.Person;
import models.Role;
import models.WorkingTimeType;

import org.assertj.core.util.Maps;
import org.assertj.core.util.Sets;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.mvc.Controller;
import play.mvc.With;
import synch.perseoconsumers.contracts.ContractPerseoConsumer;
import synch.perseoconsumers.office.OfficePerseoConsumer;
import synch.perseoconsumers.people.PeoplePerseoConsumer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

@Slf4j
@With({Resecure.class, RequestInit.class})
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
  static PeriodManager periodManager;
  @Inject
  static UserManager userManager;
  @Inject
  static OfficeManager officeManager;
  @Inject
  static ContractManager contractManager;
  @Inject
  static WorkingTimeTypeDao workingTimeTypeDao;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;

  /**
   * Gli istituti in epas da sincronizzare.
   */
  public static void institutes(String name) {

    List<Institute> institutes = officeDao.institutes(Optional.<String>fromNullable(name),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECNICAL_ADMIN)).list();

    Map<String, Institute> perseoInstitutesByCds = officePerseoConsumer.perseoInstitutesByCds();

    Map<String, Office> perseoOfficeByCodeId = Maps.newHashMap();
    for (Institute institute : perseoInstitutesByCds.values()) {
      for (Office office : institute.seats) {
        perseoOfficeByCodeId.put(office.codeId, office);
      }
    }

    render(institutes, perseoInstitutesByCds, perseoOfficeByCodeId);
  }

  /**
   * Gli istituti non in epas, si possono importare.
   */
  public static void otherInstitutes() {

    List<Institute> perseoInstitutes = officePerseoConsumer.perseoInstitutes();

    List<Institute> institutes = officeDao.institutes(Optional.<String>absent(),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECNICAL_ADMIN)).list();

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
   * Lega l'istituto epas al perseoId. Da utilizzare manualmente con cautela!!!
   *
   * @param epasInstituteId id Istituto
   * @param perseoId        Perseo id dell'istituto.
   */
  public static void joinInstitute(Long epasInstituteId, Long perseoId) {
    Optional<Institute> institute = officeDao.instituteById(epasInstituteId);
    Verify.verify(institute.isPresent());
    Verify.verifyNotNull(perseoId);

    Optional<Institute> instituteInPerseo = officePerseoConsumer
        .perseoInstituteByInstitutePerseoId(perseoId);
    Verify.verify(instituteInPerseo.isPresent());

    //copy ( TODO: update method)
    institute.get().perseoId = instituteInPerseo.get().perseoId;
    institute.get().cds = instituteInPerseo.get().cds;
    institute.get().code = instituteInPerseo.get().code;
    institute.get().name = instituteInPerseo.get().name;
    institute.get().save();

    log.info("Associato istituto={} al perseoId={}", institute.get().toString(), perseoId);
    flash.success("Operazione effettuata correttamente");
    institutes(null);
  }

  /**
   * Lega la sede epas al perseoId. Da utilizzare manualmente con cautela!!!
   */
  public static void joinOffice(Long epasOfficeId, Long perseoId) {
    Office office = officeDao.getOfficeById(epasOfficeId);
    Verify.verifyNotNull(office);
    Verify.verifyNotNull(perseoId);

    Optional<Institute> instituteWithThatSeat =
        officePerseoConsumer.perseoInstituteByOfficePerseoId(perseoId);
    Verify.verify(instituteWithThatSeat.isPresent());

    Office perseoOffice = instituteWithThatSeat.get().seats.iterator().next();

    //copy ( TODO: update method)
    office.perseoId = perseoOffice.perseoId;
    office.code = perseoOffice.code;
    office.name = perseoOffice.name;
    office.address = perseoOffice.address;

    office.save();

    log.info("Associata sede={} al perseoId={}", office.toString(), perseoId);
    flash.success("Operazione effettuata correttamente");
    institutes(null);
  }

  /**
   * Importa la sede da perseo, (e l'istituto se non è già presente).
   *
   * @param seatPerseoId Perseo ID della sede.
   */
  public static void importOffice(Long seatPerseoId) {

    //Prendere da perseo quella sede.
    Optional<Institute> instituteWithThatSeat =
        officePerseoConsumer.perseoInstituteByOfficePerseoId(seatPerseoId);
    if (!instituteWithThatSeat.isPresent()) {
      flash.error("Niente da fare :(((.");
      otherInstitutes();
    }
    Office seat = instituteWithThatSeat.get().seats.iterator().next();

    // Salvataggio istituto
    Optional<Institute> institute = officeDao.byCds(instituteWithThatSeat.get().cds);
    if (!institute.isPresent()) {

      //Istituto non presente

      validation.valid(instituteWithThatSeat);
      if (validation.hasErrors()) {
        // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
        log.info("L'importazione della sede con perseoId={} ha comportato errori di validazione "
            + "nel suo istituto. errors={}.", seatPerseoId, validation.errors());
        flash.error("La sede selezionata non può essere importata a causa di errori.");
        otherInstitutes();
      }

      instituteWithThatSeat.get().save();
      institute = instituteWithThatSeat;
    } else {

      //Istituto già presente, aggiungo la nuova sede alla sua lista e sistemo la relazione.
      institute.get().seats.add(seat);
      seat.institute = institute.get();
    }

    //Salvataggio sede
    validation.valid(seat);
    if (validation.hasErrors()) {
      // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
      log.info("L'importazione della sede con perseoId={} ha comportato errori di validazione "
          + "nella sede. errors={}.", seatPerseoId, validation.errors());
      flash.error("La sede selezionata non può essere importata a causa di errori.");
      otherInstitutes();
    }

    // TODO: spostare in un creator epas che venga utilizzato anche nelle crud
    // (finchè non spariranno).
    seat.beginDate = new LocalDate(LocalDate.now().getYear() - 1, 12, 31);
    periodManager.updatePropertiesInPeriodOwner(seat);
    seat.save();
    // Per i permessi di developer e admin...
    FixUserPermission.doJob();

    flash.success("La sede %s è stata importata con successo da Perseo!", seat.toString());

    otherInstitutes();
  }


  /**
   * Le persone in epas da sincronizzare.
   */
  public static void people(Long officeId) {

    Office office;
    if (officeId != null) {
      office = officeDao.getOfficeById(officeId);
      notFoundIfNull(office);
    } else {
      office = officeDao.allOffices().list().get(0);
    }
    Set<Office> offices = Sets.newHashSet();
    offices.add(office);

    @SuppressWarnings("deprecation")
    List<Person> people = personDao
        .listFetched(Optional.<String>absent(), offices, false, null, null, false)
        .list();

    List<IWrapperPerson> wrapperedPeople = FluentIterable.from(people)
        .transform(wrapperFunctionFactory.person()).toList();

    Map<Integer, Person> perseoPeopleByNumber = peoplePerseoConsumer.perseoPeopleByNumber();

    render(wrapperedPeople, perseoPeopleByNumber, office);
  }

  /**
   * Le persone non in epas si possono importare.
   */
  public static void otherPeople(Long officeId) {

    Office office;
    if (officeId != null) {
      office = officeDao.getOfficeById(officeId);
      notFoundIfNull(office);
    } else {
      office = officeDao.allOffices().list().get(0);
    }
    if (office.perseoId == null) {
      Map<Long, Person> perseoPeopleByPerseoId = Maps.newHashMap();
      Map<Long, Person> epasPeopleByPerseoId = Maps.newHashMap();
      flash.error("Selezionare una sede già sincronizzata... "
          + "%s non lo è ancora.", office.toString());
      render(perseoPeopleByPerseoId, epasPeopleByPerseoId);
    }

    Set<Office> offices = Sets.newHashSet();
    offices.add(office);

    Map<Long, Person> perseoPeopleByPerseoId = peoplePerseoConsumer
        .perseoDepartmentPeopleByPerseoId(office.perseoId);
    @SuppressWarnings("deprecation")
    List<Person> people = personDao
        .listFetched(Optional.<String>absent(), offices, false, null, null, false)
        .list();
    Map<Long, Person> epasPeopleByPerseoId = Maps.newHashMap();
    for (Person person : people) {
      if (person.perseoId != null) {
        epasPeopleByPerseoId.put(person.perseoId, person);
      }
    }

    render(perseoPeopleByPerseoId, epasPeopleByPerseoId, office);
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

    Optional<Person> personInPerseo = peoplePerseoConsumer.perseoPersonByPerseoId(perseoId);
    Verify.verify(personInPerseo.isPresent());

    join(person, personInPerseo.get());

    flash.success("Operazione effettuata correttamente");
    people(person.office.id);
  }

  // TODO: spostare nell'updater?

  /**
   * @param epasPerson   Person presente su ePas
   * @param perseoPerson Person prelevata da Perseo.
   */
  private static void join(Person epasPerson, Person perseoPerson) {
    //copy ( TODO: update method)
    epasPerson.name = perseoPerson.name;
    epasPerson.surname = perseoPerson.surname;
    epasPerson.number = perseoPerson.number;
    //epasPerson.email = perseoPerson.email; per adesso le email non combaciano @iit.cnr.it vs @cnr.it
    //epasPerson.eppn = perseoPerson.email;
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

    @SuppressWarnings("deprecation")
    List<Person> people = personDao.listFetched(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();

    Map<Integer, Person> perseoPeopleByNumber = peoplePerseoConsumer.perseoPeopleByNumber();

    for (Person person : people) {
      if (person.perseoId == null) {
        Person perseoPerson = perseoPeopleByNumber.get(person.number);
        if (perseoPerson != null) {
          join(person, perseoPerson);
        }
      }
    }

    flash.success("Operazione effettuata correttamente");
    people(office.id);

  }

  /**
   * Posso importare una persona da perseo.. purchè non esista già una persona con quella
   * matricola.
   */
  public static void importPerson(Long perseoId) {

    //Prendere da perseo quella persona.
    Optional<Person> personInPerseo = peoplePerseoConsumer.perseoPersonByPerseoId(perseoId);
    Verify.verify(personInPerseo.isPresent());

    //Caricare dalla persona l'office.
    Optional<Office> office = officeDao.byPerseoId(personInPerseo.get().perseoOfficeId);
    Verify.verify(office.isPresent());

    personInPerseo.get().office = office.get();
    validation.valid(personInPerseo.get());
    if (validation.hasErrors()) {
      // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
      log.info("L'importazione della persone con perseoId={} ha comportato errori di validazione "
          + "nella persona. errors={}.", perseoId, validation.errors());
      flash.error("La persona selezionata non può essere importata a causa di errori.");
      otherPeople(office.get().id);
    }

    // Creazione!
    if (!personCreator(personInPerseo.get()).isPresent()) {
      flash.error("La persona selezionata non può essere importata a causa di errori.");
    } else {
      flash.success("La persona %s è stata importata con successo da Perseo!",
          personInPerseo.get().toString());
    }

    otherPeople(office.get().id);

  }

  /**
   * Da spostare in un updater.
   */
  private static Optional<Person> personCreator(Person person) {

    try {
      // FIXME: patch da sistemare quando si creeranno i periodi
      person.createdAt = LocalDateTime.now().withDayOfMonth(1).withMonthOfYear(1).minusDays(1);
      person.user = userManager.createUser(person);
      person.save();

      Role employee = Role.find("byName", Role.EMPLOYEE).first();
      officeManager.setUro(person.user, person.office, employee);
      person.save();
    } catch (Exception e) {
      return Optional.<Person>absent();
    }

    return Optional.fromNullable(person);
  }


  /**
   * @param epasOfficeId id dell'ufficio.
   */
  public static void importAllPersonInOffice(Long epasOfficeId) {

    Office office = officeDao.getOfficeById(epasOfficeId);
    notFoundIfNull(office);

    Map<Long, Person> perseoPeopleByPerseoId = peoplePerseoConsumer.perseoDepartmentPeopleByPerseoId(office.perseoId);

    @SuppressWarnings("deprecation")
    List<Person> people = personDao.listFetched(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();

    // questa operazione si può fare solo se non ci sono persone in epas
    // attualmente non associate a perseo.

    Map<Long, Person> epasPeopleByPerseoId = Maps.newHashMap();
    for (Person person : people) {
      if (person.perseoId != null) {
        epasPeopleByPerseoId.put(person.perseoId, person);
      } else {
        flash.error("Per fare questa operazione tutte le persone già esistenti della sede "
            + "devono essere correttamente sincronizzate. "
            + "Esempio %s non lo è. Operazione annullata.", person.toString());
        otherPeople(office.id);
      }
    }

    for (Person perseoPerson : perseoPeopleByPerseoId.values()) {
      if (epasPeopleByPerseoId.get(perseoPerson.perseoId) == null) {

        log.info("Provo name:{} matricola:{} qualifica:{} perseoId:{}", perseoPerson.fullName(), perseoPerson.number,
            perseoPerson.qualification, perseoPerson.perseoId);

        // join dell'office (in automatico ancora non c'è...)
        perseoPerson.office = office;

        validation.valid(perseoPerson);
        if (validation.hasErrors()) {
          // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
          log.info("L'importazione della persone con perseoId={} ha comportato errori di validazione "
              + "nella persona. errors={}.", perseoPerson.perseoId, validation.errorsMap());
          validation.clear();
          continue;
        }

        // Creazione!
        if (!personCreator(perseoPerson).isPresent()) {
          // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
          log.info("L'importazione della persone con perseoId={} ha comportato errori di validazione "
              + "nella persona. errors={}.", perseoPerson.perseoId, validation.errorsMap());
          validation.clear();
          continue;
        }
      }
    }

    flash.success("Operazione effettuata correttamente");
    otherPeople(office.id);
  }

  /**
   * I contratti attivi in epas da sincronizzare.
   */
  public static void activeContracts(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    //La mappa di tutti i contratti attivi delle persone sincronizzate epas.
    Map<Long, Contract> activeContractsEpasByPersonPerseoId =
        contractPerseoConsumer.activeContractsEpasByPersonPerseoId(office);

    //Costruisco la mappa di tutti i contratti attivi perseo per le persone sincronizzate epas.
    Map<Long, Contract> perseoDepartmentActiveContractsByPersonPerseoId = contractPerseoConsumer
        .perseoDepartmentActiveContractsByPersonPerseoId(office.perseoId, office);

    render(activeContractsEpasByPersonPerseoId, perseoDepartmentActiveContractsByPersonPerseoId, office);
  }


  /**
   * Le persone non in epas si possono importare.
   */
  public static void otherContracts(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    //La mappa di tutti i contratti attivi delle persone sincronizzate epas.
    Map<Long, Contract> activeContractsEpasByPersonPerseoId =
        contractPerseoConsumer.activeContractsEpasByPersonPerseoId(office);

    //Costruisco la mappa di tutti i contratti attivi perseo per le persone sincronizzate epas.
    Map<Long, Contract> perseoDepartmentActiveContractsByPersonPerseoId = contractPerseoConsumer
        .perseoDepartmentActiveContractsByPersonPerseoId(office.perseoId, office);

    render(activeContractsEpasByPersonPerseoId, perseoDepartmentActiveContractsByPersonPerseoId, office);

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

    Optional<Contract> contractInPerseo = contractPerseoConsumer
        .perseoContractByPerseoId(perseoId, contract.person);
    Verify.verify(contractInPerseo.isPresent());

    joinUpdateContract(contract, contractInPerseo.get());

    flash.success("Operazione effettuata correttamente");
    activeContracts(contract.person.office.id);
  }


  // TODO: spostare nell'updater?

  /**
   * @param epasContract   contratto presente su ePas
   * @param perseoContract contratto Prelevato da Perseo.
   */
  private static void joinUpdateContract(Contract epasContract, Contract perseoContract) {
    //copy ( TODO: update method)

    epasContract.beginDate = perseoContract.beginDate;
    if (perseoContract.isTemporary && perseoContract.endDate == null) {
      // TODO caso particolare
    } else {
      epasContract.endDate = perseoContract.endDate;
    }
    epasContract.endContract = perseoContract.endContract;
    epasContract.isTemporary = perseoContract.isTemporary;
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
  @SuppressWarnings("deprecation")
  public static void importContract(Long perseoId, Long epasPersonId) {

    Person person = personDao.getPersonById(epasPersonId);
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.perseoId);

    Optional<Contract> contractInPerseo = contractPerseoConsumer.perseoContractByPerseoId(perseoId, person);
    Verify.verify(contractInPerseo.isPresent());

    WorkingTimeType normal = workingTimeTypeDao.getWorkingTimeTypeByDescription("Normale");

    // Salvare il contratto.
    if (!contractManager.properContractCreate(contractInPerseo.get(), normal, false)) {
      flash.error("Il contratto non può essere importato a causa di errori");
      otherContracts(person.office.id);
    }
    flash.success("Contratto di %s importato con successo da Perseo!", person.toString());

    otherContracts(person.office.id);
  }

  /**
   * Posso importare un contratto da perseo... purchè la sua persona sia sincronizzata e non
   * conflitti con le date dei contratti epas.
   */
  @SuppressWarnings("deprecation")
  public static void importAllContractsInOffice(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    //La mappa di tutti i contratti attivi delle persone sincronizzate epas.
    Map<Long, Contract> activeContractsEpasByPersonPerseoId =
        contractPerseoConsumer.activeContractsEpasByPersonPerseoId(office);

    //Costruisco la mappa di tutti i contratti attivi perseo per le persone sincronizzate epas.
    Map<Long, Contract> perseoDepartmentActiveContractsByPersonPerseoId = contractPerseoConsumer
        .perseoDepartmentActiveContractsByPersonPerseoId(office.perseoId, office);

    WorkingTimeType normal = workingTimeTypeDao.getWorkingTimeTypeByDescription("Normale");

    for (Contract perseoContract : perseoDepartmentActiveContractsByPersonPerseoId.values()) {
      Contract epasContract = activeContractsEpasByPersonPerseoId.get(perseoContract.person.perseoId);
      if (epasContract != null) {
        continue;
      }

      // Salvare il contratto.
      if (!contractManager.properContractCreate(perseoContract, normal, false)) {
        // segnalare il conflitto
        continue;
      }
    }

    flash.success("Operazione effettuata correttamente");

    otherContracts(office.id);
  }
}
