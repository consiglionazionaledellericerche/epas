package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.StampModificationType;
import models.Stamping;
import models.query.QStampModificationType;
import models.query.QStamping;

import javax.persistence.EntityManager;

/**
 * @author dario.
 */
public class StampingDao extends DaoBase {

  @Inject
  StampingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @param id l'id associato alla Timbratura sul db.
   * @return la timbratura corrispondente all'id passato come parametro.
   */
  public Stamping getStampingById(Long id) {
    final QStamping stamping = QStamping.stamping;
    final JPQLQuery query = getQueryFactory().from(stamping)
        .where(stamping.id.eq(id));
    return query.singleResult(stamping);
  }

  /**
   * * @return lo stampModificationType relativo all'id passato come parametro.
   */
  @Deprecated
  public StampModificationType getStampModificationTypeById(Long id) {
    final QStampModificationType smt = QStampModificationType.stampModificationType;

    JPQLQuery query = getQueryFactory().from(smt)
        .where(smt.id.eq(id));
    return query.singleResult(smt);
  }

}
