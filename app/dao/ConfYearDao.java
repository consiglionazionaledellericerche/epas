package dao;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.ConfYear;
import models.query.QConfYear;

import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;


/**
 * IMPORTANTE: per recuperare i valori dei parametri di configurazione
 * utilizzare i metodi forniti in ConfYearManager.
 */

public class ConfYearDao extends DaoBase{

	@Inject
	ConfYearDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @param id
	 * @return la confYear relativa all'id passato come parametro
	 */
	public ConfYear getById(Long id){
		QConfYear confYear = QConfYear.confYear;
		final JPQLQuery query = getQueryFactory().from(confYear)
				.where(confYear.id.eq(id));
		return query.singleResult(confYear);
	}
}
