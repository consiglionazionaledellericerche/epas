/*
 * Copyright (C) 2022  Consiglio Nazionale delle Ricerche
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

import dao.ContractDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ContractManager;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@Slf4j
//@OnApplicationStart(async = true)
public class ContractFixerJob  extends Job<Void>{
  
  @Inject
  static ContractDao contractDao;
  @Inject
  static ContractManager contractManager;

  @Override
  public void doJob() {
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      //return;
    }
    contractDao.getContractWithWrongPreviousContract().stream().forEach(contract -> {
      contractManager.fixPreviousContractLink(contract);
    });
  }
}