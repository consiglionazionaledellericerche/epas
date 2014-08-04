package controllers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

import models.User;
import play.Play;
import play.mvc.Before;
import play.mvc.Controller;
import security.SecurityRules;

/**
 * @author marco
 *
 */
public class Resecure extends Controller {
	
	private static final String REALM = "E-PAS";
	  
	@Inject
	static SecurityRules rules;
	
	/**
	 * @author marco
	 * 
	 * Con questo si evitano i controlli.
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD})
	public @interface NoCheck {
	}
	
	/**
	 * @author marco
	 * 
	 * Con questo si adotta soltanto la basicauth.
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD})
	public @interface BasicAuth {
	}
	
	@Before(unless={"login", "authenticate", "logout"})
    static void checkAccess() throws Throwable {
		if (getActionAnnotation(NoCheck.class) != null || 
				getControllerInheritedAnnotation(NoCheck.class) != null) {
			return;
		} else {
			if (getActionAnnotation(BasicAuth.class) != null ||
					getControllerInheritedAnnotation(BasicAuth.class) != null) {
				if (request.user == null ||
						!Security.authenticate(request.user, request.password)) {
					unauthorized(REALM);
				}
			}
			if (Security.getUser().isPresent()) {
				final User user = Security.getUser().get();
				renderArgs.put("currentUser", user);
			} else {
	            flash.put("url", "GET".equals(request.method) ? request.url : Play.ctxPath + "/"); // seems a good default
	            Secure.login();
	        }
	        // Checks
	        Check check = getActionAnnotation(Check.class);
	        if(check != null) {
	            check(check);
	        }
	        check = getControllerInheritedAnnotation(Check.class);
	        if(check != null) {
	            check(check);
	        }
			rules.checkIfPermitted();
        }
	}

    private static void check(Check check) throws Throwable {
        for(String profile : check.value()) {
            boolean hasProfile = (Boolean)Security.invoke("check", profile);
            if(!hasProfile) {
                Security.invoke("onCheckFailed", profile);
            }
        }
    }

    public static boolean check(String action, Object instance) {
		return rules.check(action, instance);
	}
}
