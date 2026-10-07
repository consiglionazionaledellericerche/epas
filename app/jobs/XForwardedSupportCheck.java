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

import common.security.ClientAddress;
import lombok.extern.slf4j.Slf4j;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Segnala all'avvio una configurazione di XForwardedSupport che permette a qualunque client
 * di falsificare il proprio indirizzo ip tramite l'header X-Forwarded-For.
 */
@OnApplicationStart
@Slf4j
public class XForwardedSupportCheck extends Job<Void> {

  @Override
  public void doJob() {
    if (Play.mode.isProd() && Play.configuration.containsKey(ClientAddress.X_FORWARDED_SUPPORT)
        && ClientAddress.trustedProxies().isEmpty()) {
      log.warn("{}=all: qualunque client che raggiunga direttamente l'applicazione può "
          + "falsificare il proprio indirizzo ip (whitelist timbrature web). Impostare gli ip "
          + "dei reverse proxy in {} e non esporre direttamente la porta dell'applicazione.",
          ClientAddress.X_FORWARDED_SUPPORT, ClientAddress.X_FORWARDED_SUPPORT);
    }
  }
}
