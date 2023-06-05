/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.inject.Provider;
import java.util.List;
import javax.inject.Inject;
import models.User;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

/**
 * DAO per la UserHistory.
 */
public class UserHistoryDao {

  private final Provider<AuditReader> auditReader;

  @Inject
  UserHistoryDao(Provider<AuditReader> auditReader) {
    this.auditReader = auditReader;
  }
  
  /**
   * La lista delle revisioni di un utente.
   *
   * @param userId l'identificativo dell'utente
   * @return la lista delle revisioni sulla modifica di un utente.
   */
  @SuppressWarnings("unchecked")
  public List<HistoryValue<User>> historyUser(long userId) {
    final AuditQuery query = auditReader.get().createQuery()
            .forRevisionsOfEntity(User.class, false, true)
            .add(AuditEntity.id().eq(userId));           

    return FluentIterable.from(query.getResultList())
            .transform(HistoryValue.fromTuple(User.class))
            .toList();
  }
}
