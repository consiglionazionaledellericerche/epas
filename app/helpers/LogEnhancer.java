package helpers;

import org.slf4j.MDC;

import play.mvc.Before;
import play.mvc.Controller;

import controllers.Security;

/**
 * Inserisce nella MDC dei log alcune informazioni.
 *
 * @author cristian
 *
 */
public class LogEnhancer extends Controller {

  private final static String ANONYMOUS_USERNAME = "anonymous"; 
  
  @Before(unless = {"login", "authenticate", "logout"})
  static void injectTemplateData() {
    MDC.put("user", Security.getUser().isPresent() ? Security.getUser().get().username : ANONYMOUS_USERNAME);
  }
}
