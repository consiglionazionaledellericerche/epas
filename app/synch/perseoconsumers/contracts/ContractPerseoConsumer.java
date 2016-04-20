package synch.perseoconsumers.contracts;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Maps;

import dao.PersonDao;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import lombok.extern.slf4j.Slf4j;

import models.Contract;
import models.Office;
import models.Person;

import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.joda.time.LocalDate;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import synch.perseoconsumers.PerseoApis;

import java.util.List;
import java.util.Map;

@Slf4j
public class ContractPerseoConsumer {

  private final PersonDao personDao;
  private final WrapperModelFunctionFactory wrapperFunctionFactory;

  @Inject
  public ContractPerseoConsumer(PersonDao personDao, WrapperModelFunctionFactory wrapperFunctionFactory) {
    this.personDao = personDao;
    this.wrapperFunctionFactory = wrapperFunctionFactory;

  }

  /**
   * Json relativo alla contracta di perseo con id perseoId nel formato utile a epas.
   *
   * @param perseoId id perseo contracta
   * @return json
   */
  private Optional<String> perseoContractJson(Long perseoId) {

    String endPoint = PerseoApis.getContractForEpasEndpoint() + perseoId;
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo il contract da {}.", endPoint);

    if (!restResponse.success()) {
      log.error("Impossibile prelevare il contract da {}", endPoint);
      return Optional.<String>absent();
    }

    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch (Exception exp) {
      log.info("Url={} non json.", endPoint);
    }

    return null;
  }

  /**
   * Json relativo a tutti i contratti del department di perseo nel formato utile a epas.
   *
   * @return json
   */
  private Optional<String> perseoContractsJson(Long perseoId) {

    String endPoint = PerseoApis.getAllDepartmentContractsForEpasEndpoint() + perseoId;
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo i contratti del department da {}.", endPoint);

    if (!restResponse.success()) {
      log.error("Impossibile prelevare i contratti del department da {}", endPoint);
      return Optional.<String>absent();
    }

    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch (Exception exp) {
      log.info("Url={} non json.", endPoint);
    }

    return null;
  }

  /**
   * La lista dei PerseoContract nel perseoDepartment.
   *
   * @param perseoId perseo id department.
   * @return lista
   */
  private List<PerseoContract> getAllPerseoDepartmentContracts(Long perseoId) {

    //Json della richiesta
    Optional<String> json = perseoContractsJson(perseoId);
    if (json == null || !json.isPresent()) {
      return null;
    }

    List<PerseoContract> perseoContracts = null;
    try {
      perseoContracts = new Gson()
          .fromJson(json.get(), new TypeToken<List<PerseoContract>>() {
          }.getType());
    } catch (Exception exp) {
      log.info("Impossibile caricare da perseo i contratti del department.");
      return Lists.newArrayList();
    }

    return perseoContracts;
  }


  /**
   * La PerseoContract con quel perseoId in Perseo.
   *
   * @param perseoId perseo id contract.
   * @return contract
   */
  private Optional<PerseoContract> getPerseoContractByPerseoId(Long perseoId) {

    //Json della richiesta
    Optional<String> json = perseoContractJson(perseoId);
    if (json == null || !json.isPresent()) {
      return null;
    }

    PerseoContract perseoContract = null;
    try {
      perseoContract = new Gson()
          .fromJson(json.get(), new TypeToken<PerseoContract>() {
          }.getType());
    } catch (Exception exp) {
      log.info("Impossibile caricare da perseo il contratto con perseoId={}.", perseoId);
      return Optional.<PerseoContract>absent();
    }
    if (perseoContract == null) {
      return Optional.<PerseoContract>absent();
    }
    return Optional.fromNullable(perseoContract);

  }

  /**
   * Conversione a oggetti epas. PerseoContract.
   *
   * @param perseoContract da convertire
   * @return contract
   */
  private Contract epasConverter(PerseoContract perseoContract, Person person) {

    Contract contract = new Contract();
    contract.beginDate = perseoContract.beginContract != null ? new LocalDate(perseoContract.beginContract) : null;
    contract.endDate = perseoContract.expireContract != null ? new LocalDate(perseoContract.expireContract) : null;
    contract.endContract = perseoContract.endContract != null ? new LocalDate(perseoContract.endContract) : null;
    contract.isTemporary = perseoContract.temporary;
    contract.perseoId = perseoContract.id;
    contract.person = person;

    return contract;
  }

