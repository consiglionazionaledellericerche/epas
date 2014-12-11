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
	 * @return
	 */
	public static User getUserByRecoveryToken(String recoveryToken){
		QUser user = QUser.user;
		final JPQLQuery query = ModelQuery.queryFactory().from(user)
				.where(user.recoveryToken.eq(recoveryToken));
		return query.singleResult(user);
	}
}
