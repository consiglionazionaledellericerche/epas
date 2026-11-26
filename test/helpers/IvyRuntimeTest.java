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

package helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.ivy.Ivy;
import org.junit.Test;
import play.Play;
import play.deps.DependenciesManager;
import play.test.UnitTest;

/**
 * Play usa ivy anche a runtime, all'avvio, per leggere l'ordine dei moduli da
 * dependencies.yml. Verifica che con ivy 2.5 (in lib/, al posto della 2.4.0 di play, #684)
 * la lettura funzioni come prima.
 */
public class IvyRuntimeTest extends UnitTest {

  /**
   * La versione caricata è quella aggiornata.
   */
  @Test
  public void ivyIsTheUpgradedVersion() {
    String jar = Ivy.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    assertTrue(jar, jar.endsWith("ivy-2.5.2.jar"));
  }

  /**
   * L'elenco ordinato dei moduli letto da play contiene tutti i moduli dell'applicazione.
   */
  @Test
  public void playReadsTheOrderedModuleList() throws Exception {
    DependenciesManager dm = new DependenciesManager(Play.applicationPath, Play.frameworkPath,
        new File(System.getProperty("user.home")));

    Set<String> modules = dm.retrieveModules();

    List<String> names = new ArrayList<>();
    for (String module : modules) {
      names.add(module.contains("-") ? module.substring(0, module.indexOf('-')) : module);
    }
    assertTrue(names.toString(), names.contains("secure"));
    assertTrue(names.toString(), names.contains("shibboleth"));
    assertTrue(names.toString(), names.contains("fastergt"));
    assertTrue(names.toString(), names.contains("pdf"));
    assertTrue(names.toString(), names.contains("lombok"));
  }
}
