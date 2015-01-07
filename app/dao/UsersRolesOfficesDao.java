package dao;

import helpers.ModelQuery;

import com.google.common.base.Optional;
import com.mysema.query.jpa.JPQLQuery;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QUsersRolesOffices;

public class UsersRolesOfficesDao {

	/**
	 * 
	 * @param user
	 * @param role
	 * @param office
	 * @return l'usersRolesOffice associato ai parametri passati
	 */
	public static Optional<UsersRolesOffices> getUsersRolesOffices(User user, Role role, Office office){
		QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
		JPQLQuery query = ModelQuery.queryFactory().from(uro)
				.where(uro.user.eq(user).and(uro.role.eq(role).and(uro.office.eq(office))));
		return Optional.fromNullable(query.singleResult(uro));
	}
	
	
	/**
	 * 
	 * @param user
	 * @param office
	 * @return l'usersRolesOffice associato ai parametri passati
	 */
	public static Optional<UsersRolesOffices> getUsersRolesOfficesByUserAndOffice(User user, Office office){
		QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
		JPQLQuery query = ModelQuery.queryFactory().from(uro)
				.where(uro.user.eq(user).and(uro.office.eq(office)));
		return Optional.fromNullable(query.singleResult(uro));
	}
}
