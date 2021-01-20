package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.apt.Configuration;
import java.util.List;
import models.Competence;
import models.Contract;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;


public class ContractHistoryDao {

  private final Provider<AuditReader> auditReader;

  @Inject
  ContractHistoryDao(Provider<AuditReader> auditReader) {
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
  
  /**
   * Metodo di storico per le modifiche sul contratto.
   * @param contractId l'id del contratto di cui recuperare lo storico
   * @return la lista di modifiche per il contratto in oggetto.
   */
  public List<HistoryValue<Contract>> contracts(long contractId) {
    
    final AuditQuery query = auditReader.get().createQuery()
        .forRevisionsOfEntity(Contract.class, false, true)
        .add(AuditEntity.id().eq(contractId))
        .addOrder(AuditEntity.revisionNumber().asc());
    
    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(Contract.class))
        .toList();
  }

}
