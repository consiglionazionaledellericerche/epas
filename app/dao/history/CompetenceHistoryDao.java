package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import models.Competence;

public class CompetenceHistoryDao {

  private final Provider<AuditReader> auditReader;
  
  @Inject
  CompetenceHistoryDao(Provider<AuditReader> auditReader) {
    this.auditReader = auditReader;
  }
  
  /**
   * Metodo di storico per le modifiche sulla competenza.
   * @param competenceId l'id della competenza di cui recuperare lo storico
   * @return la lista di modifiche per la competenza in oggetto.
   */
  public List<HistoryValue<Competence>> competences(long competenceId) {
    
    final AuditQuery query = auditReader.get().createQuery()
        .forRevisionsOfEntity(Competence.class, false, true)
        .add(AuditEntity.id().eq(competenceId))
        .addOrder(AuditEntity.revisionNumber().asc());
    
    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(Competence.class))
        .toList();
  }
  
}
