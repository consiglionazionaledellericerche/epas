package dao;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.StampType;
import models.Stamping;
import models.query.QStampType;
import models.query.QStamping;

public class StampingDao {

	/**
	 * 
	 * @param id
	 * @return la timbratura corrispondente all'id passato come parametro
	 */
	public static Stamping getStampingById(Long id){
		QStamping stamping = QStamping.stamping;
		final JPQLQuery query = ModelQuery.queryFactory().from(stamping)
				.where(stamping.id.eq(id));
		return query.singleResult(stamping);
	}
	
	
	/************************************************************************************************************************************/
	/*Inserisco in questa classe anche i metodi di ricerca per gli StampType di modo da evitare di creare classi che farebbero risultare*/
	/*la ricerca troppo dispersiva																										*/
	/************************************************************************************************************************************/
	
	/**
	 * 
	 * @param description
	 * @return lo stampType corrispondente alla descrizione passata come parametro
	 */
	public static StampType getStampTypeByCode(String code){
		QStampType stampType = QStampType.stampType;
		final JPQLQuery query = ModelQuery.queryFactory().from(stampType)
				.where(stampType.code.eq(code));
		return query.singleResult(stampType);
	}
}