  /**
   * Conversione di una lista di oggetti epas. PerseoContract
   *
   * @param perseoContracts                        lista di perseoContract
   * @param departmentSyncrhonizedPeopleByPerseoId mappa perseoId -> person
   * @return contratti epas
   */
  private List<Contract> epasConverter(List<PerseoContract> perseoContracts,
                                       Map<Long, Person> departmentSyncrhonizedPeopleByPerseoId) {
    List<Contract> contracts = Lists.newArrayList();
    for (PerseoContract perseoContract : perseoContracts) {

      Person person = departmentSyncrhonizedPeopleByPerseoId.get(perseoContract.personId);
      if (person == null) {
        log.info("I contratti perseo di persone non sincronizzate perseo vengono scartati.");
      } else {
        Contract contract = epasConverter(perseoContract, person);
        contracts.add(contract);
      }
    }
    return contracts;
  }

//  /**
//   * Tutti i contratti attivi del department con quel perseoId.<br>
//   * I contratti di persone non sincronizzate vengono scartati.<br>
//   * Formato mappa: perseoId -> contract
//   * @param perseoDepartmentId department perseo id
//   * @param departmentSynchronizedPeople  mappa personeEpas per perseoId
//   * @return mappa
//   */
//  public Map<Long, Contract> perseoDepartmentActiveContractsByPerseoId(Long perseoDepartmentId, 
//      Map<Long, Contract> departmentSynchronizedPeople) {
//    
//    List<PerseoContract> perseoContracts = getAllPerseoDepartmentContracts(perseoDepartmentId);
//    Map<Long, Contract> perseoContractsMap = Maps.newHashMap();
//    for (Contract contract : epasConverter(perseoContracts, departmentSynchronizedPeople)) {
//      perseoContractsMap.put(contract.perseoId, contract);
//    }
//   
//    return perseoContractsMap;
//  }

  /**
   * Serve per sincronizzare i contratti attivi già presenti in epas non sincronizzati.
   *
   * Tutti i contratti attivi del department con quel perseoId.<br> I contratti di persone non
   * sincronizzate vengono scartati.<br> Formato mappa: person.perseoId -> contract
   *
   * @param perseoDepartmentId department perseo id
   * @param office             ?
   * @return mappa
   */
  public Map<Long, Contract> perseoDepartmentActiveContractsByPersonPerseoId(Long perseoDepartmentId,
                                                                             Office office) {

    @SuppressWarnings("deprecation")
    List<Person> people = personDao.listFetched(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();

    Map<Long, Person> departmentSyncrhonizedPeopleByPerseoId = epasSynchronizedPersonByPersonId(people);
    List<PerseoContract> perseoContracts = getAllPerseoDepartmentContracts(perseoDepartmentId);
    Map<Long, Contract> perseoContractsMap = Maps.newHashMap();
    for (Contract contract : epasConverter(perseoContracts, departmentSyncrhonizedPeopleByPerseoId)) {
      perseoContractsMap.put(contract.person.perseoId, contract);
    }

    return perseoContractsMap;
  }

  /**
   * Il contratto con quel perseoId.
   *
   * @return contratto
   */
  public Optional<Contract> perseoContractByPerseoId(Long contractPerseoId, Person person) {
    Optional<PerseoContract> perseoContract = getPerseoContractByPerseoId(contractPerseoId);
    if (!perseoContract.isPresent()) {
      return Optional.<Contract>absent();
    }
    return Optional.fromNullable(epasConverter(perseoContract.get(), person));
  }

  /**
   *
   * @return
   */
  public Map<Long, Person> epasSynchronizedPersonByPersonId(List<Person> people) {
    Map<Long, Person> maps = Maps.newHashMap();
    for (Person person : people) {
      if (person.perseoId != null) {
        maps.put(person.perseoId, person);
      }
    }
    return maps;
  }

  /**
   * La lista di tutti i contratti attivi delle persone sincronizzate con perseo. La chiave è il
   * person.perseoId.
   */
  public Map<Long, Contract> activeContractsEpasByPersonPerseoId(Office office) {
    //Costruisco La mappa di tutte le persone attive epas sincronizzate.
    @SuppressWarnings("deprecation")
    List<Person> people = personDao.listFetched(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, null, null, false).list();
    List<IWrapperPerson> wrapperedPeople = FluentIterable.from(people)
        .transform(wrapperFunctionFactory.person()).toList();

    Map<Long, Contract> activeSynchronizedEpas = Maps.newHashMap();
    for (IWrapperPerson wrPerson : wrapperedPeople) {
      if (wrPerson.getCurrentContract().isPresent() && wrPerson.getValue().perseoId != null) {
        activeSynchronizedEpas.put(wrPerson.getValue().perseoId, wrPerson.getCurrentContract().get());
      }
    }
    return activeSynchronizedEpas;
  }
}
