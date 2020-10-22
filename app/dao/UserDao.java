package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import helpers.jpa.ModelQuery.SimpleResults;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.val;
import manager.configurations.EpasParam;
import models.Office;
import models.Role;
import models.User;
import models.enumerate.StampTypes;
import models.enumerate.TeleworkStampTypes;
import models.query.QBadgeReader;
import models.query.QPerson;
import models.query.QUser;

public class UserDao extends DaoBase {

  @Inject
  UserDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  public static final Splitter TOKEN_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

  /**
   * L'user identificato dall'id passato come parametro.
   * @param id l'identificativo dell'utente
   * @param password (opzionale) la password dell'utente
   * @return lo user  identificato dall'id passato come parametro.
   */
  public User getUserByIdAndPassword(Long id, Optional<String> password) {
    final QUser user = QUser.user;
    final BooleanBuilder condition = new BooleanBuilder();

    if (password.isPresent()) {
      condition.and(user.password.eq(password.get()));
    }
    return getQueryFactory().selectFrom(user)
        .where(condition.and(user.id.eq(id))).fetchOne();
  }

  /**
   * L'utente cui il token appartiene.
   * @param recoveryToken la stringa con il token per ricreare la password
   * @return l'user corrispondente al recoveryToken inviato per il recovery della password.
   */
  public User getUserByRecoveryToken(String recoveryToken) {
    final QUser user = QUser.user;
    return getQueryFactory().selectFrom(user)
        .where(user.recoveryToken.eq(recoveryToken)).fetchOne();
  }

  /**
   * L'user corrispondente all'username e alla password (opzionale) passati.
   * @param username l'username dell'utente
   * @param password (opzionale) la password dell'utente
   * @return l'user corrispondente a username e password passati come parametro.
   */
  public User getUserByUsernameAndPassword(String username, Optional<String> password) {
    final QUser user = QUser.user;
    final BooleanBuilder condition = new BooleanBuilder()
        // Solo gli utenti attivi
        .and(user.disabled.isFalse());

    if (password.isPresent()) {
      condition.and(user.password.eq(password.get()));
    }
    return getQueryFactory().selectFrom(user)
        .where(condition.and(user.username.eq(username))).fetchOne();
  }

  public User byUsername(String username) {
    return getUserByUsernameAndPassword(username, Optional.absent());
  }

  /**
   * Tutti gli username già presenti che contengono il pattern all'interno del proprio username.
   *
   * @param pattern pattern
   * @return list
   */
  public List<String> containsUsername(String pattern) {
    Preconditions.checkState(!Strings.isNullOrEmpty(pattern));
    final QUser user = QUser.user;

    return getQueryFactory().select(user.username).from(user)
        .where(user.username.contains(pattern)).fetch();
  }

  /**
   * La lista degli utenti per sede.
   * @param name Filtro sul nome
   * @param offices Gli uffici che hanno qualche tipo di relazione con gli user restituiti
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

    return ModelQuery.wrap(getQueryFactory().selectFrom(user).leftJoin(user.person, person)
        .leftJoin(user.badgeReaders, badgeReader)
        .where(condition).orderBy(user.username.asc()), user);
  }

  /**
   * la lista degli utenti "orfani".
   * @param name (opzionale) l'eventuale nome su cui fare la restrizione di ricerca
   * @return La lista degli utenti senza legami con una sede.
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

    return ModelQuery.wrap(getQueryFactory().selectFrom(user)
        .leftJoin(user.person, person)
        .leftJoin(user.badgeReaders, badgeReader)
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

  /**
   * Ritorna true se l'user è di sistema oppure è amministatore: responsabile, personale, tecnico.
   *
   * @param user user
   * @return esito
   */
  public boolean hasAdminRoles(User user) {
    Preconditions.checkNotNull(user);

    return user.isSystemUser()
        || user.hasRoles(Role.SEAT_SUPERVISOR, Role.PERSONNEL_ADMIN,
        Role.PERSONNEL_ADMIN_MINI, Role.TECHNICAL_ADMIN);
  }

  /**
   * Gli stamp types utilizzabili dall'user. In particolare gli utenti senza diritti di
   * amministrazione potranno usufruire della sola causale lavoro fuori sede.
   *
   * @param user user
   * @return list
   */
  public static List<StampTypes> getAllowedStampTypes(final User user) {

    if (user.isSystemUser()) {
      return StampTypes.onlyActive();
    }
    val stampTypes = Lists.<StampTypes>newArrayList();
    if (user.hasRoles(Role.PERSONNEL_ADMIN, Role.PERSONNEL_ADMIN_MINI,
        Role.TECHNICAL_ADMIN)) {
      stampTypes.addAll(StampTypes.onlyActiveWithoutOffSiteWork());
    }
    if (user.person.qualification.qualification <= 3
        && user.person.office.checkConf(EpasParam.TR_AUTOCERTIFICATION, "true")) {

      stampTypes.addAll(StampTypes.onlyActiveWithoutOffSiteWork());
    }
    if (user.person.office.checkConf(EpasParam.WORKING_OFF_SITE, "true")
        && user.person.checkConf(EpasParam.OFF_SITE_STAMPING, "true")) {
      stampTypes.add(StampTypes.LAVORO_FUORI_SEDE);
    } 

    return stampTypes;
  }
  
  /**
   * Gli stamp types utilizzabili dall'user. In particolare gli utenti senza diritti di
   * amministrazione potranno usufruire della sola causale lavoro fuori sede.
   *
   * @param user user
   * @return list
   */
  public static List<TeleworkStampTypes> getAllowedTeleworkStampTypes(final User user) {
    
    if (user.isSystemUser()) {
      return TeleworkStampTypes.onlyActive();
    }
    val stampTypes = Lists.<TeleworkStampTypes>newArrayList();
    if (user.person.checkConf(EpasParam.TELEWORK_STAMPINGS, "true")) {
      stampTypes.addAll(TeleworkStampTypes.onlyActiveInTelework());
    }
    return stampTypes;
  }

  /**
   * Gli utenti con almeno uno dei ruoli passati nella lista all'interno dell'office.
   *
   * @param office ufficio del quale restituire gli utenti
   * @param roles ruoli da considerare
   * @return La lista degli utenti con i ruoli specificati nell'ufficio passato come parametro
   */
  public List<User> getUsersWithRoles(final Office office, String... roles) {

    return office.usersRolesOffices.stream()
        .filter(uro -> Arrays.asList(roles).contains(uro.role.name))
        .map(uro -> uro.user).distinct().collect(Collectors.toList());
  }

}
