/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package common.security;

import java.lang.reflect.Method;
import play.PlayPlugin;
import play.mvc.Scope;

/**
 * Evita l'errore 500 sul login quando mancano username o password.
 *
 * <p>Secure.authenticate (modulo secure di Play) chiama Security.authenticate per riflessione:
 * play.utils.Java calcola il tipo dei parametri con getClass() e con un parametro nullo va in
 * NullPointerException prima di arrivare al codice di ePAS. Un POST /login senza username o
 * senza password rispondeva quindi 500 invece di un normale login fallito.</p>
 *
 * <p>Il plugin interviene prima che Play leghi i parametri dell'azione e sostituisce quelli
 * mancanti con una stringa vuota: Security.authenticate rifiuta le credenziali vuote e la
 * richiesta segue il normale percorso del login fallito.</p>
 *
 * <p>In conf/play.plugins deve avere priorità minore di 200: il ValidationPlugin di Play lega e
 * memorizza gli argomenti dell'azione (Secure.authenticate ha un parametro @Required) nel suo
 * beforeActionInvocation, quindi i parametri vanno completati prima.</p>
 */
public class LoginParamsPlugin extends PlayPlugin {

  static final String SECURE_CONTROLLER = "controllers.Secure";
  static final String AUTHENTICATE = "authenticate";
  static final String[] CREDENTIALS = {"username", "password"};

  @Override
  public void beforeActionInvocation(Method actionMethod) {
    if (!isSecureAuthenticate(actionMethod)) {
      return;
    }
    final Scope.Params params = Scope.Params.current();
    if (params == null) {
      return;
    }
    for (String name : CREDENTIALS) {
      if (params.get(name) == null) {
        params.put(name, "");
      }
    }
  }

  static boolean isSecureAuthenticate(Method actionMethod) {
    return actionMethod != null
        && AUTHENTICATE.equals(actionMethod.getName())
        && SECURE_CONTROLLER.equals(actionMethod.getDeclaringClass().getName());
  }
}
