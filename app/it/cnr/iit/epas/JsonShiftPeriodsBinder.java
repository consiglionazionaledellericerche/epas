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
import models.PersonReperibilityType;
import models.ShiftTimeTable;
import models.ShiftType;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author arianna
 *
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
			Logger.debug("jsonArray = %s", jsonArray);

			JsonObject jsonObject = null;
			Person person = null;
			ShiftType shiftType = null;
			ShiftTimeTable shiftTimeTable = null;
			
			Long personId = null;
			
			for (JsonElement jsonElement : jsonArray) {
				
				jsonObject = jsonElement.getAsJsonObject();
				Logger.trace("jsonObject = %s", jsonObject);
				
				LocalDate start = new LocalDate(jsonObject.get("start").getAsString());
				LocalDate end = new LocalDate(jsonObject.get("end").getAsString());
				
				personId = jsonObject.get("id").getAsLong();
				person = Person.findById(personId);
				
				if (person == null) {
					throw new IllegalArgumentException(String.format("Person with id = %s not found", personId));
				}
				
				ShiftPeriod shiftPeriod =	new ShiftPeriod(person, start, end, shiftType, shiftTimeTable);
				shiftPeriods.add(shiftPeriod);
			}
			
			Logger.debug("shiftPeriods = %s", shiftPeriods);
			
			return new ShiftPeriods(shiftPeriods);
			
		} catch (Exception e) {
			Logger.error(e, "Problem during binding List<ReperibilityPeriod>.");
			throw e;
		}
	}
	
}