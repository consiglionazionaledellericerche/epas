package dao;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.ConfGeneral;
import models.query.QConfGeneral;

/**
 * 
 * @author dario
 *
 */
public class ConfGeneralDao {
	
	/**
	 * 
	 * @param id
	 * @return l'oggetto confGeneral relativo all'id passato come parametro
	 */
	public static ConfGeneral getConfGeneralById(Long pk){
		QConfGeneral confGeneral = QConfGeneral.confGeneral;
		final JPQLQuery query = ModelQuery.queryFactory().from(confGeneral)
				.where(confGeneral.id.eq(pk));
		return query.singleResult(confGeneral);
	}
	
	
	/**
	 * 
	 * @param field
	 * @return il confGeneral relativo al campo field passato come parametro
	 */
	public static ConfGeneral getConfGeneralByField(String field){
		QConfGeneral confGeneral = QConfGeneral.confGeneral;
		final JPQLQuery query = ModelQuery.queryFactory().from(confGeneral)
				.where(confGeneral.field.eq(field));
		return query.singleResult(confGeneral);
	}

}
