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

import common.injection.StaticInject;
import controllers.shib.MockShibboleth;
import dao.PersonDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.Test;
import play.Play;
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
    MockShibboleth.set("eppn", DEFAULT_USER_EMAIL);
    Play.configuration.setProperty("shib.login", "true");

    final String loginUrl = Router.reverse("shib.Shibboleth.login").url;
    Response response = httpGet(loginUrl, true);
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
      response = httpGet(redirectedTo, followRedirect);
    }
    return response;
  }
}
