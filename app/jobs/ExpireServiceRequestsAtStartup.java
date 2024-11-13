/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dao.PersonDao;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.flows.InformationRequestManager;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Chiusura degli uffici senza persone con contratto attivo.
 */
@Slf4j
@OnApplicationStart(async = true)
public class ExpireServiceRequestsAtStartup extends Job<Void> {

  @Inject
  static InformationRequestManager irManager;
  
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    val expired = irManager.expireServiceRequests();
    log.info("Sono state chiuse come expired {} richieste di uscita di servizio.", expired.size());
  }
}
