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

import common.security.ContentSecurityPolicy.Mode;
import java.lang.reflect.Method;
import play.PlayPlugin;
import play.mvc.Http;

/**
 * Aggiunge l'header della Content Security Policy alle risposte delle azioni.
 *
 * <p>È un plugin e non un @Before dei controller perché l'header serve anche sulle pagine dei
 * controller che non passano da Resecure, come il login del modulo secure.</p>
 *
 * @see ContentSecurityPolicy
 */
public class ContentSecurityPolicyPlugin extends PlayPlugin {

  @Override
  public void beforeActionInvocation(Method actionMethod) {
    final Mode mode = ContentSecurityPolicy.mode();
    final Http.Response response = Http.Response.current();
    if (mode == Mode.OFF || response == null) {
      return;
    }
    response.setHeader(mode.header, ContentSecurityPolicy.headerValue());
  }
}
