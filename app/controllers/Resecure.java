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
import security.SecurityRules;

/**
 * Contiene metodi per l'attivazione dei controlli sui permessi per le richieste ai controller.
 *
 * @author Marco Andreini
 */
@With({RequestInit.class, LogEnhancer.class, Metrics.class})
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
    }
    if (getActionAnnotation(BasicAuth.class) != null
        || getControllerInheritedAnnotation(BasicAuth.class) != null) {
      if (!Secure.Security.isConnected()) {
        unauthorized(REALM);
      }
    } else {
      Secure.checkAccess();
    }
    rules.checkIfPermitted();
  }

  /**
   * True se si può eseguire l'azione sull'istanza, false altrimenti.
   *
   * @param action l'azione da eseguire
   * @param instance l'oggetto su cui eseguirla
   * @return se è possibile eseguire l'azione action sull'istanza instance.
   */
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
   * @author Marco Andreini
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface NoCheck {

  }

  /**
   * Con questo si adotta soltanto la basicauth.
   *
   * @author Marco Andreini
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface BasicAuth {

  }
}
