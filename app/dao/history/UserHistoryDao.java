package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;
import models.User;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

public class UserHistoryDao {

  private final Provider<AuditReader> auditReader;

  @Inject
  UserHistoryDao(Provider<AuditReader> auditReader) {
    this.auditReader = auditReader;
  }
  
  /**
   * La lista delle revisioni di un utente.
   * @param userId l'identificativo dell'utente
   * @return la lista delle revisioni sulla modifica di un utente.
   */
  public List<HistoryValue<User>> historyUser(long userId) {
    final AuditQuery query = auditReader.get().createQuery()
            .forRevisionsOfEntity(User.class, false, true)
            .add(AuditEntity.id().eq(userId));           

    return FluentIterable.from(query.getResultList())
            .transform(HistoryValue.fromTuple(User.class))
            .toList();
  }
}
