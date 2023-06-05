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

package manager.cache;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.CompetenceCode;
import models.query.QCompetenceCode;
import org.apache.commons.lang.NotImplementedException;
import play.cache.Cache;

/**
 * Manager per i CompetenceCode.
 */
public class CompetenceCodeManager {

  protected final JPQLQueryFactory queryFactory;
  private static final String COMPETENCE_PREFIX = "comp";

  @Inject
  CompetenceCodeManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.queryFactory = new JPAQueryFactory(emp);
  }

  /**
   * Costruisce o se presente aggiorna il competence code In caso di aggiornamento invalida la
   * cache.
   */
  public void saveCompetenceCode(String code, String value)
      throws NotImplementedException {
    throw new NotImplementedException();
  }

  /**
   * Preleva dalla cache il competence code.
   */
  public CompetenceCode getCompetenceCode(
      String code) {

    Preconditions.checkNotNull(code);

    String key = COMPETENCE_PREFIX + code;

    CompetenceCode value = (CompetenceCode) Cache.get(key);

    if (value == null) {

      value = getCompetenceCodeByCode(code);

      Preconditions.checkNotNull(value);

      Cache.set(key, value);
    }
    value.merge();
    return value;

  }

  /**
   * Il codice di competenza relativo al codice passato.
   *
   * @return il CompetenceCode relativo al codice code passato come parametro.
   */
  private CompetenceCode getCompetenceCodeByCode(
      String code) {

    Preconditions.checkNotNull(code);

    final QCompetenceCode compCode = QCompetenceCode.competenceCode;

    JPQLQuery<?> query = queryFactory.from(compCode)
        .where(compCode.code.eq(code));

    return (CompetenceCode) query.fetchOne();
  }

}
