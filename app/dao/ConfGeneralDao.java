package dao;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.ConfGeneral;
import models.Office;
import models.query.QConfGeneral;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class ConfGeneralDao extends DaoBase{
	
	@Inject
	ConfGeneralDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}


	/**
	 * 
	 * @param id
	 * @return l'oggetto confGeneral relativo all'id passato come parametro
	 */
	public ConfGeneral getById(Long pk) {
		
		final QConfGeneral confGeneral = QConfGeneral.confGeneral;
		
		final JPQLQuery query = getQueryFactory().from(confGeneral)
				.where(confGeneral.id.eq(pk));
		
		return query.singleResult(confGeneral);
	}
	
	
	/**
	 * 
	 * @param field
	 * @param office
	 * @return il confGeneral relativo al campo field e all'ufficio office passati come parametro
	 */
	public Optional<ConfGeneral> getByFieldName(String field, Office office) {
		
		final QConfGeneral confGeneral = QConfGeneral.confGeneral;
		
		final JPQLQuery query = getQueryFactory().from(confGeneral)
				.where(confGeneral.field.eq(field).and(confGeneral.office.eq(office)));
		
		return Optional.fromNullable(query.singleResult(confGeneral));
	}
	
	/**
	 * @param field
	 * @param value
	 * @return restituisce la lista di tutti i confGeneral 
	 * che nel parametro field, contengono il valore value
	 *  
	 */
	public List<ConfGeneral> containsValue(String field, String value){
		
		final QConfGeneral confGeneral = QConfGeneral.confGeneral;
		
		return getQueryFactory().from(confGeneral).where(confGeneral.field.eq(field)
				.and(confGeneral.fieldValue.contains(value))).list(confGeneral);
		
	}

}
