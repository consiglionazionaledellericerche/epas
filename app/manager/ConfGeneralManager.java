package manager;

import org.joda.time.LocalDate;

import models.ConfGeneral;
import models.Office;
import models.enumerate.Parameter;
import play.cache.Cache;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.ConfGeneralDao;

public class ConfGeneralManager {

	/**
	 * Produce la configurazione generale di default. 
	 * 
	 * Se overwrite è false mantiene senza sovrascrivere eventuali parametri generali preesitenti.
	 * 
	 * @param office
	 * @param overwrite
	 */
	public static void buildOfficeConfGeneral(Office office, boolean overwrite) {

		for( Parameter param: Parameter.values() ) {
			
			if(param.isGeneral()) {
				
				Optional<ConfGeneral> confGeneral = ConfGeneralDao.getByFieldName(param.description, office);
				
				if( !confGeneral.isPresent() || overwrite ) {
					saveConfGeneral(param, office, Optional.<String>absent());
				}
				
			}
			
		}
	}
	
	/**
	 * Aggiorna il parametro di configurazione relativo all'office.
	 * Se value non è presente viene persistito il valore di default.
	 * 
	 * Il valore precedente se presente viene sovrascritto.
	 * 
	 * @param param
	 * @param office
	 * @param value
	 * @return
	 */
	public static Optional<ConfGeneral> saveConfGeneral(Parameter param, Office office, Optional<String> value) {
		
		String newValue = param.getDefaultValue();
		
		if(value.isPresent()) {
			newValue = value.get();
		}
		
		//Prelevo quella esistente
		Optional<ConfGeneral> confGeneral = ConfGeneralDao.getByFieldName(param.description, office);
		
		if(confGeneral.isPresent()) {
			
			confGeneral.get().fieldValue = newValue;
			confGeneral.get().save();
			return confGeneral;
		}
		
		ConfGeneral newConfGeneral = new ConfGeneral(office, param.description, newValue);
		newConfGeneral.save();
		
		return Optional.fromNullable(newConfGeneral);
		
	}
	
	/**
	 * Si recupera l'oggetto quando si vuole modificare il parametro.
	 *  
	 * Se serve il valore utilizzare getFieldValue (utilizzo della cache).
	 * 
	 * @param field
	 * @param office
	 * @return
	 */
	public static ConfGeneral getByField(Parameter param, Office office) {
		
		Preconditions.checkState(param.isGeneral());
		
		Optional<ConfGeneral> confGeneral = ConfGeneralDao.getByFieldName(param.description, office);
		
		if(!confGeneral.isPresent()) {

			confGeneral = saveConfGeneral(param, office, Optional.<String>absent());
		}
		
		return confGeneral.get();
		
		//saveConfGeneral non dovrebbe fallire mai 
		//perchè è sempre definito un default (speriamo)
		
	}

	/**
	 * Preleva dalla cache il valore del campo di configurazione generale.
	 * Se non presente lo crea a partire dal valore di default.
	 * 
	 * @param field
	 * @param office
	 * @return
	 */
	public static String getFieldValue(Parameter param, Office office) {
		
		Preconditions.checkState(param.isGeneral());
		
		String key = param.description + office.code;
		
		String value = (String)Cache.get(key);
		
		if(value == null){
			
			Optional<ConfGeneral> conf = ConfGeneralDao.getByFieldName(param.description, office);
			
			if(!conf.isPresent()){
				
				conf = saveConfGeneral(param, office, Optional.<String>absent());
			}
			
			value = conf.get().fieldValue;
			Cache.set(key, value);
		}
		
		return value;
	}

	/**
	 * 
	 * @param param
	 * @param office
	 * @return
	 */
	public static Integer getIntegerFieldValue(Parameter param, Office office) {
		return new Integer(getFieldValue(param,office));
	}

	/**
	 * 
	 * @param param
	 * @param office
	 * @return
	 */
	public static LocalDate getLocalDateFieldValue(Parameter param, Office office) {
		return new LocalDate(getFieldValue(param,office));
	}
	
	/**
	 * 
	 * @param param
	 * @param office
	 * @return
	 */
	public static boolean getBooleanFieldValue(Parameter param, Office office){
		return new Boolean(getFieldValue(param,office));
	}

}
