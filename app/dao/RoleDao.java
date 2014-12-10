package dao;

import helpers.ModelQuery;
import models.Role;
import models.query.QRole;
import com.mysema.query.jpa.JPQLQuery;

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
		return query.list(role).get(0);
	}
}
