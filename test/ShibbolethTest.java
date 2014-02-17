
import java.util.ArrayList;
import java.util.List;

import models.Person;

import org.junit.*;
import org.junit.Before;

import controllers.shib.MockShibboleth;
import controllers.shib.Shibboleth;
import play.Logger;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;

public class ShibbolethTest extends FunctionalTest {

	
	/**
	 * The basic test to authenticate as a user using Shibboleth.
	 */
    @Test
    public void testShibbolethAuthentication() {
    	
    	// Set up the mock shibboleth attributes that
    	// will be used to authenticate the next user which 
    	// logins in.
    	MockShibboleth.removeAll();
    	MockShibboleth.set("SHIB_email","admin@localhost");
    	MockShibboleth.set("SHIB_givenName", "Admin");
    	MockShibboleth.set("SHIB_sn", "Admin");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertIsOk(response);
        Logger.info("response = %s", response);
        assertContentType("text/html", response);
//        assertContentMatch("<dt>email</dt>[\\s]*<dd>someone\\@your-domain\\.net</dd>", response);
//        assertContentMatch("<dt>firstName</dt>[\\s]*<dd>Some</dd>", response);
//        assertContentMatch("<dt>lastName</dt>[\\s]*<dd>One</dd>", response);
        
    }
    
    
    /**
	 * Test the shibboleth.check tag
	 */
    //@Test
    public void testShibbolethCheckTag() {
    	
    	// Check that the protected template text is not viewable when unauthenticated
    	MockShibboleth.removeAll();
    	final String INDEX_URL = Router.reverse("Application.index").url;
    	Response response = GET(INDEX_URL);
    	assertIsOk(response);
    	assertFalse(getContent(response).contains("This text is only be viewable by authenticated users as it is restricted within the template"));
    	
    	// Now Login and check that it is visible.
    	MockShibboleth.removeAll();
    	MockShibboleth.set("SHIB_email","admin@localhost");
    	MockShibboleth.set("SHIB_givenName", "Admin");
    	MockShibboleth.set("SHIB_sn", "Admin");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        response = GET(LOGIN_URL,true);
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch("This text is only be viewable by authenticated users as it is restricted within the template", response);
        
    }
    
    /** 
     * Test that visiting a restricted controller forces us to login, and then
     * we are redirected back to that controller after authenticating
     */
    //@Test
    public void testRestrictedController() {
    	
    	MockShibboleth.removeAll();
    	MockShibboleth.set("SHIB_email","bob@gmail.com");
    	MockShibboleth.set("SHIB_givenName", "Bob");
    	MockShibboleth.set("SHIB_sn", "Smith");
    	
    	final String RESTRICTED_URL = Router.reverse("Administrative.restricted").url;
    	Response response = GET(RESTRICTED_URL, true);
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch("Welcome Bob, you are viewing a restricted page",response);
    	
    }
    
    
    /**
     * If no attributes are received when by the application then an error should
     * result.
     */
    //@Test 
    public void testNoAttributes() {
    	
    	MockShibboleth.removeAll();

    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertStatus(500, response);
    }
    
    /**
     * Only some attributes are received, but not the full expected payload.
     */
    //@Test
    public void testMinimumAttributes() {
    	
    	MockShibboleth.removeAll();
    	MockShibboleth.set("SHIB_email","someone@your-domain.net");
    	MockShibboleth.set("SHIB_givenName","bob");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch("<dt>email</dt>[\\s]*<dd>someone\\@your-domain\\.net</dd>", response);
        assertContentMatch("<dt>firstName</dt>[\\s]*<dd>bob</dd>", response);
    	
    }
    
    /**
     * Only some attributes are received, but not the full expected payload.
     */
    //@Test
    public void testMultipleValues() {
    	
    	MockShibboleth.removeAll();
    	List<String> headerValues = new ArrayList<String>();
    	headerValues.add("bob@gmail.com");
    	headerValues.add("someone@your-domain.net");
    	Header header = new Header("SHIB_email",headerValues);
    	MockShibboleth.headers.put("SHIB_email", header);
    	MockShibboleth.set("SHIB_givenName", "Bob");
    	MockShibboleth.set("SHIB_sn", "Smith");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch("<dt>email</dt>[\\s]*<dd>bob\\@gmail\\.com</dd>", response);
        assertContentMatch("<dt>firstName</dt>[\\s]*<dd>Bob</dd>", response);
        assertContentMatch("<dt>lastName</dt>[\\s]*<dd>Smith</dd>", response);
    }
    
