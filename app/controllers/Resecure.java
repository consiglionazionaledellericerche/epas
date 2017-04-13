package controllers;

import helpers.LogEnhancer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Scope.Session;

import security.SecurityRules;

/**
 * Contiene metodi per l'attivazione dei controlli sui permessi per le richieste ai controller.
 *
 * @author marco
 */
@With({RequestInit.class, LogEnhancer.class})
public class Resecure extends Controller {

  public static final String OFFICE_COUNT = "officeCount";
  private static final String REALM = "E-PAS";
  @Inject
  static SecurityRules rules;

  @Before(priority = 1, unless = {"login", "authenticate", "logout"})
  static void checkAccess() throws Throwable {
    if (getActionAnnotation(NoCheck.class) != null
        || getControllerInheritedAnnotation(NoCheck.class) != null) {
      return;
    } else {
      if (getActionAnnotation(BasicAuth.class) != null
          || getControllerInheritedAnnotation(BasicAuth.class) != null) {
        if (request.user == null
            || !Security.authenticate(request.user, request.password)) {
          unauthorized(REALM);
        } else {
          // WARNING: inserimento in sessione per farlo digerire alla play Security
          Session.current().put("username", request.user);
        }
      }
      Secure.checkAccess();
      rules.checkIfPermitted();
    }
  }

  public static boolean check(String action, Object instance) {
    if (instance != null) {
      return session.contains("username") && rules.check(action, instance);
    } else {
      return session.contains("username") && rules.checkAction(action);
    }
  }

  /**
   * Con questo si evitano i controlli. 
   *
   * @author marco
   *
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface NoCheck {

  }

  /**
   * Con questo si adotta soltanto la basicauth.
   * 
   * @author marco
   * 
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface BasicAuth {

  }
}
