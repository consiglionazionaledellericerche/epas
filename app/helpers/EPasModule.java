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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import common.injection.AutoRegister;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.inject.Named;
import play.Play;

/**
 * Modulo per l'injection per prelevare il nome dell'istanza dell'applicazione.
 */
@AutoRegister
public class EPasModule extends AbstractModule {

  /**
   * L'istanza attiva.
   *
   * @return l'informazione su quale sia l'istanza attiva.
   */
  @Provides
  @Named("app.instance")
  public String getAppInstance() {
    String hostname = "devel";
    try  {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      //Volutamente vuoto
    } 
    return Play.configuration.getProperty("app.instance", String.format("%s", hostname));
  }
}
