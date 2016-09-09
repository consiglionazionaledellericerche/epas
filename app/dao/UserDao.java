package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import helpers.jpa.ModelQuery;
import helpers.jpa.ModelQuery.SimpleResults;

import models.Office;
import models.Role;
import models.User;
import models.query.QBadgeReader;
import models.query.QPerson;
import models.query.QUser;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class UserDao extends DaoBase {

  @Inject
  UserDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  public static final Splitter TOKEN_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

  /**
   * @return lo user  identificato dall'id passato come parametro
   */
  public User getUserByIdAndPassword(Long id, Optional<String> password) {
    final QUser user = QUser.user;
    final BooleanBuilder condition = new BooleanBuilder();
    if (password.isPresent()) {
      condition.and(user.password.eq(password.get()));
    }
    final JPQLQuery query = getQueryFactory().from(user)
        .where(condition.and(user.id.eq(id)));
    return query.singleResult(user);
  }

  /**
   * @return l'user corrispondente al recoveryToken inviato per il recovery della password
   */
  public User getUserByRecoveryToken(String recoveryToken) {
    final QUser user = QUser.user;
    final JPQLQuery query = getQueryFactory().from(user)
        .where(user.recoveryToken.eq(recoveryToken));
    return query.singleResult(user);
  }

  /**
   * @return l'user corrispondente a username e password passati come parametro
   */
  public User getUserByUsernameAndPassword(String username, Optional<String> password) {
    final QUser user = QUser.user;
    final BooleanBuilder condition = new BooleanBuilder();
    if (password.isPresent()) {
      condition.and(user.password.eq(password.get()));
    }
    final JPQLQuery query = getQueryFactory().from(user)
        .where(condition.and(user.username.eq(username)));
    return query.singleResult(user);
  }

  public User byUsername(String username) {
    return getUserByUsernameAndPassword(username, Optional.absent());
  }

  public List<String> containsUsername(String username) {
    Preconditions.checkState(!Strings.isNullOrEmpty(username));
    final QUser user = QUser.user;

    return getQueryFactory().from(user).where(user.username.contains(username)).list(user.username);
  }

  /**
   * @param name       Filtro sul nome
   * @param offices    Gli uffici che hanno qualche tipo di relazione con gli user restituiti
   * @param onlyEnable filtra solo sugli utenti abilitati
   * @return una lista di utenti.
   */
  public SimpleResults<User> listUsersByOffice(Optional<String> name, Set<Office> offices,
      boolean onlyEnable) {

    final QUser user = QUser.user;
    final QPerson person = QPerson.person;
    final QBadgeReader badgeReader = QBadgeReader.badgeReader;

    BooleanBuilder condition = new BooleanBuilder()
        // La persona associata all'utente fa parte di uno degli uffici specificati
        .andAnyOf(person.office.in(offices),
            // oppure il proprietario dell'utente è tra gli uffici specificati
            user.owner.in(offices));
    // Filtro nome
    if (name.isPresent()) {
      condition.and(matchUserName(user, name.get()));
    }

    // Abilitato / Disabilitato
    if (onlyEnable) {
      condition.and(user.disabled.isFalse());
    }

    return ModelQuery.wrap(getQueryFactory().from(user).leftJoin(user.person, person)
        .leftJoin(user.badgeReader, badgeReader)
        .where(condition).orderBy(user.username.asc()), user);
  }

  /**
   * @return La lista degli utenti senza legami con una sede
   */
  public SimpleResults<User> noOwnerUsers(Optional<String> name) {

    final QUser user = QUser.user;
    final QPerson person = QPerson.person;
    final QBadgeReader badgeReader = QBadgeReader.badgeReader;

    final BooleanBuilder condition = new BooleanBuilder()
        .and(person.isNull().and(badgeReader.isNull()).and(user.owner.isNull()))
        .or(user.roles.any().isNotNull());

    // Filtro nome
    if (name.isPresent()) {
      condition.and(matchUserName(user, name.get()));
    }

    return ModelQuery.wrap(getQueryFactory().from(user)
        .leftJoin(user.person, person)
        .leftJoin(user.badgeReader, badgeReader)
        .where(condition)
        .orderBy(user.username.asc()), user);
  }


  private BooleanBuilder matchUserName(QUser user, String name) {
    final BooleanBuilder nameCondition = new BooleanBuilder();
    for (String token : TOKEN_SPLITTER.split(name)) {
      nameCondition.and(user.username.containsIgnoreCase(token));
    }
    return nameCondition.or(user.username.startsWithIgnoreCase(name));
  }

  public boolean hasAdminRoles(User user) {
    Preconditions.checkNotNull(user);

    return user.isSystemUser()
        || user.usersRolesOffices.stream().filter(uro -> ImmutableList.of(Role.PERSONNEL_ADMIN,
        Role.PERSONNEL_ADMIN_MINI, Role.TECHNICAL_ADMIN)
        .contains(uro.role.name)).findAny().isPresent();
  }
}
