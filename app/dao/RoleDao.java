package dao;

import java.util.ArrayList;
import java.util.List;

import helpers.ModelQuery;
import models.Permission;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QPermission;
import models.query.QRole;

import com.google.common.base.Optional;
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
	
	
	/**
	 * 
	 * @return
	 */
	public static List<Permission> getAllPermissions(User user) {
		List<Permission> permissions = new ArrayList<Permission>();
		
		if(user.person != null){
			Optional<UsersRolesOffices> uro = UsersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(user, user.person.office);
			if(uro.isPresent()){
				for(Permission p : uro.get().role.permissions){
					permissions.add(p);
				}
			}
//			UsersRolesOffices uro = UsersRolesOffices.find("Select upo from UsersRolesOffices uro where " +
//					"uro.user = ? and uro.office = ?", this, this.person.office).first();
			
			
		}
		
		//TODO admin 
		/*
		else{
			Office office = Office.find("Select off from Office off where off.joiningDate is null").first();
			List<UsersPermissionsOffices> upoList = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where " +
					"upo.user = ? and upo.office = ?", this, office).fetch();
			for(UsersPermissionsOffices upo : upoList){
				permissions.add(upo.permission);
			}
		}
		*/
		return permissions;
	}
}
