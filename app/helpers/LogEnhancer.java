package helpers;

import controllers.Security;

import org.slf4j.MDC;

import play.mvc.Before;
import play.mvc.Controller;

/**
 * Inserisce nella MDC dei log alcune informazioni.
 *
 * @author cristian
 *
 */
public class LogEnhancer extends Controller {

  private static final String ANONYMOUS_USERNAME = "anonymous"; 
  
  @Before(unless = {"login", "authenticate"})
  static void injectTemplateData() {
    MDC.put(
        "user", 
        Security.getUser().isPresent() ? Security.getUser().get().username : ANONYMOUS_USERNAME);
  }
}
