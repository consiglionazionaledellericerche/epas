package dao.history;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.joda.time.LocalDateTime;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.mysema.query.jpa.JPQLQueryFactory;

import helpers.jpa.HistoryViews;
import injection.StaticInject;
import lombok.extern.slf4j.Slf4j;
import models.base.BaseModel;
import models.base.Revision;
import models.base.query.QRevision;

@StaticInject
@Slf4j
public class HistoricalDao {

  @Inject
  private static Provider<AuditReader> auditReader;
  @Inject
  private static JPQLQueryFactory queryFactory;
  @Inject
  private static Provider<EntityManager> emp;

  public static Revision getRevision(int id) {
    return Verify.verifyNotNull(queryFactory.from(QRevision.revision)
        .where(QRevision.revision.id.eq(id))
        .singleResult(QRevision.revision));
  }

  public static <T extends BaseModel> T valueAtRevision(Class<T> cls, long id,
                                                 int revisionId) {

    final T current = Verify.verifyNotNull(emp.get().find(cls, id));
    final T history = cls.cast(auditReader.get().createQuery()
        .forEntitiesAtRevision(cls, revisionId)
        .add(AuditEntity.id().eq(current.id))
        .getSingleResult());
    final LocalDateTime date = getRevision(revisionId).getRevisionDate();
    return HistoryViews.historicalViewOf(cls, current, history, date);
  }

  public static HistoryValue lastRevisionOf(Class<? extends BaseModel> cls, int id) {
    List<HistoryValue> lastRevisions = lastRevisionsOf(cls, id);
    if (lastRevisions.isEmpty()) {
      return null;
    }
    return lastRevisions.get(0);
  }

  @SuppressWarnings("unchecked")
  public static List<HistoryValue> lastRevisionsOf(Class<? extends BaseModel> cls, long id) {
    return FluentIterable.from(auditReader.get().createQuery()
        .forRevisionsOfEntity(cls, false, true)
        .add(AuditEntity.id().eq(id))
        .addOrder(AuditEntity.revisionNumber().desc())
        .setMaxResults(100)
        .getResultList()).transform(HistoryValue.fromTuple(cls)).toList();
  }

  
  /**
   * @return la versione precedente del istanza individuata da cls e id.
   */
  public static <T extends BaseModel> T previousRevisionOf(Class<T> cls, long id) {
    final Integer currentRevision = (Integer) auditReader.get().createQuery()
        .forRevisionsOfEntity(cls, false, true)
        .add(AuditEntity.id().eq(id))
        .addProjection(AuditEntity.revisionNumber().max())
        .getSingleResult();
    final Integer previousRevision = (Integer) auditReader.get().createQuery()
        .forRevisionsOfEntity(cls, false, true)
        .addProjection(AuditEntity.revisionNumber().max())
        .add(AuditEntity.id().eq(id))
        .add(AuditEntity.revisionNumber().lt(currentRevision))
        .getSingleResult();
    log.debug("current-revision {} of ({}:{}), previous-revision: {}",
        currentRevision, cls, id, previousRevision);
    return valueAtRevision(cls, id, previousRevision);
  }

}
