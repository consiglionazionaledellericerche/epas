package dao;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import helpers.jpa.PerseoModelQuery;
import helpers.jpa.PerseoModelQuery.PerseoSimpleResults;

import models.Institute;
import models.Office;
import models.Role;
import models.User;
import models.query.QInstitute;
import models.query.QOffice;
import models.query.QUsersRolesOffices;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * Dao per gli uffici.
 *
 * @author dario
 */
public class OfficeDao extends DaoBase {

  public static final Splitter TOKEN_SPLITTER = Splitter.on(' ')
          .trimResults().omitEmptyStrings();

  @Inject
  OfficeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return l'ufficio identificato dall'id passato come parametro.
   */
  public Office getOfficeById(Long id) {

    final QOffice office = QOffice.office;

    final JPQLQuery query = getQueryFactory().from(office)
            .where(office.id.eq(id));
    return query.singleResult(office);
  }

  /**
   * @return la lista di tutti gli uffici presenti sul database.
   */
  public List<Office> getAllOffices() {

    final QOffice office = QOffice.office;

    final JPQLQuery query = getQueryFactory().from(office);

    return query.list(office);

  }

  /**
   * @return l'ufficio associato al codice passato come parametro.
   */
  public Optional<Office> byCode(String code) {

    final QOffice office = QOffice.office;
    final JPQLQuery query = getQueryFactory().from(office)
            .where(office.code.eq(code));
    return Optional.fromNullable(query.singleResult(office));

  }

  /**
   * @return l'ufficio associato al codice passato come parametro.
   */
  public Optional<Office> byCodeId(String codeId) {
    final QOffice office = QOffice.office;
    final JPQLQuery query = getQueryFactory().from(office)
            .where(office.codeId.eq(codeId));
    return Optional.fromNullable(query.singleResult(office));
  }
  
  /**
   * @return l'ufficio associato al perseoId
   */
  public Optional<Office> byPerseoId(Long perseoId) {
    final QOffice office = QOffice.office;
    final JPQLQuery query = getQueryFactory().from(office)
            .where(office.perseoId.eq(perseoId));
    return Optional.fromNullable(query.singleResult(office));
  }


  private BooleanBuilder matchInstituteName(QInstitute institute, String name) {
    final BooleanBuilder nameCondition = new BooleanBuilder();
    for (String token : TOKEN_SPLITTER.split(name)) {
      nameCondition.and(institute.name.containsIgnoreCase(token)
              .or(institute.code.containsIgnoreCase(token)));
    }
    return nameCondition.or(institute.name.startsWithIgnoreCase(name))
            .or(institute.code.startsWithIgnoreCase(name));
  }

  private BooleanBuilder matchOfficeName(QOffice office, String name) {
    final BooleanBuilder nameCondition = new BooleanBuilder();
    for (String token : TOKEN_SPLITTER.split(name)) {
      nameCondition.and(office.name.containsIgnoreCase(token));
    }
    return nameCondition.or(office.name.containsIgnoreCase(name));

  }

  /**
   * Gli istituti che contengono sede sulle quali l'user ha il ruolo role.
   */
  public PerseoSimpleResults<Institute> institutes(Optional<String> name, User user, Role role) {

    final QInstitute institute = QInstitute.institute;
    final QOffice office = QOffice.office;
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;

    final BooleanBuilder condition = new BooleanBuilder();
    if (name.isPresent()) {
      condition.and(matchInstituteName(institute, name.get()));
    }

    if (user.isSystemUser()) {
      final JPQLQuery query = getQueryFactory()
              .from(institute)
              .where(condition);
      return PerseoModelQuery.wrap(query, institute);
    }

    final JPQLQuery query = getQueryFactory()
            .from(institute)
            .rightJoin(institute.seats, office)
            .rightJoin(office.usersRolesOffices, uro)
            .where(condition.and(uro.user.eq(user).and(uro.role.eq(role))))
            .distinct();

    return PerseoModelQuery.wrap(query, institute);

  }

  /**
   * Tutte le sedi.
   * //TODO sarebbe meglio usare la offices definita sotto in modo da avere un
   * ordinamento sugli istituti.
   */
  public PerseoSimpleResults<Office> allOffices() {

    final QOffice office = QOffice.office;

    final JPQLQuery query = getQueryFactory()
        .from(office)
        .distinct()
        .orderBy(office.name.asc());

    return PerseoModelQuery.wrap(query, office);

  }
  
  /**
   * Le sedi sulle quali l'user ha il ruolo role.
   */
  public PerseoSimpleResults<Office> offices(Optional<String> name, User user, Role role) {

    final QOffice office = QOffice.office;
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final QInstitute institute = QInstitute.institute;

    final BooleanBuilder condition = new BooleanBuilder();
    if (name.isPresent()) {
      condition.and(matchOfficeName(office, name.get()));
      condition.and(matchInstituteName(institute, name.get()));
    }

    if (user.isSystemUser()) {
      final JPQLQuery query = getQueryFactory()
              .from(office)
              .leftJoin(office.institute, institute).fetch()
              .where(condition)
              .distinct()
              .orderBy(office.institute.name.asc());
      return PerseoModelQuery.wrap(query, office);
    }

    final JPQLQuery query = getQueryFactory()
            .from(office)
            .leftJoin(office.usersRolesOffices, uro)
            .leftJoin(office.institute, institute).fetch()
            .where(condition.and(uro.user.eq(user).and(uro.role.eq(role))))
            .distinct()
            .orderBy(office.institute.name.asc());

    return PerseoModelQuery.wrap(query, office);

  }

  
  public Optional<Institute> byCds(String cds) {

    final QInstitute institute = QInstitute.institute;
    final JPQLQuery query = queryFactory.from(institute).where(institute.cds.eq(cds));
    return Optional.fromNullable(query.singleResult(institute));
  }
  
  public Optional<Institute> instituteById(Long id) {

    final QInstitute institute = QInstitute.institute;
    final JPQLQuery query = queryFactory.from(institute).where(institute.id.eq(id));
    return Optional.fromNullable(query.singleResult(institute));
  }

}
