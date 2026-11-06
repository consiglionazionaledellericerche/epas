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

import com.google.common.base.Optional;
import common.security.ApplicationSecret;
import lombok.extern.slf4j.Slf4j;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Impedisce l'avvio in produzione senza un application.secret specifico dell'installazione.
 *
 * <p>Con un secret pubblico chiunque può firmare un cookie di sessione valido per qualunque
 * utente, compreso l'amministratore: meglio non avviare l'applicazione che avviarla così.</p>
 */
@OnApplicationStart
@Slf4j
public class ApplicationSecretCheck extends Job<Void> {

  @Override
  public void doJob() {
    if (!Play.mode.isProd()) {
      return;
    }
    final Optional<String> problem = ApplicationSecret.problem(Play.secretKey);
    if (problem.isPresent()) {
      final String message = String.format("%s: impostare un application.secret casuale di "
          + "almeno %d caratteri specifico di questa installazione (APPLICATION_SECRET nel "
          + "docker, ad esempio generato con: tr -dc A-Za-z0-9 </dev/urandom | head -c 64). "
          + "Avvio interrotto.", problem.get(), ApplicationSecret.MIN_LENGTH);
      log.error(message);
      throw new IllegalStateException(message);
    }
  }
}
