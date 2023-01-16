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
import models.absences.Absence;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;


/**
 * Dao sulle tabelle di storico.
 *
 * @author Marco Andreini
 */
public class AbsenceHistoryDao {

  private final Provider<AuditReader> auditReader;

  @Inject
  AbsenceHistoryDao(Provider<AuditReader> auditReader) {
    this.auditReader = auditReader;
  }

  /**
   * Lista delle revisioni sull'assenza con identificativo absenceId.
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