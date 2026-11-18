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

package jobs;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.sync.personoffice.PersonOfficeSyncManager;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

/**
 * Interroga periodicamente il sistema esterno (ACE/Attestati) per le variazioni di afferenza dei
 * dipendenti e le applica in ePAS. Le variazioni già presenti vengono ignorate, quindi ogni
 * esecuzione può riconsiderare gli ultimi giorni (personoffice.sync.days, default 7).
 */
@Slf4j
@On("0 30 0 * * ?")
public class SyncPersonOffices extends Job<Void> {

  private static final String DAYS = "personoffice.sync.days";

  @Inject
  static PersonOfficeSyncManager personOfficeSyncManager;

  @Override
  public void doJob() {
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))
        || !personOfficeSyncManager.isEnabled()) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    final int days = Integer.parseInt(Play.configuration.getProperty(DAYS, "7"));
    try {
      log.info("Risultato sincronizzazione afferenze: {}",
          personOfficeSyncManager.sync(LocalDate.now().minusDays(days)));
    } catch (RuntimeException ex) {
      log.error("Errore nella sincronizzazione delle afferenze", ex);
    }
  }
}
