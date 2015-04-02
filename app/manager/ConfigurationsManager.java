package manager;

import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.ConfYear;
import models.Office;
import models.enumerate.ConfigurationFields;

import org.joda.time.LocalDate;

import play.cache.Cache;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import controllers.Security;

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
	 * 
	 * @param conf
	 * @param year
	 * @param value
	 * @return un oggetto MessageResult contenente un booleano che determina se l'inserimento del parametro passato è andato a buon fine 
	 * oppure no e un messaggio allegato da passare al chiamante nella view nel caso di esito negativo
	 */
	public static MessageResult persistConfYear(ConfYear conf, Integer year, String value){
		MessageResult result = new MessageResult(false, "");
		if(conf.field.equals(ConfigurationFields.DayExpiryVacationPastYear.description)){
			Integer month = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, year, conf.office));
			try{
				new LocalDate(year, month, Integer.parseInt(value));
			}
			catch(Exception e){
				result.message = (Integer.parseInt(value)+"/"+ConfYearManager.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, year, Security.getUser().get().person.office)+"/"+year+" data non valida. Settare correttamente i parametri.");
				result.result = false;
				return result;
			}
		}

		if(conf.field.equals(ConfigurationFields.MonthExpiryVacationPastYear.description)){
			Integer day  = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, year, conf.office));
			try{
				new LocalDate(year, Integer.parseInt(value), day);
			}
			catch(Exception e){
				result.message = (ConfYearManager.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, year, Security.getUser().get().person.office)+"/"+Integer.parseInt(value)+"/"+year+" data non valida. Settare correttamente i parametri.");
				result.result = false;
				return result;
			}

		}

		if(conf.field.equals(ConfigurationFields.MonthExpireRecoveryDays13.description)){
			if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 12){
				result.message = "Bad request";
				result.result = false;
				return result;
			}
		}

		if(conf.field.equals(ConfigurationFields.MonthExpireRecoveryDays49.description)){
			if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 12){
				
				result.message = "Bad request";
				result.result = false;
				return result;
			}
		}

		if(conf.field.equals(ConfigurationFields.MaxRecoveryDays13.description)){
			if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 31){
				result.message = "Bad request";
				result.result = false;
				return result;
			}
		}

		if(conf.field.equals(ConfigurationFields.MaxRecoveryDays49.description)){
			if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 31){
				result.message = "Bad request";
				result.result = false;
				return result;
			}
		}

		conf.fieldValue = value;
		conf.save();
		Cache.set(conf.field+conf.office.name+conf.year, conf.fieldValue);
		result.message = "parametro di configurazione correttamente inserito";
		result.result = true;
		return result;
	}
	
	public static List<String> populateMonths(){
		List<String> mesi = Lists.newArrayList();
		mesi.add("Nessuno");
		for(int i = 1; i < 13; i++){
			mesi.add(DateUtility.getName(i));
		}
		return mesi;
	}
	
	/**
	 * 
	 * @param confList
	 * @param giornoMassimoFerieAnnoPrecedente
	 * @param residuiAnnoPrecedente13
	 * @param residuiAnnoPrecedente49
	 * @param giorniRecupero13
	 * @param giorniRecupero49
	 * @param oreTimbraturaNotturna
	 * @param year
	 * @param office
	 */
	public static void saveConfigurationNextYear(List<ConfYear> confList, String giornoMassimoFerieAnnoPrecedente, String residuiAnnoPrecedente13, String residuiAnnoPrecedente49,
			String giorniRecupero13, String giorniRecupero49, String oreTimbraturaNotturna, int year, Office office){
		for(ConfYear conf : confList){
			if(conf.field.equals(ConfigurationFields.DayExpiryVacationPastYear.description)){
				conf.fieldValue = (new Integer(DateUtility.dayMonth(giornoMassimoFerieAnnoPrecedente, Optional.<String>absent()).getDayOfMonth())).toString();
				conf.save();
			}
			if(conf.field.equals(ConfigurationFields.MonthExpiryVacationPastYear.description)){
				conf.fieldValue = (new Integer(DateUtility.dayMonth(giornoMassimoFerieAnnoPrecedente, Optional.<String>absent()).getMonthOfYear())).toString();
				conf.save();
			}
			if(conf.field.equals(ConfigurationFields.MonthExpireRecoveryDays13.description)){
				conf.fieldValue = (new Integer(DateUtility.fromStringToIntMonth(residuiAnnoPrecedente13))).toString();
				conf.save();
			}
			if(conf.field.equals(ConfigurationFields.MonthExpireRecoveryDays49.description)){
				conf.fieldValue = (new Integer(DateUtility.fromStringToIntMonth(residuiAnnoPrecedente49))).toString();
				conf.save();
			}
			if(conf.field.equals(ConfigurationFields.MaxRecoveryDays13.description)){
				conf.fieldValue = giorniRecupero13;
				conf.save();
			}
			if(conf.field.equals(ConfigurationFields.MaxRecoveryDays49.description)){
				conf.fieldValue = giorniRecupero49;
				conf.save();
			}
			if(conf.field.equals(ConfigurationFields.HourMaxToCalculateWorkTime.description)){
				conf.fieldValue = oreTimbraturaNotturna;
				conf.save();
			}
		}
		
	}
	
	/**
	 * 
	 * @param field
	 * @param value
	 * @param office
	 * @param year
	 */
	public static void saveNewValueField(String field, String value, Office office, int year){
		ConfYear conf = new ConfYear();
		conf.field = field;
		conf.fieldValue = value;
			 
		conf.office = Security.getUser().get().person.office;
		conf.year = year;
		conf.save();
	}
}
