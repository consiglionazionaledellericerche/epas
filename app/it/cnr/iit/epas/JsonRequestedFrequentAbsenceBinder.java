package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import models.exports.FrequentAbsenceCode;
import models.exports.PeriodAbsenceCode;
import play.Logger;
import play.data.binding.TypeBinder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonRequestedFrequentAbsenceBinder implements TypeBinder<FrequentAbsenceCode>{

	@Override
	public Object bind(String name, Annotation[] annotations, String value,
			Class actualClass, Type genericType) throws Exception {
		
		try{
			
			Logger.debug("Aha!");
			PeriodAbsenceCode periodAbsenceCode = new PeriodAbsenceCode();
			
			Logger.debug("Inizio parsing del json...");
			JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();			
			
			String dataInizio = jsonObject.get("dateFrom").getAsString();
			String dataFine = jsonObject.get("dateTo").getAsString();
			periodAbsenceCode.dateFrom = dataInizio;
			periodAbsenceCode.dateTo = dataFine;
			Logger.debug("Il periodo va da %s a %s", periodAbsenceCode.dateFrom, periodAbsenceCode.dateTo);
			return periodAbsenceCode;
		}
		catch(Exception e){
			Logger.error("Errore durante il parsing del Json....(dei codici di assenza piu' frequenti?): {}", e);
			return null;
		}		
		
	}

}
