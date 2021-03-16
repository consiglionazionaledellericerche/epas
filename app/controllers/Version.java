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

import play.mvc.Controller;
import play.mvc.With;

/**
 * Mostra la versione dell'applicazione in funzione del file VERSION.
 *
 * @author Cristian Lucchesi
 *
 */

@With({Resecure.class})
public class Version extends Controller {

  /**
   * Mostra la versione prelevata dal file VERSION.
   */
  public static void showVersion() {
    String version = ApplicationInfo.getVersion();
    render(version);
  }

}
