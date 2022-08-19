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
import dao.CompetenceCodeDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.CompetenceManager;
import models.CompetenceCode;
import models.enumerate.LimitType;
import org.joda.time.YearMonth;
import play.Play;
import play.jobs.Job;
import play.jobs.On;


/**
 * Job che assegna le quantità per le competenze a presenza mensile.
 *
 * @author dario
 *
 */
@SuppressWarnings("rawtypes")
@Slf4j
@On("0 0 7 1-3 * ?") //il primi 5 giorni di ogni mese alle 7.00
//@On("0 15 17 * * ?") //test
public class BonusJob extends Job {
  
  @Inject
  static CompetenceManager competenceManager;
  @Inject
  static CompetenceCodeDao competenceCodeDao;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.debug("Start Job bonus");

    
    YearMonth yearMonth = YearMonth.now().minusMonths(1);
    List<CompetenceCode> codeList = competenceCodeDao
        .getCompetenceCodeByLimitType(LimitType.onMonthlyPresence);
    codeList.forEach(item -> {
      competenceManager.applyBonus(Optional.absent(), item, yearMonth);
    });
    codeList = competenceCodeDao.getCompetenceCodeByLimitType(LimitType.entireMonth);
    codeList.forEach(item -> {
      competenceManager.applyBonus(Optional.absent(), item, yearMonth);
    });
    

    log.debug("End Job bonus");
  }
}
