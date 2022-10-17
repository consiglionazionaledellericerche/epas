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

package synch.perseoconsumers.people;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import dao.QualificationDao;
import helpers.rest.ApiRequestException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.Qualification;
import play.libs.WS;
import synch.perseoconsumers.AnagraficaApis;

/**
 * Classe di supporto per prelevare via REST le informazioni da Perseo.
 *
 */
@Slf4j
public class PeoplePerseoConsumer {

  private final QualificationDao qualificationDao;

  @Inject
  public PeoplePerseoConsumer(QualificationDao qualificationDao) {
    this.qualificationDao = qualificationDao;
  }

  /**
   * Preleva da Perseo i dati di una persona a partire dal suo perseoId.
   *
   * @param perseoId id di Perseo della persona richiesta
   * @return La persona recuperata da Perseo.
   */
  private ListenableFuture<PerseoPerson> perseoPerson(Long perseoId) {

    final String url;
    final String user;
    final String pass;

    try {
      url = AnagraficaApis.getPersonForEpasEndpoint() + perseoId;
      user = AnagraficaApis.getPerseoUser();
      pass = AnagraficaApis.getPerseoPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta Persona a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, response -> {
      if (!response.success()) {
        final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
            response.getStatus(), response.getStatusText());
        log.warn(error);
        throw new ApiRequestException(error);
      }
      log.info("Recuperato Json contenente la Persona con id {} da Perseo", perseoId);
      try {
        return new Gson().fromJson(response.getJson(), PerseoPerson.class);
      } catch (JsonSyntaxException ex) {
        final String error = String.format("Errore nel parsing del json: %s", ex.getMessage());
        log.warn(error);
        throw new ApiRequestException(error);
      }
    }, MoreExecutors.directExecutor());
  }


  /**
   * La lista delle persone presenti su Perseo relative alla sede specificata. Tutte le persone se
   * non si specifica nessun id.
   */
  public ListenableFuture<List<PerseoPerson>> perseoPeople(Optional<Long> departmentPerseoId) {

    final String url;
    final String user;
    final String pass;

    try {
      if (departmentPerseoId.isPresent()) {
        url = AnagraficaApis.getAllDepartmentPeopleForEpasEndpoint() + departmentPerseoId.get();
      } else {
        url = AnagraficaApis.getPeople();
      }
      user = AnagraficaApis.getPerseoUser();
      pass = AnagraficaApis.getPerseoPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta lista persone a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, response -> {
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
          private static final long serialVersionUID = 6287635192815160188L;
        }.getType());
      } catch (JsonSyntaxException ex) {
        final String error = String.format("Errore nel parsing del json: %s", ex.getMessage());
        log.warn(error);
        throw new ApiRequestException(error);
      }
    }, MoreExecutors.directExecutor());
  }

  /**
   * Conversione a oggetti epas. PerseoPerson.
   *
   * @param perseoPerson da convertire
   * @param qualificationsMap mappa delle qualifiche epas
   * @return person
   */
  private Person epasConverter(PerseoPerson perseoPerson,
      Map<Integer, Qualification> qualificationsMap) {

    Person person = new Person();
    person.setName(perseoPerson.firstname);
    person.setSurname(perseoPerson.surname);
    person.setNumber(perseoPerson.number);
    person.setEmail(perseoPerson.email); //per adesso le email non combaciano @iit.cnr.it vs @cnr.it
    if (perseoPerson.eppn != null) {
      person.setEppn(perseoPerson.eppn);
    } else {
      person.setEppn(perseoPerson.email);
    }
    person.setQualification(qualificationsMap.get(perseoPerson.qualification));
    person.setPerseoId(perseoPerson.id);

    person.setPerseoOfficeId(perseoPerson.departmentId);

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
      if (person.getNumber() == null) {
        //non dovrebbe succedere...
        log.warn("Ricevuta dall'anagrafica una persona senza matricola... {}.", person.toString());
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
    } catch (InterruptedException | ExecutionException ex) {
      String error = String.format("Impossibile recuperare le persone da Perseo - %s",
          ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }
    Map<Long, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : epasConverter(perseoPeople)) {
      perseoPeopleMap.put(person.getPerseoId(), person);
    }
    return perseoPeopleMap;
  }

  /**
   * Tutte le persone in perseo.<br> Formato mappa: number -> person
   *
   * @return mappa
   */
  public Map<String, Person> perseoPeopleByNumber(Optional<Long> departmentPerseoId) {

    Map<String, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : perseoPeopleByPerseoId(departmentPerseoId).values()) {
      perseoPeopleMap.put(person.getNumber(), person);
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
    } catch (InterruptedException | ExecutionException ex) {
      String error = String.format("Impossibile recuperare la persona con id %d da Perseo - %s",
          personPerseoId, ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    if (perseoPerson == null) {
      return Optional.<Person>absent();
    }
    Map<Integer, Qualification> qualificationsMap = qualificationDao.allQualificationMap();
    return Optional.fromNullable(epasConverter(perseoPerson, qualificationsMap));
  }

  /**
   * Preleva le informazioni sui badge di una persona.
   */
  public ListenableFuture<PersonBadge> getPersonBadge(Long personId) {

    final String url;
    final String user;
    final String pass;

    try {
      url = AnagraficaApis.getPersonBadge() + personId;
      user = AnagraficaApis.getPerseoUser();
      pass = AnagraficaApis.getPerseoPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Richiesta REST GET {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, response -> {
      if (!response.success()) {
        final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
            response.getStatus(), response.getStatusText());
        log.warn(error);
        throw new ApiRequestException(error);
      }
      log.info("Recuperato Json contenente il badge della persona con id {}", personId);
      try {
        return new Gson().fromJson(response.getJson(), PersonBadge.class);
      } catch (JsonSyntaxException ex) {
        final String error = String.format("Errore nel parsing del json: %s", ex.getMessage());
        log.warn(error);
        throw new ApiRequestException(error);
      }
    }, MoreExecutors.directExecutor());

  }


  /**
   * Preleva le informazioni su tutti i badge associati ad un ufficio.
   */
  public ListenableFuture<List<PersonBadge>> getOfficeBadges(Long departmentPerseoId) {

    final String url;
    final String user;
    final String pass;

    try {
      url = AnagraficaApis.getDepartmentsBadges() + departmentPerseoId;
      user = AnagraficaApis.getPerseoUser();
      pass = AnagraficaApis.getPerseoPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Richiesta REST GET {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, response -> {
      if (!response.success()) {
        final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
            response.getStatus(), response.getStatusText());
        log.warn(error);
        throw new ApiRequestException(error);
      }
      log.info("Recuperato Json contenente i badges dell'ufficio con id {}", departmentPerseoId);
      try {
        return new Gson().fromJson(response.getJson(), new TypeToken<List<PersonBadge>>() {
          private static final long serialVersionUID = -203781881911244237L;
        }.getType());
      } catch (JsonSyntaxException ex) {
        final String error = String.format("Errore nel parsing del json: %s", ex.getMessage());
        log.warn(error);
        throw new ApiRequestException(error);
      }
    }, MoreExecutors.directExecutor());

  }
}
