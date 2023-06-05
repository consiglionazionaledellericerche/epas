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

package helpers.rest;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.GsonBuilder;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import common.injection.AutoRegister;
import lombok.val;

/**
 * Registrazione delle utilità per la serializzazione / deserializzazione JSON.
 */
@AutoRegister
public class GsonModule implements Module {

  /**
   * Fornisce una istanza configurata del GsonBuilder.
   */
  @Provides
  public GsonBuilder builderFactory() {
    val builder = new GsonBuilder(); 
    com.fatboyindustrial.gsonjavatime.Converters.registerAll(builder).serializeNulls();
    return Converters.registerAll(builder)
        .serializeNulls();
  }

  @Override
  public void configure(Binder binder) {
  }
}
