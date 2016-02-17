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

import helpers.jpa.PerseoModelQuery;
import helpers.jpa.PerseoModelQuery.PerseoSimpleResults;

import models.BadgeReader;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QBadgeReader;
import models.query.QPerson;
import models.query.QUser;
import models.query.QUsersRolesOffices;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class UserDao extends DaoBase {

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


  public boolean isAdmin(User user) {

    if (user.username.equals("admin")) {
      return true;
    } else {
      return false;
    }
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
  
  /**
   * 
   * @param name opzionale il nome su cui filtrare
   * @param office l'ufficio per cui si vogliono gli utenti
   * @param associated se si vogliono solo gli utenti con persona associata o anche quelli di sistema
   * @return la lista di utenti che soddisfano i parametri passati.
   */
  public PerseoSimpleResults<User> listUsersByOffice(Optional<String> name,Office office, boolean associated){
    final QUser user = QUser.user;
    final QUsersRolesOffices userRoleOffice = QUsersRolesOffices.usersRolesOffices;
    final QPerson person = QPerson.person;
    JPQLQuery query = getQueryFactory().from(user)
        .leftJoin(user.usersRolesOffices, userRoleOffice)
        .leftJoin(user.person, person).where(userRoleOffice.office.eq(office));
    BooleanBuilder condition = new BooleanBuilder();
    if (name.isPresent()) {
      condition.and(matchBadgeReaderName(user, name.get()));
    }
    if (associated) {
      condition.and(person.user.isNotNull());
    } else {
      condition.and(person.user.isNull());
      condition.and(user.username.notIn("admin", "developer"));
    }
    query.where(condition).distinct().orderBy(user.username.asc());   
        
    return PerseoModelQuery.wrap(query, user);
  }

  
  private BooleanBuilder matchBadgeReaderName(QUser user, String name) {
    final BooleanBuilder nameCondition = new BooleanBuilder();
    for (String token : TOKEN_SPLITTER.split(name)) {
      nameCondition.and(user.username.containsIgnoreCase(token));
    }
    return nameCondition.or(user.username.startsWithIgnoreCase(name));
  }
}
