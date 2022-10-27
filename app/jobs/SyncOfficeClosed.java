/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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
import dao.PersonDao;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
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
public class SyncOfficeClosed extends Job<Void> {

  @Inject
  static PersonDao personDao;
  
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    
    int count = 0;
    List<Office> list = Office.findAll();
    List<Office> offices = null;
    for (Office office : list) {
      if (office.getEndDate() != null) {
        continue;
      }
      log.debug("Analizzo la sede: {}", office.getName());
      offices = Lists.newArrayList();
      offices.add(office);
      List<Person> people = personDao.listFetched(Optional.<String>absent(),
          new HashSet<Office>(offices), false, LocalDate.now(), LocalDate.now(), true).list();
      if (people.isEmpty()
          //"Ufficio da cambiare" è l'ufficio predefinito creato al primo setup dell'applicazione
          && !office.getName().equalsIgnoreCase("Ufficio da cambiare")) {
        log.info("Non ci sono persone con contratto attivo sulla sede {}. "
            + "Inserisco la data di chiusura sede.", office.getName());
        office.setEndDate(LocalDate.now());
        office.save();
        count++;
      }
    }
    log.info("Sono state chiuse {} sedi.", count);
  }
}
