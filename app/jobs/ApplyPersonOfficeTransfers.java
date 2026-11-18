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
import manager.PersonOfficeManager;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

/**
 * Rende efficaci i trasferimenti di sede inseriti con data di decorrenza futura: allinea il ruolo
 * di dipendente alla nuova sede e notifica il cambio di sede.
 */
@Slf4j
@On("0 5 0 * * ?")
public class ApplyPersonOfficeTransfers extends Job<Void> {

  @Inject
  static PersonOfficeManager personOfficeManager;

  @Override
  public void doJob() {
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    final int applied = personOfficeManager.applyTransfersStartingAt(LocalDate.now());
    log.info("Applicati {} trasferimenti di sede con decorrenza odierna", applied);
  }
}