    /** 
     * Test handling of blank attributes. This is how the shibboleth SP will send
     * attributes which do not exist. They should be recognized as not existing.
     */
    //@Test
    public void testBlankAttributes() {
    	
    	{
    		// 1. All blank
    		MockShibboleth.removeAll();
    		MockShibboleth.set("SHIB_email","");
    		MockShibboleth.set("SHIB_givenName", "");
    		MockShibboleth.set("SHIB_sn", "");

    		final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
    		Response response = GET(LOGIN_URL,true);
    		assertStatus(500, response);
    	}


    	{
    		// 2. Minimum + blank attributes
    		MockShibboleth.removeAll();
    		MockShibboleth.set("SHIB_email","bob@gmail.com");
    		MockShibboleth.set("SHIB_givenName", "Bob");
    		MockShibboleth.set("SHIB_sn", "");

    		final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
    		Response response = GET(LOGIN_URL, true);
    		assertIsOk(response);
    		assertContentType("text/html", response);
            assertContentMatch("<dt>email</dt>[\\s]*<dd>bob\\@gmail\\.com</dd>", response);
            assertContentMatch("<dt>firstName</dt>[\\s]*<dd>Bob</dd>", response);
            assertFalse(getContent(response).contains("<dt>lastName</dt>"));
    	}
    }
   
    
    
    /**
     * Test the logout feature
     */
    //@Test
    public void testLogout() {
    	
    	MockShibboleth.removeAll();
    	
    	final String INDEX_URL = Router.reverse("Application.index").url;
        Response response = GET(INDEX_URL,true);
        assertIsOk(response);
        assertFalse(response.cookies.get("PLAY_SESSION").value.contains("someone%40your-domain.net"));
    	
    	MockShibboleth.set("SHIB_email","someone@your-domain.net");
    	MockShibboleth.set("SHIB_givenName", "Some");
    	MockShibboleth.set("SHIB_sn", "One");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        response = GET(LOGIN_URL,true);
        assertIsOk(response);
        assertTrue(response.cookies.get("PLAY_SESSION").value.contains("someone%40your-domain.net"));
        
        final String LOGOUT_URL = Router.reverse("shib.Shibboleth.logout").url;
        response = GET(LOGOUT_URL);
		assertFalse(response.cookies.get("PLAY_SESSION").value.contains("someone%40your-domain.net"));
    }
    
	/**
	 * Test the multivalue attribute split method.
	 */
    //@Test
	public void testSplit() {

		{
			final String test = "SingleValue";
			List<String> result = Shibboleth.split(test);
			assertNotNull(result);
			assertEquals(1, result.size());
			assertEquals("SingleValue", result.get(0));
		}

		{
			final String test = "One;Two;Three";
			List<String> result = Shibboleth.split(test);
			assertNotNull(result);
			assertEquals(3, result.size());
			assertEquals("One", result.get(0));
			assertEquals("Two", result.get(1));
			assertEquals("Three", result.get(2));
		}
		
		{
			final String test = "One\\;;Two;\\;Three";
			List<String> result = Shibboleth.split(test);
			assertNotNull(result);
			assertEquals(3, result.size());
			assertEquals("One;", result.get(0));
			assertEquals("Two", result.get(1));
			assertEquals(";Three", result.get(2));
		}
		
		{
			final String test = "One\\;;;Two;\\;Three";
			List<String> result = Shibboleth.split(test);
			assertNotNull(result);
			assertEquals(3, result.size());
			assertEquals("One;", result.get(0));
			assertEquals("Two", result.get(1));
			assertEquals(";Three", result.get(2));
		}
		
		{
			final String test = "One\\;\\;Two;";
			List<String> result = Shibboleth.split(test);
			assertNotNull(result);
			assertEquals(1, result.size());
			assertEquals("One;;Two", result.get(0));
		}
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