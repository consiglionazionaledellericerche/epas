package manager;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.ConfYear;
import models.Office;
import models.enumerate.Parameter;
import models.query.QConfYear;

import org.joda.time.LocalDate;

import play.cache.Cache;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

public class ConfYearManager {

	/**
	 * Questo manager utilizza direttamente JPQL perchè implementa un ulteriore 
	 * strato di astrazione sulle configurazioni (le configurazioni richieste non
	 * esistenti vengono create sulla base dei dati di default o degli anni precedenti)
	 * @param queryFactory
	 * @param emp
	 */
	@Inject
	ConfYearManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		this.queryFactory = new JPAQueryFactory(emp);
	}
	protected final JPQLQueryFactory queryFactory;

	/**
	 * Produce la configurazione annuale per l'office. 
	 * I parametri vengono creati a partire dalla configurazione dell'anno precedente (se presente),
	 * altrimenti dai valori di default.  
	 * 
	 * Se overwrite è false mantiene senza sovrascrivere eventuali parametri generali preesitenti.
	 * 
	 * @param office
	 * @param overwrite
	 */
	public void buildOfficeConfYear(Office office, Integer year, boolean overwrite) {

		for( Parameter param: Parameter.values() ) {

			if(param.isYearly()) {

				Optional<ConfYear> confYear = getByFieldName(param.description, year, office);

				if( !confYear.isPresent() || overwrite ) {

					Optional<ConfYear> previousConfYear = getByFieldName(param.description, year - 1, office);

					String newValue = null;

					if(previousConfYear.isPresent()) {
						newValue = previousConfYear.get().fieldValue;
					}

					saveConfYear(param, office, year, Optional.fromNullable(newValue));
				}
			}
		}
	}

	/**
	 * Aggiorna il parametro di configurazione relativo all'office.
	 * Se value non è presente viene persistito il valore dell'anno precedente.
	 * Se il valore dell'anno precedente non è presente viene persistito il valore di default.
	 * 
	 * Il valore preesistente se presente viene sovrascritto.
	 * 
	 * @param param
	 * @param office
	 * @param value
	 * @return
	 */
	public Optional<ConfYear> saveConfYear(Parameter param, Office office, Integer year, Optional<String> value) {

		//Decido il nuovo valore

		String newValue = param.getDefaultValue();

		Optional<ConfYear> previousConfYear = getByFieldName(param.description, year - 1 , office);

		if(previousConfYear.isPresent()) {
			newValue = previousConfYear.get().fieldValue;
		}

		if(value.isPresent()) {
			newValue = value.get();
		}

		//Prelevo quella esistente
		Optional<ConfYear> confYear = getByFieldName(param.description, year, office);

		if(confYear.isPresent()) {

			confYear.get().fieldValue = newValue;
			confYear.get().save();
			return confYear;
		}

		ConfYear newConfYear = new ConfYear(office, year, param.description, newValue);
		newConfYear.save();

		return Optional.fromNullable(newConfYear);

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
	public ConfYear getByField(Parameter param, Office office, Integer year) {

		Preconditions.checkState(param.isYearly());

		Optional<ConfYear> confYear = getByFieldName(param.description, year, office);

		if(!confYear.isPresent()) {

			confYear = saveConfYear(param, office, year, Optional.<String>absent());
		}

		return confYear.get();

	}

	/**
	 * Preleva dalla cache il valore del campo di configurazione annuale.
	 * Se non presente lo crea a partire da (1) eventuale valore definito per 
	 * l'anno precedente (2) il valore di default.
	 *  
	 * @param param
	 * @param office
	 * @param year
	 * @return
	 */
	public String getFieldValue(Parameter param, Office office, Integer year) {

		Preconditions.checkState(param.isYearly());

		String key = param.description + office.code;

		String value = (String)Cache.get(key);

		if(value == null){

			Optional<ConfYear> conf = getByFieldName(param.description, year, office);

			if(!conf.isPresent()){

				conf = saveConfYear(param, office, year, Optional.<String>absent());
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
	 * @param year
	 * @return
	 */
	public Integer getIntegerFieldValue(Parameter param, Office office, Integer year) {
		return new Integer(getFieldValue(param, office, year));
	}

	/**
	 * 
	 * @param param
	 * @param office
	 * @param year
	 * @return
	 */
	public LocalDate getLocalDateFieldValue(Parameter param, Office office, Integer year) {
		return new LocalDate(getFieldValue(param, office, year));
	}

	/**
	 * L'enumerato associato a ConfYear.
	 * 
	 * @return
	 */
	public Parameter getParameter(ConfYear confYear) {

		Parameter parameter = null;

		for(Parameter param : Parameter.values()) {
			if(param.description.equals(confYear.field)) { 
				parameter = param;
				break;
			}

		}

		Preconditions.checkNotNull(parameter);

		return parameter;

	}
	
	/**
	 * 
	 * @param office
	 * @param year
	 * @param field
	 * @return il conf year di un certo ufficio in un certo anno rispondente al parametro field
	 */
	private Optional<ConfYear> getByFieldName(String field, Integer year, Office office) {

		final QConfYear confYear = QConfYear.confYear;
		final JPQLQuery query = queryFactory.from(confYear);
		
		query.where(confYear.year.eq(year)
				.and(confYear.field.eq(field)).and(confYear.office.eq(office)));

		return Optional.fromNullable(query.singleResult(confYear));
	}

}
