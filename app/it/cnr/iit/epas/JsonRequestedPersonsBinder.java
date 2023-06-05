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

package it.cnr.iit.epas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.injection.StaticInject;
import dao.PersonDao;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.exports.PersonsList;
import play.data.binding.Global;
import play.data.binding.TypeBinder;


/**
 * Binder per il json la lista delle persone.
 *
 * @author Arianna Del Soldato
 */
@Slf4j
@Global
@StaticInject
public class JsonRequestedPersonsBinder implements TypeBinder<PersonsList> {

  @Inject
  private static PersonDao personDao;

  /**
   * Binder per il json la lista delle persone.
   *
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
        log.debug("Find person {} with email {}", person.getName(), personEmail);

        persons.add(person);
      }

      log.debug("persons = {}", persons);

      return new PersonsList(persons);

    } catch (Exception ex) {
      log.error("Problem during binding List<Person>.", ex);
      throw ex;
    }
  }

}
