package cnr.sync.consumers;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import cnr.sync.Deserializers.PersonDeserializer;
import cnr.sync.dto.PersonDTO;
import cnr.sync.dto.SimplePersonDTO;
import models.Person;
import play.Play;
import play.libs.WS;

public class PeopleConsumer {

  private static final String URL_BASE = Play.configuration.getProperty("perseo.base");
  private static final String PEOPLE_ENDPOINT = Play.configuration
      .getProperty("perseo.rest.people");

  public ListenableFuture<PersonDTO> getPerson(int id) throws IllegalStateException {

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(WS.url(URL_BASE + PEOPLE_ENDPOINT + id).getAsync());
    return Futures.transform(future, new Function<WS.HttpResponse, PersonDTO>() {
      @Override
      public PersonDTO apply(WS.HttpResponse response) {
        if (!response.success()) {
          throw new IllegalStateException("not found");
        }
        return new Gson().fromJson(response.getJson(), PersonDTO.class);
      }
    });
  }

  public ListenableFuture<List<SimplePersonDTO>> getPeople() throws IllegalStateException {

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(WS.url(URL_BASE + PEOPLE_ENDPOINT + "list").getAsync());

    return Futures.transform(future, new Function<WS.HttpResponse, List<SimplePersonDTO>>() {
      @Override
      public List<SimplePersonDTO> apply(WS.HttpResponse response) {
        if (!response.success()) {
          throw new IllegalStateException("not found");
        }
        return new Gson().fromJson(response.getJson(),
            new TypeToken<List<SimplePersonDTO>>() {
            }.getType());
      }
    });
  }

  public ListenableFuture<Set<Person>> seatPeople(String code) throws IllegalStateException {

    final String url = URL_BASE + PEOPLE_ENDPOINT + Play.configuration
        .getProperty("perseo.rest.seatPeople") + code;

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(WS.url(url).getAsync());
    return Futures.transform(future, new Function<WS.HttpResponse, Set<Person>>() {
      @Override
      public Set<Person> apply(WS.HttpResponse response) {
        if (!response.success()) {
          throw new IllegalStateException("not found");
        }
        //  TODO spostare in una classe di configurazione globale
        GsonBuilder gson = new GsonBuilder();
        gson.registerTypeAdapter(Person.class, new PersonDeserializer());

        return gson.create().fromJson(response.getJson(), new TypeToken<Set<Person>>() {
        }.getType());
      }
    });
  }

}
