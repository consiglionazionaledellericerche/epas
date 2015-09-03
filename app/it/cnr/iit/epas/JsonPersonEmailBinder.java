package it.cnr.iit.epas;

import injection.StaticInject;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.PersonDao;

@Global
@StaticInject
public class JsonPersonEmailBinder implements TypeBinder<PersonEmailFromJson> {

	@Inject
	private static PersonDao personDao;
	
	private final static Logger log = LoggerFactory.getLogger(JsonPersonEmailBinder.class);

	@Override
	public Object bind(String name, Annotation[] annotations, String value,
			Class actualClass, Type genericType) throws Exception {

		try{
			List<Person> persons = new ArrayList<Person>();

			Person person = null;
			JsonObject macroJsonObject = new JsonParser().parse(value).getAsJsonObject();

			PersonEmailFromJson pefjl = new PersonEmailFromJson(persons);
			JsonObject jsonObject = null;
			JsonArray jsonArray = macroJsonObject.get("emails").getAsJsonArray();
			String email = "";
			for(JsonElement jsonElement : jsonArray){

				jsonObject = jsonElement.getAsJsonObject();
				email = jsonObject.get("email").getAsString();

				log.debug("email=%s personDao=%s", email, personDao);
				person = personDao.byEmail(email).orNull();

				
				if (person != null)
					persons.add(person);
			}
			log.debug("Ritorno lista persone...%s", persons);
			pefjl.persons = persons;

			return pefjl;
		}
		catch(Exception e){
			log.error("Errore durante il parsing del Json della lista persone {}", e);
			return null;
		}
	}
}