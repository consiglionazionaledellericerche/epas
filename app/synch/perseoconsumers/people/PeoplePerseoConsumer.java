package synch.perseoconsumers.people;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import dao.QualificationDao;

import helpers.rest.ApiRequestException;

import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.Qualification;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import synch.perseoconsumers.PerseoApis;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
public class PeoplePerseoConsumer {

  private final QualificationDao qualificationDao;

  @Inject
  public PeoplePerseoConsumer(QualificationDao qualificationDao) {
    this.qualificationDao = qualificationDao;
  }

  /**
   * @param perseoId id di Perseo della persona richiesta
   * @return La persona recuperata da Perseo.
   */
  private ListenableFuture<PerseoPerson> perseoPerson(Long perseoId) {

    final String url;
    final String user;
    final String pass;

    try {
      url = PerseoApis.getPersonForEpasEndpoint() + perseoId;
      user = PerseoApis.getPerseoUser();
      pass = PerseoApis.getPerseoPass();
    } catch (NoSuchFieldException e) {
      final String error = String.format("Parametro necessario non trovato: %s", e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta Persona a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, new Function<HttpResponse, PerseoPerson>() {
      @Override
      public PerseoPerson apply(WS.HttpResponse response) {
        if (!response.success()) {
          final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
              response.getStatus(), response.getStatusText());
          log.warn(error);
          throw new ApiRequestException(error);
        }
        log.info("Recuperato Json contenente la Persona con id {} da Perseo", perseoId);
        try {
          return new Gson().fromJson(response.getJson(), PerseoPerson.class);
        } catch (JsonSyntaxException e) {
          final String error = String.format("Errore nel parsing del json: %s", e.getMessage());
          log.warn(error);
          throw new ApiRequestException(error);
        }
      }
    });
  }


  /**
   * La lista delle persone presenti su Perseo relative alla sede specificata. Tutte le persone se
   * non si specifica nessun id.
   */
  private ListenableFuture<List<PerseoPerson>> perseoPeople(Optional<Long> departmentPerseoId) {

    final String url;
    final String user;
    final String pass;

    try {
      if (departmentPerseoId.isPresent()) {
        url = PerseoApis.getAllDepartmentPeopleForEpasEndpoint() + departmentPerseoId.get();
      } else {
        url = PerseoApis.getAllPeopleForEpasEndpoint();
      }
      user = PerseoApis.getPerseoUser();
      pass = PerseoApis.getPerseoPass();
    } catch (NoSuchFieldException e) {
      final String error = String.format("Parametro necessario non trovato: %s", e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta lista persone a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, new Function<HttpResponse, List<PerseoPerson>>() {
      @Override
      public List<PerseoPerson> apply(WS.HttpResponse response) {
        if (!response.success()) {
          final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
              response.getStatus(), response.getStatusText());
          log.warn(error);
          throw new ApiRequestException(error);
        }
        log.info("Recuperato Json contenente le persone da Perseo: {} {}",
            response.getStatus(), response.getStatusText());
        try {
          return new Gson().fromJson(response.getJson(), new TypeToken<List<PerseoPerson>>() {
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
   * Conversione a oggetti epas. PerseoPerson.
   *
   * @param perseoPerson      da convertire
   * @param qualificationsMap mappa delle qualifiche epas
   * @return person
   */
  private Person epasConverter(PerseoPerson perseoPerson,
                               Map<Integer, Qualification> qualificationsMap) {

    Person person = new Person();
    person.name = perseoPerson.firstname;
    person.surname = perseoPerson.surname;
    person.number = perseoPerson.number;
    person.email = perseoPerson.email; //per adesso le email non combaciano @iit.cnr.it vs @cnr.it
    person.eppn = perseoPerson.email;
    person.qualification = qualificationsMap.get(perseoPerson.qualification);
    person.perseoId = perseoPerson.id;

    person.perseoOfficeId = perseoPerson.departmentId;

    return person;
  }

  /**
   * Conversione di una lista di oggetti epas. PerseoPerson
   *
   * @param perseoPeople lista di perseoPeople
   * @return persone epas
   */
  private List<Person> epasConverter(List<PerseoPerson> perseoPeople) {
    Map<Integer, Qualification> qualificationsMap = qualificationDao.allQualificationMap();
    List<Person> people = Lists.newArrayList();
    for (PerseoPerson perseoPerson : perseoPeople) {
      Person person = epasConverter(perseoPerson, qualificationsMap);
      if (person.number == null) {
        //non dovrebbe succedere...
        log.info("Giunta da Siper persona senza matricola... {}.", person.toString());
      } else {
        people.add(person);
      }
    }
    return people;
  }

  /**
   * Tutte le persone in perseo, possibile filtrare su una sede.
   * <br> Formato mappa: perseoId -> person
   *
   * @return mappa
   */
  public Map<Long, Person> perseoPeopleByPerseoId(Optional<Long> departmentPerseoId) {

    List<PerseoPerson> perseoPeople = Lists.newArrayList();
    try {
      perseoPeople = perseoPeople(departmentPerseoId).get();
    } catch (InterruptedException | ExecutionException e) {
      String error = String.format("Impossibile recuperare le persone da Perseo - %s",
          e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }
    Map<Long, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : epasConverter(perseoPeople)) {
      perseoPeopleMap.put(person.perseoId, person);
    }
    return perseoPeopleMap;
  }

  /**
   * Tutte le persone in perseo.<br> Formato mappa: number -> person
   *
   * @return mappa
   */
  public Map<Integer, Person> perseoPeopleByNumber(Optional<Long> departmentPerseoId) {

    Map<Integer, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : perseoPeopleByPerseoId(departmentPerseoId).values()) {
      perseoPeopleMap.put(person.number, person);
    }
    return perseoPeopleMap;
  }

  /**
   * La persona con quel perseoId.
   *
   * @return persona
   */
  public Optional<Person> perseoPersonByPerseoId(Long personPerseoId) {
    PerseoPerson perseoPerson = null;
    try {
      perseoPerson = perseoPerson(personPerseoId).get();
    } catch (InterruptedException | ExecutionException e) {
      String error = String.format("Impossibile recuperare la persona con id %d da Perseo - %s",
          personPerseoId, e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    if (perseoPerson == null) {
      return Optional.<Person>absent();
    }
    Map<Integer, Qualification> qualificationsMap = qualificationDao.allQualificationMap();
    return Optional.fromNullable(epasConverter(perseoPerson, qualificationsMap));
  }

}
