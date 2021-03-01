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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Controller;
import play.mvc.With;

@Slf4j
@With({Resecure.class})
public class Version extends Controller {

  /**
   * Mostra la versione prelevata dal file VERSION.
   */
  public static void showVersion() {
    String version = null;
    try {
      version = Files.asCharSource(new File("VERSION"), Charsets.UTF_8).read();
    } catch (IOException ex) {
      log.error("File di versione 'VERSION' non trovato");
    }
    render(version);
  }

}
