package dao;

import helpers.ModelQuery;
import models.ConfGeneral;
import models.Office;
import models.query.QConfGeneral;

import com.google.common.base.Optional;
import com.mysema.query.jpa.JPQLQuery;

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
	 * @param office
	 * @return il confGeneral relativo al campo field e all'ufficio office passati come parametro
	 */
	public static Optional<ConfGeneral> getConfGeneralByField(String field, Office office){
		QConfGeneral confGeneral = QConfGeneral.confGeneral;
		final JPQLQuery query = ModelQuery.queryFactory().from(confGeneral)
				.where(confGeneral.field.eq(field).and(confGeneral.office.eq(office)));
		return Optional.fromNullable(query.singleResult(confGeneral));
	}

}
