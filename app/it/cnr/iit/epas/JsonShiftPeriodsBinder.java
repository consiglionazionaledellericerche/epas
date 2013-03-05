/**
 * 
 */
package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityType;
import models.ShiftTimeTable;
import models.ShiftType;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;
import net.fortuna.ical4j.model.DateTime;

import org.apache.log4j.jmx.LoggerDynamicMBean;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;
import play.db.jpa.JPA;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author arianna
 *
 * Read data sent from the sist-org shift calendar
 */
@Global
public class JsonShiftPeriodsBinder implements TypeBinder<ShiftPeriods> {

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	Class actualClass, Type genericType) throws Exception {
		
		Logger.debug("binding ShiftPeriods: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
		try {
			List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
			
			JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
			Logger.debug("jsonArray di shift period letti = %s", jsonArray);

			JsonObject jsonObject = null;
			Person person = null;
			ShiftType shiftType = null;
			
			Long personId = null;
			
			for (JsonElement jsonElement : jsonArray) {
				jsonObject = jsonElement.getAsJsonObject();
				Logger.trace("jsonObject (shift period letto) = %s", jsonObject);
				
				// read the start and end data of the period
				LocalDate start = new LocalDate(jsonObject.get("start").getAsString());
				LocalDate end = new LocalDate(jsonObject.get("end").getAsString());
				
				if (!jsonObject.get("cancelled").getAsBoolean()) {
					// validate person id
					personId = jsonObject.get("id").getAsLong();
					person = Person.findById(personId);
					if (person == null) {
						throw new IllegalArgumentException(String.format("Person with id = %s not found", personId));
					}
					Logger.debug("letto id = %s corrispondente a person = %s", personId, person.name);
					
					// validate the time table
					String[] hmsStart = jsonObject.get("time_table_start").getAsString().split(":");
					String[] hmsEnd = jsonObject.get("time_table_end").getAsString().split(":");
					
					ShiftTimeTable shiftTimeTable = (ShiftTimeTable) JPA.em().createQuery("SELECT stt FROM ShiftTimeTable stt WHERE stt.startShift = :ldtStart")
							.setParameter("ldtStart", new LocalDateTime(1970, 01, 01, Integer.parseInt(hmsStart[0]), Integer.parseInt(hmsStart[1])))
							.getSingleResult();
	
					Logger.debug("shiftTimeTable = %s", shiftTimeTable);
					if (shiftTimeTable == null) {
						throw new IllegalArgumentException(String.format("shiftTimeTable whith startShift = %s and endShift = %s not found", hmsStart, hmsEnd));
					}
					
					ShiftPeriod shiftPeriod =	new ShiftPeriod(person, start, end, shiftType, false, shiftTimeTable);
					Logger.debug("Creato ShiftPeriod person = %s, start=%s, end=%s, shiftType=%s, shiftTimeTable=%s", person.name, start, end, shiftType, shiftTimeTable.description);
					
					shiftPeriods.add(shiftPeriod);
				} else {
					ShiftPeriod shiftPeriod =	new ShiftPeriod(start, end, shiftType, true);
					shiftPeriods.add(shiftPeriod);
				}
				Logger.debug("letto id = %s corrispondente a person = %s", personId, person.name);
			}
			
			Logger.debug("shiftPeriods = %s", shiftPeriods);
			
			return new ShiftPeriods(shiftPeriods);
			
		} catch (Exception e) {
			Logger.error(e, "Problem during binding List<ShiftPeriod>.");
			throw e;
		}
	}
	
}