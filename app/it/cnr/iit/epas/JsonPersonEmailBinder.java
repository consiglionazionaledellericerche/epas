package it.cnr.iit.epas;

import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.crypto.Data;

import org.joda.time.LocalDate;



import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import models.Person;

import models.exports.PersonEmailFromJson;
import models.exports.StampingFromClient;
import play.Logger;
import play.data.binding.TypeBinder;
import play.mvc.Http.Request;


public class JsonPersonEmailBinder implements TypeBinder<PersonEmailFromJson>{

	@Override
	public Object bind(String name, Annotation[] annotations, String value,
			Class actualClass, Type genericType) throws Exception {

		try{
			List<Person> persons = new ArrayList<Person>();
			Logger.debug("Aha!");
			
			//Logger.debug("JsonArray---");
			Person person = null;
			JsonObject macroJsonObject = new JsonParser().parse(value).getAsJsonObject();
			Logger.debug("Macro json: %s", macroJsonObject.toString());
			String dataInizio = macroJsonObject.get("dateFrom").getAsString();
			String dataFine = macroJsonObject.get("dateTo").getAsString();
			Logger.debug("Date inizio e fine...%s %s", dataInizio, dataFine);
			//String dateFrom = new Date(dataInizio);
			//Date dateTo = new Date(dataFine);
			PersonEmailFromJson pefjl = new PersonEmailFromJson(persons, dataInizio, dataFine);
			JsonObject jsonObject = null;
			JsonArray jsonArray = macroJsonObject.get("emails").getAsJsonArray();
			String email = "";
			for(JsonElement jsonElement : jsonArray){
				
				jsonObject = jsonElement.getAsJsonObject();
				email = jsonObject.get("email").getAsString();
				Logger.debug("Email corretta: %s", email);
				person = Person.find("SELECT p FROM Person p WHERE p.contactData.email = ?", email).first();
				Logger.debug("Trovata persona: %s %s", person.name, person.surname);								
				
				persons.add(person);
			}
			Logger.debug("Ritorno lista persone...%s", persons);
			pefjl.persons = persons;
			pefjl.dateFrom = dataInizio;
			pefjl.dateTo = dataFine;
			Logger.debug("Data inizio: %s", dataInizio);
			Logger.debug("Data fine: %s", dataFine);
			return pefjl;
		}
		catch(Exception e){
			Logger.error("Ahia...");
			return null;
		}
	}

}
