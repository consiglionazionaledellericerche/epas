package dao;

import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Role;
import models.query.QRole;

/**
 * Dao per l'accesso alle informazioni dei Role.
 *
 * @author dario
 */
public class RoleDao extends DaoBase {

  @Inject
  RoleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
    // TODO Auto-generated constructor stub
  }

  /**
   * Il ruolo identificato dall'id passato come parametro.
   * @param id l'identificativo del ruolo
   * @return il ruolo identificato dall'id passato come parametro.
   */
  public Role getRoleById(Long id) {
    final QRole role = QRole.role;
    return getQueryFactory().selectFrom(role)
        .where(role.id.eq(id))
        .fetchOne();
  }

  /**
   * Il ruolo identificato dal nome passato come parametro.
   * @name il nome del ruolo
   * @return il ruolo identificato dal nome passato come parametro.
   */
  public Role getRoleByName(String name) {
    final QRole role = QRole.role;
    return getQueryFactory().selectFrom(role)
        .where(role.name.eq(name))
        .fetchOne();
  }

  /**
   * La lista dei ruoli disponibili.
   * @return Tutti i ruoli disponibili.
   */
  public List<Role> getAll() {
    final QRole role = QRole.role;
    return getQueryFactory().selectFrom(role).fetch();
  }

}
