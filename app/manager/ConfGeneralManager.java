package manager;

import org.joda.time.LocalDate;

import models.ConfGeneral;
import models.Office;
import play.cache.Cache;

import com.google.common.base.Optional;

import dao.ConfGeneralDao;

public class ConfGeneralManager {

	/**
	 * 
	 * @param field
	 * @param office
	 * @return il valore del campo field relativo all'ufficio office passati come parametro
	 */
	public static String getFieldValue(String field, Office office){
		String value = (String)Cache.get(field+office.name);
		if(value == null || value.equals("")){
			Optional<ConfGeneral> conf = ConfGeneralDao.getConfGeneralByField(field, office);
//			ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.field = ? and conf.office = ?", 
//					field, office).first();
			if(conf.isPresent())
				value = conf.get().fieldValue;
			Cache.set(field+office.name, value);
		}
		return value;
	}
	
	/**
	 * 
	 * @param field
	 * @param office
	 * @return l'oggetto conf_general individuato a partire dalla stringa field e dall'ufficio office passati come parametro
	 */
	public static ConfGeneral getConfGeneralByField(String field, Office office){
		
		Optional<ConfGeneral> conf = ConfGeneralDao.getConfGeneralByField(field, office);
//		ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.field = ? and conf.office = ?", 
//				field, office).first();
				
		if(conf.isPresent())
			return conf.get();
		return null;
	}
	
	
	
	/**
	 * Produce la configurazione generale di default al momento della creazione di una nuova sede
	 * @param office
	 */
	public static void buildDefaultConfGeneral(Office office) {

		//TODO inserire i dati di default in un file di configurazione
		
		ConfGeneral confGeneral;

		LocalDate beginYear = new LocalDate(LocalDate.now().getYear(), 1, 1);
		confGeneral = new ConfGeneral(office, ConfGeneral.INIT_USE_PROGRAM, beginYear.toString());
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.DATE_START_MEAL_TICKET,null);
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.SEND_MAIL,"false");
		confGeneral.save();

		confGeneral = new ConfGeneral(office, ConfGeneral.DAY_OF_PATRON, "1");
		confGeneral.save();

		confGeneral = new ConfGeneral(office, ConfGeneral.MONTH_OF_PATRON, "1");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.USER_TO_PRESENCE, null);
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.PASSWORD_TO_PRESENCE, null);
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.URL_TO_PRESENCE, "https://attestati.rm.cnr.it/attestati/" );
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.NUMBER_OF_VIEWING_COUPLE, "2");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.WEB_STAMPING_ALLOWED, "false");
		confGeneral.save();
		
		
		//TODO SPORTARLI NELLA CONFIGURAZIONE PERIODICA QUANDO CI SARA'
		confGeneral = new ConfGeneral(office, ConfGeneral.MEAL_TIME_START_HOUR, "1");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.MEAL_TIME_START_MINUTE, "0");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.MEAL_TIME_END_HOUR, "23");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.MEAL_TIME_END_MINUTE, "0");
		confGeneral.save();
	}
	
}
