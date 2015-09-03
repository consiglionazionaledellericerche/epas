package helpers.deserializers;

import java.lang.reflect.Type;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;


public class LocalDateDeserializer implements JsonDeserializer<LocalDate>{

	final static DateTimeFormatter dtf = DateTimeFormat.forPattern("YYYY-MM-dd");
	
	@Override
	public LocalDate deserialize(JsonElement arg0, Type arg1,
			JsonDeserializationContext arg2) throws JsonParseException {
		
		return LocalDate.parse(arg0.getAsString(), dtf);
	}
}