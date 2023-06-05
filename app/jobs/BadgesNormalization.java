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

import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.BadgeManager;
import models.Badge;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Normalizza eventuali numeri di bagde non corretti.
 *
 * @author Daniele Murgia
 * @since 30/06/16.
 */
@Slf4j
@OnApplicationStart(async = true)
public class BadgesNormalization extends Job<Void> {

  @Inject
  static BadgeManager badgeManager;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    // Rimuove gli zeri iniziali se il codice badge è un intero.
    List<Badge> badges = Badge.findAll();
    for (Badge badge : badges) {
      badgeManager.normalizeBadgeCode(badge, true);
    }
    log.debug("Terminata normalizzazione Badges");
  }
}
