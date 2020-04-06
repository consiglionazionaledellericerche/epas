package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;
import models.Stamping;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

/**
 * Dao sullo storico delle timbrature.
 * 
 * @author marco
 */
public class StampingHistoryDao {

  private final Provider<AuditReader> auditReader;

  @Inject
  StampingHistoryDao(Provider<AuditReader> auditReader) {
    this.auditReader = auditReader;
  }

  /**
   * La lista delle revisioni sulla timbratura con id passato.
   * @param stampingId l'identificativo della timbratura
   * @return la lista delle revisioni sulla timbratura con id passato.
   */
  @SuppressWarnings("unchecked")
  public List<HistoryValue<Stamping>> stampings(long stampingId) {

    final AuditQuery query = auditReader.get().createQuery()
            .forRevisionsOfEntity(Stamping.class, false, true)
            .add(AuditEntity.id().eq(stampingId))
            .addOrder(AuditEntity.revisionNumber().asc());

    return FluentIterable.from(query.getResultList())
            .transform(HistoryValue.fromTuple(Stamping.class))
            .toList();
  }

}
