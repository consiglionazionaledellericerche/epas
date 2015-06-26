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

import models.Person;
import models.exports.PersonsList;
import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.PersonDao;


/**
 * @author arianna
 *
 */
@Global
@StaticInject
public class JsonRequestedPersonsBinder implements TypeBinder<PersonsList> {

	@Inject
	private static PersonDao personDao;

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	Class actualClass, Type genericType) throws Exception {

		Logger.debug("binding Persons: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
		try {
			List<Person> persons = new ArrayList<Person>();
			Logger.debug("letto vaue = %s", value);

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
				Logger.debug("Find person %s with email %s", person.name, personEmail);

				persons.add(person);
			}

			Logger.debug("persons = %s", persons);

			return new PersonsList(persons);

		} catch (Exception e) {
			Logger.error(e, "Problem during binding List<Person>.");
			throw e;
		}
	}

}
