package dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import models.Role;
import models.query.QRole;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * 
 * @author dario
 *
 */
public class RoleDao extends DaoBase {

	@Inject
	RoleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 * @param id
	 * @return il ruolo identificato dall'id passato come parametro
	 */
	public Role getRoleById(Long id){
		QRole role = QRole.role;
		final JPQLQuery query = getQueryFactory().from(role)
				.where(role.id.eq(id));
		return query.singleResult(role);
	}

	/**
	 * 
	 * @param name
	 * @return il ruolo identificato dal nome passato come parametro
	 */
	public Role getRoleByName(String name){
		QRole role = QRole.role;
		final JPQLQuery query = getQueryFactory().from(role)
				.where(role.name.eq(name));
		return query.singleResult(role);
	}
	
	public List<Role> getRolesByNames(ImmutableList<String> roles) {
		QRole role = QRole.role;
		final JPQLQuery query = getQueryFactory()
				.from(role).where(role.name.in(roles));
		return query.list(role);
	}

	/**
	 * La lista dei ruoli di sistema.
	 * 
	 * @return
	 */
	public List<Role> getSystemRolesOffices() {
		
		List<Role> roleList = Lists.newArrayList();
		roleList.add(getRoleByName(Role.BADGE_READER));
		roleList.add(getRoleByName(Role.REST_CLIENT));
		
		return roleList;
	}
	
}
