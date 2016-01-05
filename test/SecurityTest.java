import org.junit.Test;

import play.mvc.Http;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
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
    loginAs("cristian.lucchesi", "epas2014");
    Response responseRedirect = GET("/");
    assertStatus(302, responseRedirect);
    assertLocationRedirect("/presenze", responseRedirect);

    Response responsePresente = GET("/", true);
    assertContentMatch("Amministrazione del personale", responsePresente);
  }

  private void assertLocationRedirect(String location, Response resp) {
    assertHeaderEquals("Location", location, resp);
  }

  private void loginAs(String user, String password) {
    POST("/login?username=" + user + "&password=" + password);
  }

  /** Fixed a bug in the default version of this method. It dosn't follow redirects properly **/
  public static Response GET(Object url, boolean followRedirect) {
    Response response = GET(url);
    if (Http.StatusCode.FOUND == response.status && followRedirect) {
      String redirectedTo = response.getHeader("Location");
      response = GET(redirectedTo,followRedirect);
    }
    return response;
  }
}
