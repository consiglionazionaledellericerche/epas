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
import manager.NotificationManager;
import manager.flows.InformationRequestManager;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;

/**
 * Impostata scadenza di lettura per le notifiche più vecchi di 3 mesi.
 */
@Slf4j
@On("0 10 7 ? * MON-FRI") //tutti i giorni dal lunedi al venerdi di ogni mese alle 7.15
public class ExpireNotifications extends Job<Void> {

  @Inject
  static NotificationManager notificationManager;
  
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    val expired = notificationManager.expiredNotificationsNotRead();
    log.info("Sono state impostate come lette {} notifiche.", expired);
  }
}
