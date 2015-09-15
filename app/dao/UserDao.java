package dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QUser;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
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
public class UserDao extends DaoBase {
	
	@Inject
	UserDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	@Inject
	public UsersRolesOfficesDao usersRolesOfficesDao;
	
	/**
	 * 
	 * @param id
	 * @return l'ufficio identificato dall'id passato come parametro
	 */
	public User getUserById(Long id, Optional<String> password){
		final QUser user = QUser.user;
		final BooleanBuilder condition = new BooleanBuilder();
		if(password.isPresent())
			condition.and(user.password.eq(password.get()));
		final JPQLQuery query = getQueryFactory().from(user)
				.where(condition.and(user.id.eq(id)));
		return query.singleResult(user);
	}
	
	/**
	 * 
	 * @param recoveryToken
	 * @return l'user corrispondente al recoveryToken inviato per il recovery della password
	 */
	public User getUserByRecoveryToken(String recoveryToken){
		final QUser user = QUser.user;
		final JPQLQuery query = getQueryFactory().from(user)
				.where(user.recoveryToken.eq(recoveryToken));
		return query.singleResult(user);
	}
	
	/**
	 * 
	 * @param username
	 * @param password
	 * @return l'user corrispondente a username e password passati come parametro
	 */
	public User getUserByUsernameAndPassword(String username, Optional<String> password){
		final QUser user = QUser.user;
		final BooleanBuilder condition = new BooleanBuilder();
		if(password.isPresent())
			condition.and(user.password.eq(password.get()));
		final JPQLQuery query = getQueryFactory().from(user)
				.where(condition.and(user.username.eq(username)));
		return query.singleResult(user);
	}
	
	
	public boolean isAdmin(User user) {
		
		if(user.username.equals("admin")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Ritorna la lista degli utenti che hanno ruolo role nell'ufficio office
	 * @param office
	 * @param role
	 * @return
	 */
	public List<User> getUserByOfficeAndRole(Office office, Role role) {
		
		List<User> userList = Lists.newArrayList();
		
		for(UsersRolesOffices uro : office.usersRolesOffices) {
			
			if(uro.role.id.equals(role.id)) {
				
				userList.add(uro.user);
			}
		}
		return userList;
	}
	
}
