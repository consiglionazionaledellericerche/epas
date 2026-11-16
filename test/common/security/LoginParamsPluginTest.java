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

import java.util.List;
import org.junit.Test;
import play.Play;
import play.PlayPlugin;
import play.data.validation.ValidationPlugin;
import play.test.UnitTest;

/**
 * Verifica della configurazione di LoginParamsPlugin.
 */
public class LoginParamsPluginTest extends UnitTest {

  /**
   * Il plugin deve completare i parametri del login prima che il ValidationPlugin leghi e
   * memorizzi gli argomenti dell'azione: se l'ordine si inverte il POST /login senza
   * username o password torna a rispondere 500.
   */
  @Test
  public void runsBeforeValidationPlugin() {
    final List<PlayPlugin> plugins = Play.pluginCollection.getEnabledPlugins();
    final LoginParamsPlugin loginParams =
        Play.pluginCollection.getPluginInstance(LoginParamsPlugin.class);
    final ValidationPlugin validation =
        Play.pluginCollection.getPluginInstance(ValidationPlugin.class);

    assertNotNull("LoginParamsPlugin non registrato in conf/play.plugins", loginParams);
    assertNotNull(validation);
    assertTrue("LoginParamsPlugin non abilitato", plugins.contains(loginParams));
    assertTrue("ValidationPlugin non abilitato", plugins.contains(validation));
    assertTrue("la priorità di LoginParamsPlugin (" + loginParams.index
            + ") deve essere minore di quella di ValidationPlugin (" + validation.index + ")",
        loginParams.index < validation.index);
    assertTrue(plugins.indexOf(loginParams) < plugins.indexOf(validation));
  }
}
