/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dao;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import helpers.jpa.ModelQuery.SimpleResults;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import manager.configurations.EpasParam;
import models.Institute;
import models.Office;
import models.Role;
import models.User;
import models.query.QConfiguration;
import models.query.QInstitute;
import models.query.QOffice;
import models.query.QUsersRolesOffices;
import org.joda.time.LocalDate;
import org.testng.util.Strings;

/**
 * Dao per gli uffici.
 *
 * @author Dario Tagliaferri
 * @author Cristian Lucchesi
 */
public class OfficeDao extends DaoBase {

  public static final Splitter TOKEN_SPLITTER = Splitter.on(' ')
      .trimResults().omitEmptyStrings();

  @Inject
  OfficeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Cerca un ufficio per id, code o codeId, in questo ordine.
   */
  public Optional<Office> byIdOrCodeOrCodeId(Long id, String code, String codeId) {
    if (id != null) {
      return Optional.fromNullable(getOfficeById(id));
    }
    if (!Strings.isNullOrEmpty(code)) {
      return byCode(code);
    }
    if (!Strings.isNullOrEmpty(codeId)) {
      return byCodeId(codeId);
    }
    return Optional.absent();
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
   * Uffici che hanno abilitato il parametro per effettuare le timbrature
   * via web.
   */
  public List<Office> getOfficesWebStampingEnabled() {
    final QOffice office = QOffice.office;
    final QConfiguration configuration = QConfiguration.configuration;
    return getQueryFactory().selectFrom(office)
        .join(office.configurations, configuration)
        .where(
            office.endDate.isNull(), 
            configuration.epasParam.eq(EpasParam.WEB_STAMPING_ALLOWED),
            configuration.fieldValue.equalsIgnoreCase("true"))
        .distinct().fetch();
  }

  /**
   * L'ufficio con il codice code.
   *
   * @param code il codice della sede
   * @return l'ufficio associato al codice passato come parametro.
   */
  public Optional<Office> byCode(String code) {

    final QOffice office = QOffice.office;
    final Office result = getQueryFactory().selectFrom(office)
        .where(office.code.eq(code)).fetchOne();
    return Optional.fromNullable(result);

  }

  /**
   * L'ufficio, se esiste, con il codeId passato come parametro.
   *
   * @param codeId il codice della sede 
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
   * L'ufficio, se esiste, con perseoId uguale a quello passato come parametro.
   *
   * @param perseoId l'id dell'anagrafica
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

  /**
   * L'istituto con cds passato come parametro.
   *
   * @param cds centro di spesa
   * @return l'istituto, se esiste, con centro di spesa uguale a quello passato.
   */
  public Optional<Institute> byCds(String cds) {

    final QInstitute institute = QInstitute.institute;
    final Institute result = queryFactory.selectFrom(institute).where(institute.cds.eq(cds))
        .fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * L'istituto con id passato come parametro.
   *
   * @param id l'identificativo dell'istituto
   * @return l'istituto, se esiste, con id passato come parametro
   */
  public Optional<Institute> instituteById(Long id) {

    final QInstitute institute = QInstitute.institute;
    final Institute result = queryFactory.selectFrom(institute).where(institute.id.eq(id))
        .fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * Lista degli uffici dell'istituto passato come parametro.
   */
  public List<Office> byInstitute(Institute institute) {
    final QOffice office = QOffice.office;
    return queryFactory.selectFrom(office).where(office.institute.eq(institute)).fetch();
  }
  
  /**
   * Lista di tutte le sedi attualmente abilitate e non chiuse.
   */
  public List<Office> allEnabledOffices() {
    final QOffice office = QOffice.office;
    return queryFactory.selectFrom(office)
        .where(office.endDate.isNull()
            .or(office.endDate.after(LocalDate.now()))).fetch();
  }

}
