package manager;

import javax.inject.Inject;

import models.ConfGeneral;
import models.Office;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

import play.cache.Cache;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.ConfGeneralDao;

public class ConfGeneralManager {

	@Inject
	private ConfGeneralDao confGeneralDao;

	/**
	 * Produce la configurazione generale di default. 
	 * 
	 * Se overwrite è false mantiene senza sovrascrivere eventuali parametri generali preesitenti.
	 * 
	 * @param office
	 * @param overwrite
	 */
	public void buildOfficeConfGeneral(Office office, boolean overwrite) {

		for( Parameter param: Parameter.values() ) {

			if(param.isGeneral()) {

				Optional<ConfGeneral> confGeneral = confGeneralDao.getByFieldName(param.description, office);

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
	public Optional<ConfGeneral> saveConfGeneral(Parameter param, Office office, Optional<String> value) {

		String newValue = param.getDefaultValue();

		if(value.isPresent()) {
			newValue = value.get();
		}

		//Prelevo quella esistente
		Optional<ConfGeneral> confGeneral = confGeneralDao.getByFieldName(param.description, office);

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
	public ConfGeneral getByField(Parameter param, Office office) {

		Preconditions.checkState(param.isGeneral());

		Optional<ConfGeneral> confGeneral = confGeneralDao.getByFieldName(param.description, office);

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
	public String getFieldValue(Parameter param, Office office) {

		Preconditions.checkState(param.isGeneral());

		String key = param.description + office.code;

		String value = (String)Cache.get(key);

		if(value == null){

			Optional<ConfGeneral> conf = confGeneralDao.getByFieldName(param.description, office);

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
	public Integer getIntegerFieldValue(Parameter param, Office office) {
		return new Integer(getFieldValue(param,office));
	}

	/**
	 * 
	 * @param param
	 * @param office
	 * @return
	 */
	public Optional<LocalDate> getLocalDateFieldValue(Parameter param, Office office) {
		try {
			return Optional.fromNullable(new LocalDate(getFieldValue(param,office)));
			
		} catch(Exception e) {
			return Optional.<LocalDate>absent(); 
		}
		
	}

	/**
	 * 
	 * @param param
	 * @param office
	 * @return
	 */
	public boolean getBooleanFieldValue(Parameter param, Office office){
		return new Boolean(getFieldValue(param,office));
	}
	
	public Optional<MonthDay> officePatron(Office office){
		
		if(office == null)
			return Optional.absent();
		
		String monthOfPatron = getFieldValue(Parameter.MONTH_OF_PATRON, office);
		String dayOfPatron = getFieldValue(Parameter.DAY_OF_PATRON, office);
		return Optional.of(MonthDay.parse("--"+monthOfPatron+"-"+dayOfPatron));
	}

}
