package controllers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

import models.User;
import play.mvc.Before;
import play.mvc.Controller;
import security.SecurityRules;

/**
 * @author marco
 *
 */
public class Resecure extends Controller {
	
	@Inject
	static SecurityRules rules;
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD})
	public @interface NoCheck {
	}
	
	@Before(unless={"login", "authenticate", "logout"})
    static void checkAccess() throws Throwable {
		if (getActionAnnotation(NoCheck.class) != null || 
				getControllerInheritedAnnotation(NoCheck.class) != null) {
			return;
		} else {
			if (Security.getUser().isPresent()) {
				User user = Security.getUser().get();
				renderArgs.put("currentUser", Security.getUser().get());
			}
			Secure.checkAccess();
			rules.checkIfPermitted();
        }
	}
	
	public static boolean check(String action, Object instance) {
		return rules.check(action, instance);
	}
	
}
