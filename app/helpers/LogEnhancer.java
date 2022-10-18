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

import controllers.Security;
import org.slf4j.MDC;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;

/**
 * Inserisce nella MDC dei log alcune informazioni.
 *
 * @author Cristian Lucchesi
 *
 */
public class LogEnhancer extends Controller {

  private static final String ANONYMOUS_USERNAME = "anonymous"; 
  
  @Before
  static void mdcPutUser() {
    MDC.put(
        "user", 
        Security.getUser().isPresent() ? Security.getUser().get().getUsername() : ANONYMOUS_USERNAME);
  }
  
  @After
  static void mdcRemoveUser() {    
    MDC.remove("user");
    
  }
}
