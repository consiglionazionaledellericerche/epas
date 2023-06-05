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

import java.time.YearMonth;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Classe di verifica di base dell'attivazione della parte di Security. 
 *
 * @author Cristian Lucchesi
 *
 */
public class SecurityTest extends FunctionalTest {

  @Test
  public void testThatIndexPageNeedsLogin() {
    Response response = GET("/");
    assertStatus(302, response);
    assertLocationRedirect("/login", response);
  }

  @Test
  public void testThatUserCanLogin() {
    loginAs("cristian.lucchesi", "epas2016");
    Response responseRedirect = GET("/");
    assertStatus(302, responseRedirect);

    YearMonth ym = YearMonth.now();
    //ex. stampings/stampings?month=4&year=2016
    String urlSituazioneMensile =
        String.format(
            "/stampings/stampings?month=%d&year=%d",
            ym.getMonthValue(), ym.getYear());

    assertLocationRedirect(
        urlSituazioneMensile, responseRedirect);

    Response responsePresente = httpGet("/", true);
    assertContentMatch("ePAS - Timbrature", responsePresente);
    assertContentMatch("cristian.lucchesi", responsePresente);

  }

  private void assertLocationRedirect(String location, Response resp) {
    assertHeaderEquals("Location", location, resp);
  }

  private void loginAs(String user, String password) {
    POST("/login?username=" + user + "&password=" + password);
  }

  /** Fixed a bug in the default version of this method. It dosn't follow redirects properly **/
  public static Response httpGet(Object url, boolean followRedirect) {
    Response response = GET(url);
    if (Http.StatusCode.FOUND == response.status && followRedirect) {
      String redirectedTo = response.getHeader("Location");
      response = httpGet(redirectedTo, followRedirect);
    }
    return response;
  }
}
