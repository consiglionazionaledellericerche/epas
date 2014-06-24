package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import models.Person;
import models.exports.PersonEmailFromJson;
import play.Logger;
import play.data.binding.TypeBinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class JsonPersonEmailBinder implements TypeBinder<PersonEmailFromJson>{

	@Override
	public Object bind(String name, Annotation[] annotations, String value,
			Class actualClass, Type genericType) throws Exception {

		try{
			List<Person> persons = new ArrayList<Person>();
			Logger.debug("Aha!");
			
			
			Person person = null;
			JsonObject macroJsonObject = new JsonParser().parse(value).getAsJsonObject();

			
			PersonEmailFromJson pefjl = new PersonEmailFromJson(persons);
			JsonObject jsonObject = null;
			JsonArray jsonArray = macroJsonObject.get("emails").getAsJsonArray();
			String email = "";
			for(JsonElement jsonElement : jsonArray){
				
				jsonObject = jsonElement.getAsJsonObject();
				email = jsonObject.get("email").getAsString();

				person = Person.find("SELECT p FROM Person p WHERE p.email = ?", email).first();
				if(person != null)
					persons.add(person);
			}
			Logger.debug("Ritorno lista persone...%s", persons);
			pefjl.persons = persons;

			return pefjl;
		}
		catch(Exception e){
			Logger.error("Ahia...");
			return null;
		}
	}

}
