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

import dao.JwtTokenDao;
import dao.OfficeDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.CheckGreenPassManager;
import manager.EmailManager;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

/**
 * Job per la pulizia del token scaduti.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@On("0 15 6 ? * *") //tutti i giorni alle 06.15
public class JwtTokenCleanerJob extends Job<Void> {

  @Inject
  static JwtTokenDao jwtTokenDao;

  @Override
  public void doJob() {
    
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    log.info("Start Jwt Token Cleaner Job");
    val expiredTokens = jwtTokenDao.expiredTokens();
    log.debug("Presenti {} expired tokens da cancellare", expiredTokens.size());
    expiredTokens.forEach(jwtTokenDao::delete);
    log.debug("Cancellati {} expired tokens", expiredTokens.size());
    log.info("End Jwt Token Cleaner Job");
  }
}