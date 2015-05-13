package dao;

import helpers.ModelQuery;

import java.util.List;

import javax.persistence.EntityManager;

import models.Office;
import models.Permission;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QPermission;
import models.query.QRole;
import models.query.QUsersRolesOffices;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

public class UsersRolesOfficesDao extends DaoBase {

	@Inject
	UsersRolesOfficesDao(JPQLQueryFactory queryFactory,
			Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	private final static QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public UsersRolesOffices getById(Long id) {
		QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
		final JPQLQuery query = getQueryFactory().from(uro)
				.where(uro.id.eq(id));
		return query.singleResult(uro);
	}
	
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
	
	/**
	 * La lista dei ruoli di sistema.
	 * 
	 * @return
	 */
	public List<Role> getSystemRolesOffices() {
		
		List<Role> roleList = Lists.newArrayList();
		roleList.add(RoleDao.getRoleByName(Role.BADGE_READER));
		roleList.add(RoleDao.getRoleByName(Role.REST_CLIENT));
		
		return roleList;
		
	}
	
	/**
	 * La lista di tutti i permessi per l'user. 
	 * Utilizzato per visualizzare gli elementi della navbar.
	 * @param user
	 * @return
	 */
	public List<Permission> getUserPermission(User user) {

		final QUsersRolesOffices quro = QUsersRolesOffices.usersRolesOffices;
		final QRole qr = QRole.role;
		final QPermission qp = QPermission.permission;

		final JPQLQuery query = getQueryFactory().from(qp)
				.leftJoin(qp.roles, qr).fetch()
				.leftJoin(qr.usersRolesOffices, quro).fetch()
				.distinct();

		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(quro.user.eq(user));

		query.where(condition);

		List<Permission> pList = ModelQuery.simpleResults(query, qp).list();

		return pList;
	}
}
