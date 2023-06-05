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

package jobs;

import com.google.common.base.Optional;
import dao.ContractDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ContractManager;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Corregge i contratti che hanno impostato un contratto precedente errato.
 */
@Slf4j
@OnApplicationStart(async = true)
public class ContractFixerJob  extends Job<Void> {
  
  @Inject
  static ContractDao contractDao;
  @Inject
  static ContractManager contractManager;

  @Override
  public void doJob() {
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Avviato il job per controllare i contratti da verificare e correggere "
        + "per il previousContract");
    int fixedContract = contractManager.fixContractsWithWrongPreviousContract(Optional.absent());
    log.info("Corretti {} contratti con previousContract errato", fixedContract);
  }
}