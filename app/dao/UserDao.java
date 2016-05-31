package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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
import models.UsersRolesOffices;
import models.query.QPerson;
import models.query.QUser;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class UserDao extends DaoBase {

  public static final String ADMIN_USERNAME = "admin";
  public static final String DEVELOPER_USERNAME = "developer";

  @Inject
  public UsersRolesOfficesDao usersRolesOfficesDao;

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

    final QUser user = QUser.user;
    final JPQLQuery query = getQueryFactory()
        .from(user)
        .where(user.username.eq(username));
    return query.singleResult(user);
  }


  /**
   * Se l'utente è admin.
   */
  public boolean isAdmin(User user) {
    return user.username.equals(ADMIN_USERNAME);
  }

  /**
   * Se l'utente è developer.
   */
  public boolean isDeveloper(User user) {
    return user.username.equals(DEVELOPER_USERNAME);
  }

  /**
   * Ritorna la lista degli utenti che hanno ruolo role nell'ufficio office
   */
  public List<User> getUserByOfficeAndRole(Office office, Role role) {

    List<User> userList = Lists.newArrayList();

    for (UsersRolesOffices uro : office.usersRolesOffices) {

      if (uro.role.id.equals(role.id)) {

        userList.add(uro.user);
      }
    }
    return userList;
  }

  public List<String> containsUsername(String username) {
    Preconditions.checkState(!Strings.isNullOrEmpty(username));
    final QUser user = QUser.user;

    return getQueryFactory().from(user).where(user.username.contains(username)).list(user.username);
  }

  public static enum UserType {
    PERSON, SYSTEM_WITH_OWNER, SYSTEM_WITHOUT_OWNER;
  }

  public static enum EnabledType {
    ONLY_ENABLED, ONLY_DISABLED, BOTH;
  }

  /**
   * @param name       opzionale il nome su cui filtrare
   * @param office     l'ufficio per cui si vogliono gli utenti
   * @param associated se si vogliono solo gli utenti con persona associata o anche quelli di
   *                   sistema
   * @return la lista di utenti che soddisfano i parametri passati.
   */
  public SimpleResults<User> listUsersByOffice(Optional<String> name, Set<Office> offices,
      EnabledType enableType, List<UserType> types) {

    final QUser user = QUser.user;
    final QPerson person = QPerson.person;
    JPQLQuery query = getQueryFactory().from(user)
        .leftJoin(user.person, person);

    BooleanBuilder condition = new BooleanBuilder();

    // Tipi

    if (types.contains(UserType.PERSON)) {
      condition.or(person.office.in(offices));
    }
    if (types.contains(UserType.SYSTEM_WITH_OWNER)) {
      condition.or(person.office.isNull().and(user.owner.in(offices)));
    }
    if (types.contains(UserType.SYSTEM_WITHOUT_OWNER)) {
      condition.or(person.office.isNull().and(user.owner.isNull()));
    }
    // Abilitato / Disabilitato

    if (enableType.equals(EnabledType.ONLY_ENABLED)) {
      condition.and(user.disabled.isFalse());
    } else if (enableType.equals(EnabledType.ONLY_DISABLED)) {
      condition.and(user.disabled.isTrue());
    }

    // Filtro nome
    if (name.isPresent()) {
      condition.and(matchUserName(user, name.get()));
    }

    query.where(condition).distinct().orderBy(user.username.asc());

    return ModelQuery.wrap(query, user);
  }

  private BooleanBuilder matchUserName(QUser user, String name) {
    final BooleanBuilder nameCondition = new BooleanBuilder();
    for (String token : TOKEN_SPLITTER.split(name)) {
      nameCondition.and(user.username.containsIgnoreCase(token));
    }
    return nameCondition.or(user.username.startsWithIgnoreCase(name));
  }
}
