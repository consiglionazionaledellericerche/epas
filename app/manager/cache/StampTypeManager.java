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
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.query.QStampModificationType;
import org.apache.commons.lang.NotImplementedException;
import play.cache.Cache;

/**
 * Manager per gli StampType.
 */
public class StampTypeManager {

  protected final JPQLQueryFactory queryFactory;
  private static final String SMT_PREFIX = "smt";

  /**
   * Default constructor per l'injection.
   */
  @Inject
  StampTypeManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.queryFactory = new JPAQueryFactory(emp);
  }

  /**
   * Lo stampModificationType relativo al codice passato.
   *
   * @return lo stampModificationType relativo al codice code passato come parametro.
   */
  private StampModificationType getStampModificationTypeByCode(
      StampModificationTypeCode smtCode) {

    Preconditions.checkNotNull(smtCode);

    final QStampModificationType smt = QStampModificationType.stampModificationType;

    JPQLQuery<?> query = queryFactory.from(smt)
        .where(smt.code.eq(smtCode.getCode()));

    return (StampModificationType) query.fetchOne();
  }

  /**
   * Costruisce o se presente aggiorna lo StampModificatinType. In caso di aggiornamento invalida la
   * cache.
   */
  public void saveStampType(String code, String value)
      throws NotImplementedException {
    throw new NotImplementedException();
  }

  /**
   * Preleva dalla cache lo stamp modifcation type.
   */
  public StampModificationType getStampMofificationType(
      StampModificationTypeCode code) {

    Preconditions.checkNotNull(code);

    String key = SMT_PREFIX + code;

    StampModificationType value = (StampModificationType) Cache.get(key);

    if (value == null) {

      value = getStampModificationTypeByCode(code);

      Preconditions.checkNotNull(value);

      Cache.set(key, value);
    }
    value.merge();
    return value;

  }

}
