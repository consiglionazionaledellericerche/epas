package dao;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import helpers.jpa.ModelQuery.SimpleResults;
import java.util.List;
import javax.persistence.EntityManager;
import models.Institute;
import models.Office;
import models.Role;
import models.User;
import models.query.QInstitute;
import models.query.QOffice;
import models.query.QUsersRolesOffices;

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
   * Preleva l'office dal suo id.
   * 
   * @return l'ufficio identificato dall'id passato come parametro.
   */
  public Office getOfficeById(Long id) {

    final QOffice office = QOffice.office;

    return getQueryFactory().selectFrom(office)
        .where(office.id.eq(id)).fetchOne();
  }

  /**
   * Tutti gli Uffici presenti.
   * 
   * @return la lista di tutti gli uffici presenti sul database.
   */
  public List<Office> getAllOffices() {

    final QOffice office = QOffice.office;

    return getQueryFactory().selectFrom(office).where(office.endDate.isNull()).fetch();
  }

  
  /**
   * @return l'ufficio associato al codice passato come parametro.
   */
  public Optional<Office> byCode(String code) {

    final QOffice office = QOffice.office;
    final Office result = getQueryFactory().selectFrom(office)
        .where(office.code.eq(code)).fetchOne();
    return Optional.fromNullable(result);

  }

  /**
   * @return l'ufficio associato al codice passato come parametro.
   */
  public Optional<Office> byCodeId(String codeId) {
    final QOffice office = QOffice.office;
    final Office result =  getQueryFactory().selectFrom(office)
        .where(office.codeId.eq(codeId))
        .fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * @return l'ufficio associato al perseoId.
   */
  public Optional<Office> byPerseoId(Long perseoId) {
    final QOffice office = QOffice.office;
    final Office result = getQueryFactory().selectFrom(office)
        .where(office.perseoId.eq(perseoId)).fetchOne();
    return Optional.fromNullable(result);
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
  public SimpleResults<Institute> institutes(Optional<String> instituteName,
      Optional<String> officeName, Optional<String> codes, User user, Role role) {

    final QInstitute institute = QInstitute.institute;
    final QOffice office = QOffice.office;
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;

    final BooleanBuilder condition = new BooleanBuilder();
    if (instituteName.isPresent() && !instituteName.get().isEmpty()) {
      condition.and(matchInstituteName(institute, instituteName.get()));
    }
    if (officeName.isPresent() && !officeName.get().isEmpty()) {
      condition.and(matchOfficeName(office, officeName.get()));
    }
    if (codes.isPresent() && !codes.get().isEmpty()) {
      condition.and(office.code.eq(codes.get()).or(office.codeId.eq(codes.get())));
    }

    if (user.isSystemUser()) {
      final JPQLQuery<Institute> query = getQueryFactory()
          .selectFrom(institute)
          .leftJoin(institute.seats, office)
          .where(condition)
          .distinct();
      return ModelQuery.wrap(query, institute);
    }

    final JPQLQuery<Institute> query = getQueryFactory()
        .selectFrom(institute)
        .leftJoin(institute.seats, office)
        .leftJoin(office.usersRolesOffices, uro)
        .where(condition.and(uro.user.eq(user).and(uro.role.eq(role))))
        .distinct();

    return ModelQuery.wrap(query, institute);

  }

  /**
   * Tutte le sedi. //TODO sarebbe meglio usare la offices definita sotto in modo da avere un
   * ordinamento sugli istituti.
   */
  public SimpleResults<Office> allOffices() {

    final QOffice office = QOffice.office;

    final JPQLQuery<Office> query = getQueryFactory()
        .selectFrom(office)
        .distinct()
        .orderBy(office.name.asc());

    return ModelQuery.wrap(query, office);

  }

  /**
   * Le sedi sulle quali l'user ha il ruolo role.
   */
  public SimpleResults<Office> offices(Optional<String> name, User user, Role role) {

    final QOffice office = QOffice.office;
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final QInstitute institute = QInstitute.institute;

    final BooleanBuilder condition = new BooleanBuilder();
    if (name.isPresent()) {
      condition.and(matchOfficeName(office, name.get()));
      condition.and(matchInstituteName(institute, name.get()));
    }
    final JPQLQuery<Office> query;
    if (user.isSystemUser()) {
      query = getQueryFactory()
          .selectFrom(office)
          .leftJoin(office.institute, institute).fetchJoin()
          .where(condition)
          .distinct()
          .orderBy(office.institute.name.asc());

    } else {
      query = getQueryFactory()
          .selectFrom(office)
          .leftJoin(office.usersRolesOffices, uro)
          .leftJoin(office.institute, institute).fetchJoin()
          .where(condition.and(uro.user.eq(user).and(uro.role.eq(role))))
          .distinct()
          .orderBy(office.institute.name.asc());
    }

    return ModelQuery.wrap(query, office);

  }


  public Optional<Institute> byCds(String cds) {

    final QInstitute institute = QInstitute.institute;
    final Institute result = queryFactory.selectFrom(institute).where(institute.cds.eq(cds))
        .fetchOne();
    return Optional.fromNullable(result);
  }

  public Optional<Institute> instituteById(Long id) {

    final QInstitute institute = QInstitute.institute;
    final Institute result = queryFactory.selectFrom(institute).where(institute.id.eq(id))
        .fetchOne();
    return Optional.fromNullable(result);
  }

  public List<Office> byInstitute(Institute institute) {
    final QOffice office = QOffice.office;
    return queryFactory.selectFrom(office).where(office.institute.eq(institute)).fetch();
  }

}
