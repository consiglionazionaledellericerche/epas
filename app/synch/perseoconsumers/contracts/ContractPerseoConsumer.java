package synch.perseoconsumers.contracts;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Maps;

import dao.PersonDao;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.rest.ApiRequestException;

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
import java.util.concurrent.ExecutionException;

@Slf4j
public class ContractPerseoConsumer {

  private final PersonDao personDao;
  private final WrapperModelFunctionFactory wrapperFunctionFactory;

  @Inject
  public ContractPerseoConsumer(PersonDao personDao,
                                WrapperModelFunctionFactory wrapperFunctionFactory) {
    this.personDao = personDao;
    this.wrapperFunctionFactory = wrapperFunctionFactory;

  }

  /**
   * @param perseoContractId id di Perseo del contratto richiesta
   * @return Il Contratto relativo all'id specificato.
   */
  private ListenableFuture<PerseoContract> perseoContractByPerseoId(Long perseoContractId) {

    final String url;
    final String user;
    final String pass;

    try {
      url = PerseoApis.getContractForEpasEndpoint() + perseoContractId;
      user = PerseoApis.getPerseoUser();
      pass = PerseoApis.getPerseoPass();
    } catch (NoSuchFieldException e) {
      final String error = String.format("Parametro necessario non trovato: %s", e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta Contratto a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, new Function<HttpResponse, PerseoContract>() {
      @Override
      public PerseoContract apply(WS.HttpResponse response) {
        if (!response.success()) {
          final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
              response.getStatus(), response.getStatusText());
          log.warn(error);
          throw new ApiRequestException(error);
        }
        log.info("Recuperato Json contenente il contratto con id {} da Perseo", perseoContractId);
        try {
          return new Gson().fromJson(response.getJson(), PerseoContract.class);
        } catch (JsonSyntaxException e) {
          final String error = String.format("Errore nel parsing del json: %s", e.getMessage());
          log.warn(error);
          throw new ApiRequestException(error);
        }
      }
    });
  }

  /**
   * @param departmentPerseoId id (di Perseo) della sede sulla quale recuperare i contratti
   * @return La Lista dei contratti delle persone appartenenti alla sede specificata.
   */
  private ListenableFuture<List<PerseoContract>> perseoDepartmentContracts(Long departmentPerseoId) {

    final String url;
    final String user;
    final String pass;

    try {
      url = PerseoApis.getAllDepartmentContractsForEpasEndpoint() + departmentPerseoId;
      user = PerseoApis.getPerseoUser();
      pass = PerseoApis.getPerseoPass();
    } catch (NoSuchFieldException e) {
      final String error = String.format("Parametro necessario non trovato: %s", e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta contratti a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, new Function<HttpResponse, List<PerseoContract>>() {
      @Override
      public List<PerseoContract> apply(WS.HttpResponse response) {
        if (!response.success()) {
          final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
              response.getStatus(), response.getStatusText());
          log.warn(error);
          throw new ApiRequestException(error);
        }
        log.info("Recuperato Json contenente i contratti da Perseo: {} {}",
            response.getStatus(), response.getStatusText());
        try {
          return new Gson().fromJson(response.getJson(), new TypeToken<List<PerseoContract>>() {
          }.getType());
        } catch (JsonSyntaxException e) {
          final String error = String.format("Errore nel parsing del json: %s", e.getMessage());
          log.warn(error);
          throw new ApiRequestException(error);
        }
      }
    });
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

    List<PerseoContract> perseoContracts = Lists.newArrayList();

    try {
      perseoContracts = perseoDepartmentContracts(perseoDepartmentId).get();
    } catch (InterruptedException | ExecutionException e) {
      String error = String.format("Impossibile recuperare i contratti della sede con id %d da Perseo - %s",
          perseoDepartmentId, e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }
    Map<Long, Person> departmentSyncrhonizedPeopleByPerseoId = epasSynchronizedPersonByPersonId(people);
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

    PerseoContract perseoContract = null;
    try {
      perseoContract = perseoContractByPerseoId(contractPerseoId).get();
    } catch (InterruptedException | ExecutionException e) {
      String error = String.format("Impossibile recuperare il contratto con id %d da Perseo - %s",
          contractPerseoId, e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }
    if (perseoContract == null) {
      return Optional.<Contract>absent();
    }
    return Optional.fromNullable(epasConverter(perseoContract, person));
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
