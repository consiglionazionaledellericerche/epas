package helpers.deserializers;

import java.lang.reflect.Type;

import javax.inject.Inject;

import models.Person;
import models.exports.AbsenceFromClient;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import dao.PersonDao;


public class LocalDateDeserializer implements JsonDeserializer<LocalDate>{

	final static DateTimeFormatter dtf = DateTimeFormat.forPattern("YYYY-MM-dd");
	
	@Override
	public LocalDate deserialize(JsonElement arg0, Type arg1,
			JsonDeserializationContext arg2) throws JsonParseException {
		
		return LocalDate.parse(arg0.getAsString(), dtf);
	}
}