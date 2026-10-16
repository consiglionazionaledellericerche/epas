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

package controllers;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import models.exports.ReportData;
import org.junit.Test;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test sulle segnalazioni: l'invio resta disponibile senza autenticazione (pagina di login),
 * le risposte ai ticket OIL richiedono l'autenticazione e alle segnalazioni non vengono
 * allegati token e segreti della sessione.
 */
public class ReportCentreTest extends FunctionalTest {

  /**
   * Senza autenticazione le pagine di risposta ai ticket OIL rimandano al login.
   */
  @Test
  public void oilUserReplyRequiresAuthentication() {
    Map<String, Object> args = new HashMap<>();
    args.put("id", "1");
    args.put("azione", "risposta");
    Response response = GET(Router.reverse("ReportCentre.oilUserReply", args).url);
    assertStatus(302, response);
    assertTrue(response.getHeader("Location").contains("login"));

    response = POST(Router.reverse("ReportCentre.sendOilUserReplay").url,
        ImmutableMap.of("id", "1", "azione", "risposta", "description", "testo"));
    assertStatus(302, response);
    assertTrue(response.getHeader("Location").contains("login"));
  }

  /**
   * L'invio di una segnalazione resta possibile anche senza autenticazione.
   */
  @Test
  public void sendReportWorksWithoutAuthentication() {
    Response response = POST(Router.reverse("ReportCentre.sendReport").url, "application/json",
        "{\"note\": \"non riesco ad accedere\", \"url\": \"/login\", "
        + "\"html\": \"<html></html>\", \"img\": \"data:image/png;base64,AAAA\", "
        + "\"browser\": {\"userAgent\": \"test\"}}");
    assertIsOk(response);
  }

  /**
   * Dalla sessione allegata alle segnalazioni sono esclusi i token OAuth e le chiavi interne
   * di Play.
   */
  @Test
  public void sessionForReportExcludesTokens() {
    Map<String, String> session = new HashMap<>();
    session.put("username", "mario.rossi");
    session.put("___id_token", "id-token");
    session.put("___refresh_token", "refresh-token");
    session.put("___AT", "authenticity-token");

    assertEquals(ImmutableMap.of("username", "mario.rossi"),
        ReportData.sessionForReport(session));
  }
}
