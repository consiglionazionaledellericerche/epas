package dao;

import javax.persistence.EntityManager;

import models.BadgeReader;
import models.query.QBadgeReader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class BadgeReaderDao extends DaoBase {

	@Inject
	BadgeReaderDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}
	/**
	 * 
	 * @param code
	 * @return il badgereader associato al codice passato come parametro
	 */
	public BadgeReader getBadgeReaderByCode(String code){

		final QBadgeReader badge = QBadgeReader.badgeReader;

		final JPQLQuery query = getQueryFactory().from(badge)
				.where(badge.code.eq(code));
		return query.singleResult(badge);
	}
}
