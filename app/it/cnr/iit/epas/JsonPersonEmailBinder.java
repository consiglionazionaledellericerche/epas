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
import models.Person;
import models.exports.PersonEmailFromJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * binder per il recupero della person a partire dalla email.
 *
 * @author dario
 *
 */
@Global
@StaticInject
public class JsonPersonEmailBinder implements TypeBinder<PersonEmailFromJson> {

  private static final Logger log = LoggerFactory.getLogger(JsonPersonEmailBinder.class);
  @Inject
  private static PersonDao personDao;

  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
                     Class actualClass, Type genericType) throws Exception {

    try {
      List<Person> persons = new ArrayList<Person>();

      Person person = null;
      JsonObject macroJsonObject = new JsonParser().parse(value).getAsJsonObject();

      PersonEmailFromJson pefjl = new PersonEmailFromJson(persons);
      JsonObject jsonObject = null;
      JsonArray jsonArray = macroJsonObject.get("emails").getAsJsonArray();
      String email = "";
      for (JsonElement jsonElement : jsonArray) {

        jsonObject = jsonElement.getAsJsonObject();
        email = jsonObject.get("email").getAsString();

        log.debug("email=%s personDao=%s", email, personDao);
        person = personDao.byEmail(email).orNull();


        if (person != null) {
          persons.add(person);
        }
      }
      log.debug("Ritorno lista persone...%s", persons);
      pefjl.persons = persons;

      return pefjl;
    } catch (Exception ex) {
      log.error("Errore durante il parsing del Json della lista persone {}", ex);
      return null;
    }
  }
}
