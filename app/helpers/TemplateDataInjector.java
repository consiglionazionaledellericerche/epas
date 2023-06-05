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

package helpers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import common.injection.StaticInject;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Scope.RenderArgs;

/**
 * inject nei renderargs del template una variable templateUtility che corrisponde
 * ad una istanza (nuova per la richiesta) di TemplateData.
 *
 * @author Marco Andreini
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
