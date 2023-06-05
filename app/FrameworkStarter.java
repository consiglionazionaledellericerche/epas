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

import java.io.File;
import java.lang.reflect.Method;
import play.Play;

/**
 * Classe wrapper utilizzare per avviare il server del play.
 *
 * @author Marco Andreini
 *
 */
public class FrameworkStarter {

  /**
   * Avvia il play in modalità server.
   */
  public static void main(String[] args) throws Exception {

    Play.frameworkPath = new File(System.getProperty("playFramework"));
    final Class<?> cls = Class.forName(System.getProperty("playMainClass",
        "play.server.Server"));
    final Method meth = cls.getMethod("main", String[].class);
    meth.invoke(null, (Object) args);
  }
}