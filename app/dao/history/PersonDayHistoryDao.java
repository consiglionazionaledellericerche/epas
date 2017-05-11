package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.List;

import models.Stamping;
import models.absences.Absence;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

/**
 * Dao per lo storico dei PersonDay.
 *
 * @author marco
 */
public class PersonDayHistoryDao {

  private final Provider<AuditReader> auditReader;

  @Inject
  PersonDayHistoryDao(Provider<AuditReader> auditReader) {
    this.auditReader = auditReader;
  }

  @SuppressWarnings("unchecked")
  public List<HistoryValue<Stamping>> stampings(long personDayId) {
    final AuditQuery query = auditReader.get().createQuery()
            .forRevisionsOfEntity(Stamping.class, false, true)
            .add(AuditEntity.relatedId("personDay").eq(personDayId))
            .addOrder(AuditEntity.property("id").asc())
            .addOrder(AuditEntity.revisionNumber().asc());

    return FluentIterable.from(query.getResultList())
            .transform(HistoryValue.fromTuple(Stamping.class))
            .toList();
  }

  @SuppressWarnings("unchecked")
  public List<HistoryValue<Absence>> absences(long personDayId) {
    final AuditQuery query = auditReader.get().createQuery()
            .forRevisionsOfEntity(Absence.class, false, true)
            .add(AuditEntity.relatedId("personDay").eq(personDayId))
            .addOrder(AuditEntity.property("id").asc())
            .addOrder(AuditEntity.revisionNumber().asc());

    return FluentIterable.from(query.getResultList())
            .transform(HistoryValue.fromTuple(Absence.class))
            .toList();
  }
  
}
