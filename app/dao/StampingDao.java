package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.StampModificationType;
import models.StampType;
import models.Stamping;
import models.query.QStampModificationType;
import models.query.QStampType;
import models.query.QStamping;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class StampingDao extends DaoBase {

  @Inject
  StampingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return la timbratura corrispondente all'id passato come parametro
   */
  public Stamping getStampingById(Long id) {
    final QStamping stamping = QStamping.stamping;
    final JPQLQuery query = getQueryFactory().from(stamping)
            .where(stamping.id.eq(id));
    return query.singleResult(stamping);
  }

  /**
   * //FIXME questo metodo è usato in un binder e ciò non è una buona cosa!!
   *
   * @return lo stampType corrispondente alla descrizione passata come parametro
   */
  @Deprecated
  public StampType getStampTypeByCode(String code) {
    QStampType stampType = QStampType.stampType;
    final JPQLQuery query = getQueryFactory().from(stampType)
            .where(stampType.code.eq(code));
    return query.singleResult(stampType);
  }

  /**
   * @return lo stampModificationType relativo all'id passato come parametro
   */
  @Deprecated
  public StampModificationType getStampModificationTypeById(Long id) {
    final QStampModificationType smt = QStampModificationType.stampModificationType;

    JPQLQuery query = getQueryFactory().from(smt)
            .where(smt.id.eq(id));
    return query.singleResult(smt);
  }

  /**
   * @return la lista di tutti gli stampType
   */
  public List<StampType> findAll() {
    final QStampType smt = QStampType.stampType;
    JPQLQuery query = getQueryFactory().from(smt).orderBy(smt.code.asc());
    return query.list(smt);
  }

  /**
   * @return lo stampType corrispondente all'id passato come parametro. Absent se non esiste uno
   * stampType con quell'id
   */
  public Optional<StampType> getStampTypeById(Long stampTypeId) {
    final QStampType smt = QStampType.stampType;
    JPQLQuery query = getQueryFactory().from(smt).where(smt.id.eq(stampTypeId));
    return Optional.fromNullable(query.singleResult(smt));
  }


}
