package dao;

import helpers.ModelQuery;
import models.BadgeReader;
import models.query.QBadgeReader;

import com.mysema.query.jpa.JPQLQuery;

/**
 * 
 * @author dario
 *
 */
public class BadgeReaderDao {

	/**
	 * 
	 * @param code
	 * @return il badgereader associato al codice passato come parametro
	 */
	public static BadgeReader getBadgeReaderByCode(String code){
		
		final QBadgeReader badge = QBadgeReader.badgeReader;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(badge)
				.where(badge.code.eq(code));
		return query.singleResult(badge);
	}
}
