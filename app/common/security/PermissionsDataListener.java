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

package common.security;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.transaction.Synchronization;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import play.db.jpa.JPA;

/**
 * Invalida gli esiti condivisi dei controlli dei permessi ({@link SharedChecksCache}) quando
 * cambia un'entità letta dalle regole senza target: persona (qualifica, sede), utente, sede,
 * configurazioni della sede e della persona, impostazioni generali, categorie di turno,
 * turnisti, tipi di reperibilità e reperibilità.
 *
 * <p>Le relazioni molti-a-molti (es. i responsabili di una categoria di turno) appartengono
 * all'entità che ne è proprietaria: una loro modifica ne incrementa la versione e passa da
 * {@code @PostUpdate}.</p>
 *
 * <p>Dentro una transazione l'invalidazione avviene dopo la sua conclusione: prima del commit
 * le altre richieste leggono ancora i dati precedenti, e un esito calcolato con quei dati
 * resterebbe valido per tutta la durata della cache. Gli aggiornamenti massivi via query
 * (update/delete HQL) non passano dai listener JPA.</p>
 */
@Slf4j
public class PermissionsDataListener {

  @PostPersist
  @PostUpdate
  @PostRemove
  void onChange(Object entity) {
    log.trace("Modificata {}, invalido gli esiti condivisi dei permessi", entity);
    invalidateAfterTransaction();
  }

  /**
   * Invalida gli esiti condivisi alla fine della transazione corrente, o subito se non ce n'è
   * una attiva. Più modifiche nella stessa transazione registrano più invalidazioni: costano
   * solo un incremento ciascuna.
   */
  static void invalidateAfterTransaction() {
    if (JPA.isInsideTransaction() && JPA.em().getTransaction().isActive()) {
      SharedChecksCache.markPendingChanges();
      try {
        JPA.em().unwrap(Session.class).getTransaction()
            .registerSynchronization(new Synchronization() {
              @Override
              public void beforeCompletion() {
                // niente da fare prima del commit
              }

              @Override
              public void afterCompletion(int status) {
                // anche dopo un rollback: invalidare non è mai sbagliato
                SharedChecksCache.invalidateAll();
              }
            });
      } catch (RuntimeException ex) {
        log.warn("Impossibile invalidare dopo il commit, invalido subito", ex);
        SharedChecksCache.invalidateAll();
      }
      return;
    }
    SharedChecksCache.invalidateAll();
  }
}
