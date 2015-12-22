package it.cnr.iit.epas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.PersonDao;

import injection.StaticInject;

import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.exports.PersonsList;

import play.data.binding.Global;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


/**
 * Binder per il json la lista delle persone.
 *
 * @author arianna
 */
@Slf4j
@Global
@StaticInject
public class JsonRequestedPersonsBinder implements TypeBinder<PersonsList> {

  @Inject
  private static PersonDao personDao;

  /**
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(
      String name, Annotation[] annotations, String value, Class actualClass, Type genericType)
          throws Exception {

    log.debug("binding Persons: {}, {}, {}, {}, {}",
        name, annotations, value, actualClass, genericType);
    try {
      List<Person> persons = new ArrayList<Person>();
      log.debug("letto vaue = {}", value);

      JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
      log.debug("jsonArray = {}", jsonArray);

      JsonObject jsonObject = null;
      Person person = null;
      String personEmail = "";

      for (JsonElement jsonElement : jsonArray) {

        jsonObject = jsonElement.getAsJsonObject();
        log.trace("jsonObject = {}", jsonObject);

        personEmail = jsonObject.get("email").getAsString();

        person = personDao.byEmail(personEmail).orNull();
        if (person == null) {
          throw new IllegalArgumentException(
              String.format("Person with email = %s doesn't exist", personEmail));
        }
        log.debug("Find person {} with email {}", person.name, personEmail);

        persons.add(person);
      }

      log.debug("persons = {}", persons);

      return new PersonsList(persons);

    } catch (Exception e) {
      log.error("Problem during binding List<Person>.", e);
      throw e;
    }
  }

}
