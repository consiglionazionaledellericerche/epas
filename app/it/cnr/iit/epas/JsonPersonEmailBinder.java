package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;

import org.joda.time.LocalDate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import models.exports.PersonEmailFromJson;
import models.exports.StampingFromClient;
import play.Logger;
import play.data.binding.TypeBinder;

public class JsonPersonEmailBinder implements TypeBinder<PersonEmailFromJson>{

	@Override
	public Object bind(String name, Annotation[] annotations, String value,
			Class actualClass, Type genericType) throws Exception {

		try{
			Logger.debug("Aha!");
			JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
			Logger.debug("Json---");
			PersonEmailFromJson emailFromJson = new PersonEmailFromJson();
			
			String email = jsonObject.get("email").getAsString();
			Logger.debug("Email corretta: %s", email);
			Date dateFrom = new Date
					(jsonObject.get("yearFrom").getAsInt(), jsonObject.get("monthFrom").getAsInt(), jsonObject.get("dayFrom").getAsInt());
			Date dateTo = new Date
					(jsonObject.get("yearTo").getAsInt(), jsonObject.get("monthTo").getAsInt(), jsonObject.get("dayTo").getAsInt());
			Logger.debug("Date fatte correttamente %s %s", dateFrom, dateTo);
			emailFromJson.dateFrom = dateFrom;
			emailFromJson.dateTo = dateTo;
			emailFromJson.email = email;

			Logger.debug("Creato nuovo json %s", emailFromJson);

			return emailFromJson;
		}
		catch(Exception e){
			Logger.error("Ahia...");
			return null;
		}
	}

}
