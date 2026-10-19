/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package helpers.jpa;

import java.util.function.Function;
import javax.persistence.EntityManager;
import play.db.jpa.JPA;

/**
 * Lettura dei valori salvati nel database di un'entità, indipendentemente dalle modifiche non
 * ancora salvate nel contesto di persistenza corrente.
 *
 * <p>Serve per i controlli dei permessi sulle entità ricevute dai form: il binder JPA di play
 * carica l'entità esistente dall'id e la modifica con i parametri della richiesta, quindi
 * l'entità del contesto corrente non dice più a chi apparteneva il record.</p>
 */
public final class PersistedValues {

  private PersistedValues() {
  }

  /**
   * Legge un valore dall'entità salvata nel database, con un entity manager separato.
   *
   * @param entityClass la classe dell'entità
   * @param id l'identificativo dell'entità
   * @param reader la funzione che estrae il valore dall'entità salvata
   * @return il valore estratto, null se l'entità non esiste nel database
   */
  public static <E, R> R read(Class<E> entityClass, Object id, Function<E, R> reader) {
    final EntityManager em = JPA.newEntityManager();
    try {
      final E entity = em.find(entityClass, id);
      return entity == null ? null : reader.apply(entity);
    } finally {
      em.close();
    }
  }
}
