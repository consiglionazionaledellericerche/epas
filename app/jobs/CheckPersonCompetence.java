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

import dao.CompetenceCodeDao;
import dao.PersonDao;
import dao.PersonShiftDayDao;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.CompetenceCode;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonShift;
import models.dto.PersonCompetenceCodeDto;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Job per le associazioni tra persone e competenze.
 *
 * @author dario
 *
 */
@Slf4j
@OnApplicationStart(async = true)
public class CheckPersonCompetence extends Job<Void> {

  @Inject
  static CompetenceCodeDao competenceCodeDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static PersonShiftDayDao shiftDao;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Lanciata procedura di check dei periodi di competenza abilitati per persona.");
    
    log.info("Rimuovo periodi con date sballate");
    List<PersonCompetenceCodes> wrongPcc = competenceCodeDao.getWrongs();
    int size = wrongPcc.size();
    wrongPcc.forEach(pcc -> pcc.delete());
    log.info("Eliminati {} periodi con date sballate", size);
    

    List<PersonCompetenceCodeDto> list = competenceCodeDao.getDuplicates();
    log.info("Trovate {} pcc con date sovrapposte", list.size());
    
    
    for (PersonCompetenceCodeDto dto : list) {
      Person person = personDao.getPersonById(dto.personId);
      CompetenceCode code = competenceCodeDao.getCompetenceCodeById(dto.competenceCodeId);
      List<PersonCompetenceCodes> subList = competenceCodeDao.listByPersonAndCode(person, code);

      log.debug("Trovati {} periodi", subList.size());
      //List<PersonCompetenceCodes> toDelete = Lists.newArrayList();
      PersonCompetenceCodes temp = null;
      for (PersonCompetenceCodes pcc : subList) {

        if (temp == null) {
          temp = pcc;
          continue;
        }
        DateInterval interval = new DateInterval(pcc.getBeginDate(), pcc.getEndDate());
        if (DateUtility.isIntervalIntoAnother(interval, 
            new DateInterval(temp.getBeginDate(), temp.getEndDate()))) {
          //toDelete.add(pcc); 
          pcc.delete();
          log.debug("Cancellato {}", pcc.toString());

        }
        if (DateUtility.isIntervalIntoAnother(
              new DateInterval(temp.getBeginDate(), temp.getEndDate()), 
            interval)) {
          //toDelete.add(prev);
          temp.delete();
          log.debug("Cancellato {}", temp.toString());
          temp = pcc;

        }
        //log.debug("Accorpati personCompetenceCodes {} e {}", prev.toString(), pcc.toString());
      }
      //toDelete.forEach(pcc -> pcc.delete());
    }
    
    List<PersonShift> wrongDisabled = shiftDao.getWrongDisabled();
    wrongDisabled.forEach(ps -> {
      ps.setDisabled(false);
      ps.save();
    });
    log.info("Sistemati i dipendenti erroneamente disabilitati.");
    log.info("Procedura terminata");

  }
}
