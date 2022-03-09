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
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;
import org.joda.time.LocalDate;
import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * Binder per il json dei periodi di reperibilità.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@Global
@StaticInject
public class JsonReperibilityPeriodsBinder implements TypeBinder<ReperibilityPeriods> {

  @Inject
  private static PersonDao personDao;

  /**
   * Binder per le informazioni dei periodi di reperibilità (ReperibilityPeriod).
   *
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(
      String name, Annotation[] annotations, String value, Class actualClass, Type genericType)
          throws Exception {

    log.debug("binding ReperibilityPeriods: {}, {}, {}, {}, {}",
        name, annotations, value, actualClass, genericType);
    try {

      List<ReperibilityPeriod> reperibilityPeriods = new ArrayList<ReperibilityPeriod>();

      JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
      Logger.debug("jsonArray = %s", jsonArray);

      JsonObject jsonObject = null;
      Person person = null;
      //PersonReperibilityType reperibilityType = null;

      Long personId = null;
      //Long reperibilityTypeId = null;

      for (JsonElement jsonElement : jsonArray) {

        jsonObject = jsonElement.getAsJsonObject();
        Logger.trace("jsonObject = %s", jsonObject);

        personId = jsonObject.get("id").getAsLong();
        person = personDao.getPersonById(personId);
        //person = Person.findById(personId);
        if (person == null) {
          throw new IllegalArgumentException(
              String.format("Person with id = %s not found", personId));
        }

        LocalDate start = new LocalDate(jsonObject.get("start").getAsString());
        LocalDate end = new LocalDate(jsonObject.get("end").getAsString());

        ReperibilityPeriod reperibilityPeriod = new ReperibilityPeriod(person, start, end);
        reperibilityPeriods.add(reperibilityPeriod);
      }

      Logger.debug("reperibilityPeriods = %s", reperibilityPeriods);

      return new ReperibilityPeriods(reperibilityPeriods);

    } catch (Exception ex) {
      Logger.error(ex, "Problem during binding List<ReperibilityPeriod>.");
      throw ex;
    }
  }

}
