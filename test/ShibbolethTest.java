
import java.util.ArrayList;
import java.util.List;

import models.Person;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.*;
import org.junit.Before;

import controllers.shib.MockShibboleth;
import controllers.shib.Shibboleth;
import play.Logger;
import play.test.*;
import play.db.jpa.JPA;
import play.mvc.*;
import play.mvc.Http.*;

public class ShibbolethTest extends FunctionalTest {


	@Before
	public void loadModels() {
    	Fixtures.loadModels("persons.yml");
	}
	/**
	 * The basic test to authenticate as a user using Shibboleth.
	 */
    @Test
    public void testShibbolethAuthentication() {
    	assertThat(Person.find("SELECT p FROM Person p where email = ?" , "cristian.lucchesi@cnr.it"), IsNull.notNullValue());
    	// Set up the mock shibboleth attributes that
    	// will be used to authenticate the next user which 
    	// logins in.
    	MockShibboleth.removeAll();
    	MockShibboleth.set("eppn","cristian.lucchesi@cnr.it");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertIsOk(response);
        Logger.info("response = %s", response);
        assertContentType("text/html", response);
        assertTrue(response.cookies.get("PLAY_SESSION").value
                .contains("cristian.lucchesi"));
        
    }
    
    @AfterClass
    public static void cleanup() {
    	MockShibboleth.reload();
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