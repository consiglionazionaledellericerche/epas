package dao;

import javax.persistence.EntityManager;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QUsersRolesOffices;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;

public class UsersRolesOfficesDao extends DaoBase {

	@Inject
	UsersRolesOfficesDao(/*JPQLQueryFactory queryFactory, */Provider<EntityManager> emp) {
		super(/*queryFactory, */emp);
	}

	private final static QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
	
	/**
	 * 
	 * @param user
	 * @param role
	 * @param office
	 * @return l'usersRolesOffice associato ai parametri passati
	 */
	public Optional<UsersRolesOffices> getUsersRolesOffices(User user, Role role, Office office){
		
		final JPQLQuery query = getQueryFactory().from(uro)
				.where(uro.user.eq(user)
				.and(uro.role.eq(role)
				.and(uro.office.eq(office))));
	
		return Optional.fromNullable(query.singleResult(uro));
	}
	
	
	/**
	 * 
	 * @param user
	 * @param office
	 * @return l'usersRolesOffice associato ai parametri passati
	 */
	public Optional<UsersRolesOffices> getUsersRolesOfficesByUserAndOffice(User user, Office office){
		
		final JPQLQuery query = getQueryFactory().from(uro)
				.where(uro.user.eq(user)
				.and(uro.office.eq(office)));
		
		return Optional.fromNullable(query.singleResult(uro));
	}
}
