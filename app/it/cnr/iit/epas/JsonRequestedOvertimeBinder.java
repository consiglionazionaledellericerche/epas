/**
 * 
 */
package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.exports.PersonsCompetences;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import org.joda.time.LocalDate;

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
public class JsonRequestedOvertimeBinder implements TypeBinder<PersonsCompetences> {

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	Class actualClass, Type genericType) throws Exception {
		
		Logger.debug("binding ReperibilityCompetence: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
		try {
			List<Competence> personsCompetences = new ArrayList<Competence>();
			
			JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
			Logger.debug("jsonArray = %s", jsonArray);

			JsonObject jsonObject = null;
			Person person = null;
			String personEmail = "";
			
			for (JsonElement jsonElement : jsonArray) {
				
				jsonObject = jsonElement.getAsJsonObject();
				Logger.trace("jsonObject = %s", jsonObject);
				
				personEmail = jsonObject.get("email").getAsString();
				
				person = Person.find("SELECT p FROM Person p WHERE p.contactData.email = ?", personEmail).first();
				if (person == null) {
					throw new IllegalArgumentException(String.format("Person with email = %s doesn't exist", personEmail));			
				}
				Logger.debug("Find persons %s with email %s", person.name, personEmail);
				
				CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
				Competence competence =	new Competence(person, competenceCode, 0, 0);
				competence.setValueApproved(jsonObject.get("ore").getAsInt(), jsonObject.get("motivazione").getAsString());
				
				Logger.debug("Letto ore = %d e motivazione = %s", jsonObject.get("ore").getAsInt(), jsonObject.get("motivazione").getAsString());
				
				personsCompetences.add(competence);
			}
			
			Logger.debug("personsCompetence = %s", personsCompetences);
			
			return new PersonsCompetences(personsCompetences);
			
		} catch (Exception e) {
			Logger.error(e, "Problem during binding List<Competence>.");
			throw e;
		}
	}
	
}