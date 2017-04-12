package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.List;

import models.absences.Absence;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;


/**
 * @author marco.
 */
public class AbsenceHistoryDao {

  private final Provider<AuditReader> auditReader;

  @Inject
  AbsenceHistoryDao(Provider<AuditReader> auditReader) {
    this.auditReader = auditReader;
  }

  /**
   *
   * @param absenceId id dell'assenza della quale recuperare lo storico
   * @return La lista delle revisioni relative all'assenza specificata.
   */
  @SuppressWarnings("unchecked")
  public List<HistoryValue<Absence>> absences(long absenceId) {

    final AuditQuery query = auditReader.get().createQuery()
            .forRevisionsOfEntity(Absence.class, false, true)
            .add(AuditEntity.id().eq(absenceId))
            .addOrder(AuditEntity.revisionNumber().asc());

    return FluentIterable.from(query.getResultList())
            .transform(HistoryValue.fromTuple(Absence.class))
            .toList();
  }


}
