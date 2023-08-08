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

import static com.querydsl.core.group.GroupBy.groupBy;

import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.JwtToken;
import models.Qualification;
import models.absences.query.QAbsenceType;
import models.enumerate.QualificationMapping;
import models.flows.query.QGroup;
import models.query.QJwtToken;
import models.query.QQualification;
import org.joda.time.LocalDateTime;

/**
 * Dao per l'accesso alle informazioni dei Jwt Token.
 *
 * @author Cristian Lucchesi
 */
public class JwtTokenDao extends DaoBase {

  @Inject
  JwtTokenDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Il JwtToken, se esiste, dal idToken passato come parametro.
   *
   * @param idToken del jwt token da cercare.
   * @return il jwt token corrispondente all'idToken indicato.
   */
  public Optional<JwtToken> byIdToken(String idToken) {

    Preconditions.checkNotNull(idToken);
    final QJwtToken jwtToken = QJwtToken.jwtToken;
    return Optional.ofNullable(
        getQueryFactory().selectFrom(jwtToken).where(jwtToken.idToken.eq(idToken)).fetchFirst());

  }

  /**
   * Il JwtToken, se esiste, dal id passato come parametro.
   *
   * @param id del jwt token da cercare.
   * @return il jwt token corrispondente all'id indicato.
   */
  public Optional<JwtToken> byId(Long id) {
    Preconditions.checkNotNull(id);
    final QJwtToken jwtToken = QJwtToken.jwtToken;
    return Optional.ofNullable(
        getQueryFactory().selectFrom(jwtToken).where(jwtToken.id.eq(id)).fetchFirst());
  }

  /**
   * @return lista dei jwtToken che sono già scaduti.
   */
  public List<JwtToken> expiredTokens() {
    final QJwtToken jwtToken = QJwtToken.jwtToken;
    return getQueryFactory().selectFrom(jwtToken)
        .where(jwtToken.expiresAt.before(LocalDateTime.now())).fetch();
  }

  /**
   * Salva il jwtToken.
   *
   * @param jwtToken il da salvare
   */
  public JwtToken persist(JwtToken jwtToken) {
    emp.get().persist(jwtToken);
    return jwtToken;
  }

  public JwtToken save(JwtToken jwtToken) {
    return emp.get().merge(jwtToken);
  }

  /**
   * Elimina il jwtToken.
   *
   * @param jwtToken il da eliminare
   */
  public void delete(JwtToken jwtToken) {
    emp.get().remove(jwtToken);
  }

  /**
   * Cancella il jwtToken con l'idToken passato per parametro.
   *
   * @param idToken da cercare per cancellare il jwtToken.
   * @return il numero di jwtToken cancellati.
   */
  public long deleteByIdToken(String idToken) {
    final QJwtToken jwtToken = QJwtToken.jwtToken;
    return getQueryFactory().delete(jwtToken).where(jwtToken.idToken.eq(idToken)).execute();
  }

}