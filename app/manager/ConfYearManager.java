package manager;

import play.cache.Cache;

import com.google.common.base.Optional;

import dao.ConfYearDao;
import models.ConfYear;
import models.Office;

public class ConfYearManager {

	/**
	 * Produce la configurazione annuale di default al momento della creazione di una nuova sede
	 * @param office
	 */
	public static void buildDefaultConfYear(Office office, Integer year) {

		//TODO inserire i dati di default in un file di configurazione
		
		ConfYear confYear;
	
		confYear = new ConfYear(office, year, ConfYear.MONTH_EXPIRY_VACATION_PAST_YEAR, "8");
		confYear.save();
		
		confYear = new ConfYear(office, year, ConfYear.DAY_EXPIRY_VACATION_PAST_YEAR, "31");
		confYear.save();
		
		confYear = new ConfYear(office, year, ConfYear.MONTH_EXPIRY_RECOVERY_DAYS_13, "0");
		confYear.save();
		
		confYear = new ConfYear(office, year, ConfYear.MONTH_EXPIRY_RECOVERY_DAYS_49, "4");
		confYear.save();
		
		confYear = new ConfYear(office, year, ConfYear.MAX_RECOVERY_DAYS_13, "22");
		confYear.save();
		
		confYear = new ConfYear(office, year, ConfYear.MAX_RECOVERY_DAYS_49, "0");
		confYear.save();
		
		confYear = new ConfYear(office, year, ConfYear.HOUR_MAX_TO_CALCULATE_WORKTIME, "5");
		confYear.save();
		
		return;
	}
	
	
	public static ConfYear getConfGeneralByFieldAndYear(String field, Integer year, Office office){
		
		Optional<ConfYear> conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, field);
		if(conf.isPresent())
//		ConfYear conf = ConfYear.find("Select conf from ConfYear conf where conf.field = ? and conf.office = ? and conf.year = ?", 
//				field, office, year).first();
			return conf.get();
		else
			return null;
	}
	
	
	

	public static String getFieldValue(String field, Integer year, Office office) {
		String value = (String)Cache.get(field+year);
		if(value == null){
			Optional<ConfYear> conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, field);
//			ConfYear conf = ConfYear.find("Select cy from ConfYear cy where cy.year = ? and cy.field = ? and cy.office = ?", 
//					year, field, office).first();
			if(conf.isPresent()){
				value = conf.get().fieldValue;
				Cache.set(field+year, value);
			}
			
			
		}
		return value;
	}
}
