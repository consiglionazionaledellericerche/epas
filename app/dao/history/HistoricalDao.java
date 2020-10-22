package dao.history;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.querydsl.jpa.JPQLQueryFactory;
import helpers.jpa.HistoryViews;
import injection.StaticInject;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import models.User;
import models.base.BaseModel;
import models.base.Revision;
import models.base.query.QRevision;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.joda.time.LocalDateTime;

@StaticInject
@Slf4j
public class HistoricalDao {

  @Inject
  private static Provider<AuditReader> auditReader;
  @Inject
  private static JPQLQueryFactory queryFactory;
  @Inject
  private static Provider<EntityManager> emp;

  /**
   * @param id the id to search.
   * @return the Revision object.
   */
  public static Revision getRevision(int id) {
    return Verify.verifyNotNull(queryFactory.selectFrom(QRevision.revision)
        .where(QRevision.revision.id.eq(id))
        .fetchOne());
  }


  /**
   * @param cls Entity Class to search
   * @param id the entity primary key
   * @param revisionId the revision id
   * @return the entity instance at the specified revision.
   */
  public static <T extends BaseModel> T valueAtRevision(Class<T> cls, long id, int revisionId) {

    final T current = Verify.verifyNotNull(emp.get().find(cls, id));
    final T history = cls.cast(auditReader.get().createQuery()
        .forEntitiesAtRevision(cls, revisionId)
        .add(AuditEntity.id().eq(current.id))
        .getSingleResult());
    final LocalDateTime date = getRevision(revisionId).getRevisionDate();
    return HistoryViews.historicalViewOf(cls, current, history, date);
  }

  @SuppressWarnings("rawtypes")
  public static HistoryValue lastRevisionOf(Class<? extends BaseModel> cls, long id) {
    List<HistoryValue> lastRevisions = lastRevisionsOf(cls, id);
    if (lastRevisions.isEmpty()) {
      return null;
    }
    return lastRevisions.get(0);
  }

  /**
   * @param cls Entity Class to search
   * @param id the entity primary key
   * @return List of revisions for the specified entity instance.
   */
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
   * @param cls Entity Class to search
   * @param id the entity primary key
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

  public static User lastRevisionOperator(BaseModel entity) {
    return lastRevisionOf(entity.getClass(), entity.id).revision.owner;
  }

}
