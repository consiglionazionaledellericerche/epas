package it.cnr.iit.epas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.PersonDao;

import injection.StaticInject;

import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Binder per il json dei periodi di reperibilità.
 *
 * @author cristian
 */
@Slf4j
@Global
@StaticInject
public class JsonReperibilityPeriodsBinder implements TypeBinder<ReperibilityPeriods> {

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

    } catch (Exception e) {
      Logger.error(e, "Problem during binding List<ReperibilityPeriod>.");
      throw e;
    }
  }

}
