package dao;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Role;
import models.User;
import models.query.QRole;
import models.query.QUser;
import models.query.QUsersRolesOffices;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class RoleDao extends DaoBase {

  @Inject
  RoleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
    // TODO Auto-generated constructor stub
  }

  /**
   * @return il ruolo identificato dall'id passato come parametro
   */
  public Role getRoleById(Long id) {
    QRole role = QRole.role;
    final JPQLQuery query = getQueryFactory().from(role)
        .where(role.id.eq(id));
    return query.singleResult(role);
  }

  /**
   * @return il ruolo identificato dal nome passato come parametro
   */
  public Role getRoleByName(String name) {
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
   */
  public List<Role> getSystemRolesOffices() {

    List<Role> roleList = Lists.newArrayList();
    roleList.add(getRoleByName(Role.BADGE_READER));
    roleList.add(getRoleByName(Role.REST_CLIENT));

    return roleList;
  }

  /**
   * 
   * @return la lista dei ruoli non di sistema.
   */
  public List<Role> getAllPhysicalRoles(){
    final QRole role = QRole.role;
    
    JPQLQuery query = getQueryFactory()
        .from(role).where(role.name.notIn(Role.BADGE_READER, Role.REST_CLIENT, Role.DEVELOPER, Role.ADMIN));
        
    return query.list(role);
  }

}
