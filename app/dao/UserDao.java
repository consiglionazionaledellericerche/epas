package dao;

import helpers.ModelQuery;
import models.Office;
import models.User;
import models.query.QOffice;
import models.query.QUser;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

public class UserDao {
	/**
	 * 
	 * @param id
	 * @return l'ufficio identificato dall'id passato come parametro
	 */
	public static User getUserById(Long id, Optional<String> password){
		QUser user = QUser.user;
		final BooleanBuilder condition = new BooleanBuilder();
		if(password.isPresent())
			condition.and(user.password.eq(password.get()));
		final JPQLQuery query = ModelQuery.queryFactory().from(user)
				.where(condition.and(user.id.eq(id)));
		return query.singleResult(user);
	}
	
	/**
	 * 
	 * @param recoveryToken
	 * @return l'user corrispondente al recoveryToken inviato per il recovery della password
	 */
	public static User getUserByRecoveryToken(String recoveryToken){
		QUser user = QUser.user;
		final JPQLQuery query = ModelQuery.queryFactory().from(user)
				.where(user.recoveryToken.eq(recoveryToken));
		return query.singleResult(user);
	}
	
	/**
	 * 
	 * @param username
	 * @param password
	 * @return l'user corrispondente a username e password passati come parametro
	 */
	public static User getUserByUsernameAndPassword(String username, Optional<String> password){
		QUser user = QUser.user;
		final BooleanBuilder condition = new BooleanBuilder();
		if(password.isPresent())
			condition.and(user.password.eq(password.get()));
		final JPQLQuery query = ModelQuery.queryFactory().from(user)
				.where(condition.and(user.username.eq(username)));
		return query.singleResult(user);
	}
}
