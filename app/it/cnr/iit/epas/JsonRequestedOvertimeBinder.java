/**
 * 
 */
package it.cnr.iit.epas;

import injection.StaticInject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.exports.PersonsCompetences;
import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.CompetenceCodeDao;
import dao.PersonDao;


/**
 * @author arianna
 *
 */
@Global
@StaticInject
public class JsonRequestedOvertimeBinder implements TypeBinder<PersonsCompetences> {

	@Inject
	private static PersonDao personDao;
	@Inject
	private static CompetenceCodeDao competenceCodeDao;

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

				person = personDao.byEmail(personEmail).orNull();
				if (person == null) {
					throw new IllegalArgumentException(String.format("Person with email = %s doesn't exist", personEmail));			
				}
				Logger.debug("Find persons %s with email %s", person.name, personEmail);

				CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode("S1");
				//CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
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