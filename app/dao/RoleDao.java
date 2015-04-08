package dao;

import helpers.ModelQuery;
import models.Permission;
import models.Role;
import models.query.QPermission;
import models.query.QRole;

import com.mysema.query.jpa.JPQLQuery;

/**
 * 
 * @author dario
 *
 */
public class RoleDao {

	/**
	 * 
	 * @param id
	 * @return il ruolo identificato dall'id passato come parametro
	 */
	public static Role getRoleById(Long id){
		QRole role = QRole.role;
		final JPQLQuery query = ModelQuery.queryFactory().from(role)
				.where(role.id.eq(id));
		return query.singleResult(role);
	}
	
	/**
	 * 
	 * @param name
	 * @return il ruolo identificato dal nome passato come parametro
	 */
	public static Role getRoleByName(String name){
		QRole role = QRole.role;
		final JPQLQuery query = ModelQuery.queryFactory().from(role)
				.where(role.name.eq(name));
		return query.singleResult(role);
	}
	
	
	/**
	 * 
	 * @param description
	 * @return il permesso associato alla descrizione passata come parametro
	 */
	public static Permission getPermissionByDescription(String description){
		QPermission permission = QPermission.permission;
		final JPQLQuery query = ModelQuery.queryFactory().from(permission)
				.where(permission.description.eq(description));
		return query.singleResult(permission);
	}
	
}
