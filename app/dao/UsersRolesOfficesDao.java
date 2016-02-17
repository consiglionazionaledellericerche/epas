package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import helpers.ModelQuery;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QRole;
import models.query.QUsersRolesOffices;

import java.util.List;

import javax.persistence.EntityManager;

public class UsersRolesOfficesDao extends DaoBase {

  @Inject
  UsersRolesOfficesDao(JPQLQueryFactory queryFactory,
                       Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  public UsersRolesOffices getById(Long id) {
    QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final JPQLQuery query = getQueryFactory().from(uro)
            .where(uro.id.eq(id));
    return query.singleResult(uro);
  }

  /**
   * @return l'usersRolesOffice associato ai parametri passati.
   */
  public Optional<UsersRolesOffices> getUsersRolesOffices(User user, Role role, Office office) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final JPQLQuery query = getQueryFactory().from(uro)
            .where(uro.user.eq(user)
                    .and(uro.role.eq(role)
                            .and(uro.office.eq(office))));

    return Optional.fromNullable(query.singleResult(uro));
  }


  /**
   * @return l'usersRolesOffice associato ai parametri passati.
   */
  public Optional<UsersRolesOffices> getUsersRolesOfficesByUserAndOffice(User user, Office office) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final JPQLQuery query = getQueryFactory().from(uro)
            .where(uro.user.eq(user)
                    .and(uro.office.eq(office)));

    return Optional.fromNullable(query.singleResult(uro));
  }
  
  /**
   * 
   * @param user
   * @return la lista di tutti gli usersRolesOffices associati al parametro passato.
   */
  public List<UsersRolesOffices> getUsersRolesOfficesByUser(User user) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final JPQLQuery query = getQueryFactory().from(uro).where(uro.user.eq(user));
    return query.list(uro);
  }

  /**
   * La lista di tutti i ruoli per l'user. Utilizzato per visualizzare gli elementi della navbar.
   */
  public List<Role> getUserRole(User user) {

    final QUsersRolesOffices quro = QUsersRolesOffices.usersRolesOffices;
    final QRole qr = QRole.role;

    final JPQLQuery query = getQueryFactory().from(qr)
            .leftJoin(qr.usersRolesOffices, quro).fetch()
            .distinct();

    final BooleanBuilder condition = new BooleanBuilder();
    condition.and(quro.user.eq(user));

    query.where(condition);

    return ModelQuery.simpleResults(query, qr).list();
  }

}
