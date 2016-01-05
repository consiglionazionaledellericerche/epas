package manager.cache;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import models.StampModificationType;
import models.StampModificationTypeCode;
import models.query.QStampModificationType;

import org.apache.commons.lang.NotImplementedException;

import play.cache.Cache;

import javax.persistence.EntityManager;

public class StampTypeManager {

  protected final JPQLQueryFactory queryFactory;
  private final String SMT_PREFIX = "smt";

  @Inject
  StampTypeManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.queryFactory = new JPAQueryFactory(emp);
  }

  /**
   * @return lo stampModificationType relativo al codice code passato come parametro
   */
  private StampModificationType getStampModificationTypeByCode(
          StampModificationTypeCode smtCode) {

    Preconditions.checkNotNull(smtCode);

    final QStampModificationType smt = QStampModificationType.stampModificationType;

    JPQLQuery query = queryFactory.from(smt)
            .where(smt.code.eq(smtCode.getCode()));

    return query.singleResult(smt);
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
