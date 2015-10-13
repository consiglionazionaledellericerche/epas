package dao;

import helpers.jpa.PerseoModelQuery;
import helpers.jpa.PerseoModelQuery.PerseoSimpleResults;

import javax.persistence.EntityManager;

import models.BadgeReader;
import models.Institute;
import models.Role;
import models.User;
import models.query.QBadgeReader;
import models.query.QInstitute;
import models.query.QOffice;
import models.query.QUsersRolesOffices;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class BadgeReaderDao extends DaoBase {

	public static final Splitter TOKEN_SPLITTER = Splitter.on(' ')
			.trimResults().omitEmptyStrings();
	
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
	
	/**
	 * Gli istituti che contengono sede sulle quali l'user ha il ruolo role.
	 * @param user
	 * @param role
	 * @return
	 */
	public PerseoSimpleResults<BadgeReader> badgeReaders(Optional<String> name) {
		
		final QBadgeReader badgeReader = QBadgeReader.badgeReader;
		
		final BooleanBuilder condition = new BooleanBuilder();
		if (name.isPresent()) {
			condition.and(matchBadgeReaderName(badgeReader, name.get()));
		}
		
		final JPQLQuery query = getQueryFactory()
					.from(badgeReader)
					.where(condition);
		
		return PerseoModelQuery.wrap(query, badgeReader);

	}
	

	private BooleanBuilder matchBadgeReaderName(QBadgeReader badgeReader, String name) {
		final BooleanBuilder nameCondition = new BooleanBuilder();
		for (String token : TOKEN_SPLITTER.split(name)) {
			nameCondition.and(badgeReader.code.containsIgnoreCase(token));
		}
		return nameCondition.or(badgeReader.code.startsWithIgnoreCase(name));
	}
}
