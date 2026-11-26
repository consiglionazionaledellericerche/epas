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
import manager.ContractManager;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Limita alla data di fine i piani ferie dei contratti terminati che vanno oltre (per esempio
 * contratti a tempo indeterminato chiusi senza accorciare i piani ferie). Si può eseguire più
 * volte: dopo la prima correzione non trova più contratti da sistemare.
 */
@Slf4j
@OnApplicationStart(async = true)
public class VacationPeriodsFix extends Job<Void> {

  @Inject
  static ContractManager contractManager;

  @Override
  public void doJob() {
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Avviato il job per limitare i piani ferie alla fine dei contratti");
    int fixed = contractManager.fixVacationPeriodsAfterContractEnd();
    log.info("Corretti i piani ferie di {} contratti terminati", fixed);
  }
}
