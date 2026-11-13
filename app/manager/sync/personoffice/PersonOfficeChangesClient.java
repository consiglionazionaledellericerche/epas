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

package manager.sync.personoffice;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import play.Play;
import play.libs.WS;

/**
 * Client del servizio REST esterno che espone le variazioni di afferenza dei dipendenti.
 * Configurazione in application.conf:
 * <pre>
 * personoffice.sync.enabled = true
 * personoffice.sync.url     = https://host/api/afferenze
 * personoffice.sync.user    = ...
 * personoffice.sync.password = ...
 * </pre>
 * Il servizio viene interrogato con il parametro since=yyyy-MM-dd e deve restituire un array
 * JSON di {@link PersonOfficeChange}.
 */
@Slf4j
public class PersonOfficeChangesClient {

  public static final String ENABLED = "personoffice.sync.enabled";
  public static final String URL = "personoffice.sync.url";
  public static final String USER = "personoffice.sync.user";
  public static final String PASSWORD = "personoffice.sync.password";

  /**
   * True se la sincronizzazione è abilitata e configurata.
   */
  public boolean isEnabled() {
    return "true".equals(Play.configuration.getProperty(ENABLED))
        && !Strings.isNullOrEmpty(Play.configuration.getProperty(URL));
  }

  /**
   * Le variazioni di afferenza registrate dal sistema esterno a partire dalla data indicata.
   */
  public List<PersonOfficeChange> changesSince(LocalDate since) {
    final String url = Play.configuration.getProperty(URL);
    WS.WSRequest request = WS.url(url).setParameter("since", since.toString("yyyy-MM-dd"));
    final String user = Play.configuration.getProperty(USER);
    if (!Strings.isNullOrEmpty(user)) {
      request = request.authenticate(user, Play.configuration.getProperty(PASSWORD));
    }
    log.info("Richiesta variazioni di afferenza dal {} a {}", since, url);
    final WS.HttpResponse response = request.get();
    if (!response.success()) {
      throw new IllegalStateException(String.format(
          "Errore nella risposta del servizio delle afferenze: %s %s",
          response.getStatus(), response.getStatusText()));
    }
    try {
      return Arrays.asList(new Gson().fromJson(response.getString(), PersonOfficeChange[].class));
    } catch (JsonSyntaxException ex) {
      throw new IllegalStateException("Risposta del servizio delle afferenze non valida", ex);
    }
  }
}
