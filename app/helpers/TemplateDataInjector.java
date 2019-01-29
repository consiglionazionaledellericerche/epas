package helpers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import injection.StaticInject;

import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Scope.RenderArgs;

/**
 * inject nei renderargs del template una variable templateUtility che corrisponde
 * ad una istanza (nuova per la richiesta) di TemplateData.
 *
 * @author marco
 *
 */
@StaticInject
public class TemplateDataInjector extends Controller {

  @Inject
  static Provider<TemplateUtility> templateUtility;

  @Inject
  static Provider<CompanyConfig> companyConfig;
  
  @Before(unless = {"login", "authenticate", "logout"})
  static void injectTemplateData() {
    RenderArgs.current().put("templateUtility", templateUtility.get());
  }

  @Before(unless = {"authenticate", "logout"})
  static void injectTemplateConfig() {
    RenderArgs.current().put("companyConfig", companyConfig.get());
  }

}
