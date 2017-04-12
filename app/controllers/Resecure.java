package controllers;

import helpers.LogEnhancer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Inject;
import play.Play;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;
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
      //Se si tenta di accedere con Basic Auth si controlla subito utente e password
      if (request.user != null && !Security.authenticate(request.user, request.password)) {
        unauthorized(REALM);
      }

      if (!Security.getUser().isPresent()) {
        flash.put(
            "url",
            // seems a good default
            "GET".equals(request.method) ? request.url : Play.ctxPath + "/");
        Secure.login();
      }
      // Checks
      Check check = getActionAnnotation(Check.class);
      if (check != null) {
        check(check);
      }
      check = getControllerInheritedAnnotation(Check.class);
      if (check != null) {
        check(check);
      }
      rules.checkIfPermitted();
    }
  }

  private static void check(Check check) throws Throwable {
    for (String profile : check.value()) {
      boolean hasProfile = (Boolean) Security.invoke("check", profile);
      if (!hasProfile) {
        Security.invoke("onCheckFailed", profile);
      }
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
   * @author marco
   *
   *         Con questo si evitano i controlli.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface NoCheck {

  }

  /**
   * @author marco
   *
   *         Con questo si adotta soltanto la basicauth.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface BasicAuth {

  }
}
