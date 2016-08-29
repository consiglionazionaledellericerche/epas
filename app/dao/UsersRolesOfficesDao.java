package dao;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QBadgeReader;
import models.query.QPerson;
import models.query.QRole;
import models.query.QUser;
import models.query.QUsersRolesOffices;

import org.testng.collections.Maps;
import org.testng.collections.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    return getQueryFactory().from(quro).where(quro.user.eq(user)).distinct().list(quro.role);
  }

  /**
   * Metodo per effettuare check dello stato ruoli epas <-> perseo.
   */
  public Map<Long, Set<String>> getEpasRoles(Optional<Office> office) {

    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final QUser user = QUser.user;
    final QPerson person = QPerson.person;

    ImmutableList<String> rolesName = ImmutableList.of(
        Role.PERSONNEL_ADMIN, Role.PERSONNEL_ADMIN_MINI, Role.TECHNICAL_ADMIN);

    final QRole role = QRole.role;
    List<Role> roles = getQueryFactory().from(role).where(role.name.in(rolesName)).list(role);

    final JPQLQuery query = getQueryFactory().from(uro)
        .leftJoin(uro.user, user).fetch()
        .leftJoin(user.badgeReader, QBadgeReader.badgeReader).fetch()
        .leftJoin(uro.role, role)
        .where(uro.role.in(roles));

    List<UsersRolesOffices> uroList = query.list(uro);

    Map<Long, Set<String>> urosMap = Maps.newHashMap();

    for (UsersRolesOffices uroItem : uroList) {
      if (uroItem.user.person == null || uroItem.user.person.perseoId == null) {
        continue;
      }
      if (office.isPresent() && !office.get().equals(uroItem.user.person.office)) {
        continue;
      }
      Set<String> personUros = urosMap.get(uroItem.user.person.perseoId);
      if (personUros == null) {
        personUros = Sets.newHashSet();
        personUros.add(formatUro(uroItem));
        urosMap.put(uroItem.user.person.perseoId, personUros);
      } else {
        personUros.add(formatUro(uroItem));
      }
    }

    return urosMap;

  }

  public String formatUro(UsersRolesOffices uro) {
    return uro.role.toString() + " - " + uro.office.name;
  }


}
