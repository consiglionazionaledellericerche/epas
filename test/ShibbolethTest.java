import controllers.shib.MockShibboleth;
import dao.PersonDao;
import injection.StaticInject;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

@Slf4j
@StaticInject
public class ShibbolethTest extends FunctionalTest {

  private static final String DEFAULT_USER_NAME = "cristian.lucchesi";
  private static final String DEFAULT_USER_EMAIL = DEFAULT_USER_NAME + "@iit.cnr.it";
  
  @Inject
  public static PersonDao personDao; 
  
  /**
   * The basic test to authenticate as a user using Shibboleth.
   */
  @Test
  public void testShibbolethAuthentication() {
    
    val defaultPerson = personDao.byEmail(DEFAULT_USER_EMAIL);
    assertThat(defaultPerson.isPresent(), Is.is(true));
    // Set up the mock shibboleth attributes that
    // will be used to authenticate the next user which
    // logins in.
    MockShibboleth.removeAll();
    MockShibboleth.set("eppn",DEFAULT_USER_EMAIL);

    final String loginUrl = Router.reverse("shib.Shibboleth.login").url;
    Response response = httpGet(loginUrl,true);
    assertIsOk(response);
    log.debug("response.contentType = {}", response.contentType);
    assertContentType("text/html", response);
    assertTrue(response.cookies.get("PLAY_SESSION").value.contains(DEFAULT_USER_NAME));

  }

  @AfterClass
  public static void cleanup() {
    MockShibboleth.reload();
  }


  /**
   * Fixed a bug in the default version of this method. It dosn't follow redirects properly.
   */
  public static Response httpGet(Object url, boolean followRedirect) {
    Response response = GET(url);
    if (Http.StatusCode.FOUND == response.status && followRedirect) {
      String redirectedTo = response.getHeader("Location");
      response = httpGet(redirectedTo,followRedirect);
    }
    return response;
  }
}
