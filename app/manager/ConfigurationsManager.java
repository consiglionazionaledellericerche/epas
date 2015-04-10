package manager;

import it.cnr.iit.epas.DateUtility;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import play.cache.Cache;
import controllers.Security;
import models.ConfYear;
import models.Office;
import models.enumerate.ConfigurationFields;
import models.enumerate.Parameter;

public class ConfigurationsManager {
	

	public final static class MessageResult{
		public boolean result;
		public String message;
				
		public MessageResult(boolean result, String message){
			this.result = result;
			this.message = message;
		}
	}

	/**
	 * Validazione del valore di configurazione. Aggiorna la CACHE.
	 * 
	 * @param conf
	 * @param year
	 * @param value
	 * @return 	
	 */
	public static MessageResult persistConfYear(ConfYear conf, String value){
				
		Preconditions.checkNotNull(conf);
		
		Integer year = conf.year;
		
		if(conf.field.equals(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR.description)){
			
			Integer month = ConfYearManager.getIntegerFieldValue(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, conf.office, year);
			try{
				new LocalDate(year, month, Integer.parseInt(value));
			}
			catch(Exception e){
				
				return new MessageResult(false, Integer.parseInt(value) + "/" + month + "/" + year + " data non valida. Settare correttamente i parametri.");
			}
		}

		if(conf.field.equals(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR.description)){
			
			Integer day  = ConfYearManager.getIntegerFieldValue(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, conf.office, year);
			try{
				new LocalDate(year, Integer.parseInt(value), day);
			}
			catch(Exception e){
				return new MessageResult(false, Integer.parseInt(value) + "/" + year + " data non valida. Settare correttamente i parametri.");
			}

		}

		if(conf.field.equals(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13.description)){
			if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 12){
				return new MessageResult(false, "Bad request");
			}
		}

		if(conf.field.equals(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49.description)){
			if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 12){
				return new MessageResult(false, "Bad request");			}
		}

		if(conf.field.equals(Parameter.MAX_RECOVERY_DAYS_13.description)){
			if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 31){
				return new MessageResult(false, "Bad request");
			}
		}

		if(conf.field.equals(Parameter.MAX_RECOVERY_DAYS_49.description)){
			if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 31){
				return new MessageResult(false, "Bad request");
			}
		}

		
		ConfYearManager.saveConfYear(ConfYearManager.getParameter(conf), 
				conf.office, conf.year, Optional.fromNullable(value) );
		
		return new MessageResult(true, "parametro di configurazione correttamente inserito");
	}
	
	public static List<String> populateMonths(){
		List<String> mesi = Lists.newArrayList();
		mesi.add("Nessuno");
		for(int i = 1; i < 13; i++){
			mesi.add(DateUtility.getName(i));
		}
		return mesi;
	}

}
