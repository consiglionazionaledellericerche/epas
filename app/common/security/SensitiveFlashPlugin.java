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

import play.PlayPlugin;
import play.mvc.Scope;
import play.mvc.results.Result;

/**
 * Impedisce che le credenziali finiscano nel cookie del flash.
 *
 * <p>Il modulo secure, dopo un login fallito, copia tutti i parametri della richiesta nel flash
 * con params.flash(), compresa la password, e il flash viene inviato al browser in chiaro.
 * Questo plugin interviene dopo l'azione e prima del salvataggio del flash.</p>
 */
public class SensitiveFlashPlugin extends PlayPlugin {

  @Override
  public void onActionInvocationResult(Result result) {
    final Scope.Params params = Scope.Params.current();
    final Scope.Flash flash = Scope.Flash.current();
    if (params == null || flash == null) {
      return;
    }
    for (String name : params.all().keySet()) {
      if (SensitiveParams.isSensitive(name)) {
        flash.discard(name);
      }
    }
  }
}
