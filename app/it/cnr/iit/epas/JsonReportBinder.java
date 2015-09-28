package it.cnr.iit.epas;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import models.exports.ReportFromJson;
import org.apache.commons.codec.binary.Base64;
import play.Logger;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class JsonReportBinder implements TypeBinder<ReportFromJson>{
	
	private final static String IMAGE_MAGIK = "data:image/png;base64,";

	public static byte[] decodeImage(String imageDataString) {
        return Base64.decodeBase64(imageDataString);
    }

	@Override
	public Object bind(String name, Annotation[] annotations, String value,
			Class actualClass, Type genericType) throws Exception {
		
		try{
			Logger.debug("Eccoci!");
			JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
			
			ReportFromJson report = new ReportFromJson();
			String url = jsonObject.get("url").getAsString();
			String note = jsonObject.get("note").getAsString();
			JsonElement elem = jsonObject.get("img");

			byte[] imageByteArray = decodeImage(elem.getAsString().substring(IMAGE_MAGIK.length()));
            
			report.image = imageByteArray;
			report.note = note;
			report.browserInfo = null;
			report.url = url;
			
			Logger.debug("Effettuato il binding, url: %s note: %s, browser: %s, immagine: %s", report.url, report.note, report.browserInfo, report.image);
			return report;
		}
		catch(Exception e){
			Logger.error("Errore durante il parsing del Json ricevuto da una segnalazione: {}", e);
			return null;
		}
		
		
	}	
	

}
