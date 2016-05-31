package it.cnr.iit.epas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.ShiftType;
import models.enumerate.ShiftSlot;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;

import org.joda.time.LocalDate;

import injection.StaticInject;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Read json sent from the sist-org shift calendar. Json consist of periods of shift of a
 * certain type and slot [{ id: id of the person in the shift start : start date end: end
 * date cancelled: true/false shiftSlot: slot of the shift (morning/afternoon) }]
 *
 * @author arianna
 *
 */
@Slf4j
@Global
@StaticInject
public class JsonShiftPeriodsBinder implements TypeBinder<ShiftPeriods> {

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

    log.debug("binding ShiftPeriods: {}, {}, {}, {}, {}",
        name, annotations, value, actualClass, genericType);
    try {
      List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();

      JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
      log.debug("\n\njsonArray di shift period letti = {} \n\n", jsonArray);

      JsonObject jsonObject = null;
      Person person = null;
      ShiftType shiftType = null;

      Long personId = null;

      for (JsonElement jsonElement : jsonArray) {
        jsonObject = jsonElement.getAsJsonObject();
        log.trace("jsonObject (shift period letto) = {}", jsonObject);

        // read the start and end data of the period
        LocalDate start = new LocalDate(jsonObject.get("start").getAsString());
        LocalDate end = new LocalDate(jsonObject.get("end").getAsString());

        // get a real shift period
        if (!jsonObject.get("cancelled").getAsBoolean()) {
          // validate person id
          personId = jsonObject.get("id").getAsLong();
          person = personDao.getPersonById(personId);
          //person = Person.findById(personId);
          if (person == null) {
            throw new IllegalArgumentException(
                String.format("Person with id = %s not found", personId));
          }
          log.debug("letto id = {} corrispondente a person = {}", personId, person.name);


          // read and validate the shift slot (MORNING/AFTERNOON)
          String shiftSlotDesc = jsonObject.get("shiftSlot").getAsString();
          log.debug("Leggo dal json shiftSlotDesc={}", shiftSlotDesc);

          ShiftSlot shiftSlot = ShiftSlot.getEnum(shiftSlotDesc);
          log.debug("Cerca e controlla shiftSlot={}", shiftSlot);
          if (shiftSlot == null) {
            throw new IllegalArgumentException(
                String.format("ShiftSlot with name = %s not found2", shiftSlotDesc));
          }

          ShiftPeriod shiftPeriod =
              new ShiftPeriod(person, start, end, shiftType, false, shiftSlot);
          log.debug("Creato ShiftPeriod person = {}, start={}, end={}, shiftType={}, shiftSlot={}",
              person.name, start, end, shiftType, shiftSlot);

          shiftPeriods.add(shiftPeriod);
          log.debug("letto id = {} corrispondente a person = {}", personId, person.name);
        } else {
          ShiftPeriod shiftPeriod = new ShiftPeriod(start, end, shiftType, true);
          shiftPeriods.add(shiftPeriod);
        }

      }

      log.debug("shiftPeriods = {}", shiftPeriods);

      return new ShiftPeriods(shiftPeriods);

    } catch (Exception e) {
      log.error("Problem during binding List<ShiftPeriod>.", e);
      throw e;
    }
  }

}

