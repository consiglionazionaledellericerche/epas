import org.junit.Test;

import play.mvc.Http;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

import java.time.YearMonth;

/**
 * Classe di verifica di base dell'attivazione della parte di Security. 
 * 
 * @author cristian
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
      response = httpGet(redirectedTo,followRedirect);
    }
    return response;
  }
}
