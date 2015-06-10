package dao;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.Office;
import models.Permission;
import models.Role;
import models.User;
import models.query.QPermission;
import models.query.QRole;
import models.query.QUsersRolesOffices;

import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class PermissionDao extends DaoBase {

	@Inject
	PermissionDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 * @param description
	 * @return il permesso associato alla descrizione passata come parametro
	 */
	public Permission getPermissionByDescription(String description){
		QPermission permission = QPermission.permission;
		final JPQLQuery query = getQueryFactory().from(permission)
				.where(permission.description.eq(description));
		return query.singleResult(permission);
	}
	
	/**
	 * La lista di tutti i permessi che un user dispone su un office.
	 * 
	 * @param user
	 * @param office
	 * @return
	 */
	public List<Permission> getOfficePermissions(User user, Office office) {
		
		QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
		QRole role = QRole.role;
		QPermission permission = QPermission.permission;
		
		
		return getQueryFactory().from(permission)
				.leftJoin(permission.roles, role)
				.leftJoin(role.usersRolesOffices, uro)
				.where(uro.user.eq(user)
				.and(uro.office.eq(office)))
				.distinct()
				.list(permission);

	}
}
