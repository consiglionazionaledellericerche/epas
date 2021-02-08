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

package it.cnr.iit.epas;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import models.exports.AuthInfo;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * Binder per le informazioni sul auth info (username, password).
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@Global
public class AuthInfoBinder implements TypeBinder<AuthInfo> {

  /**
   * Binder per le informazioni di autenticazione in formato JSON.
   *
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(
      String name, Annotation[] annotations, String value,
      Class actualClass, Type genericType) throws Exception {

    log.trace("binding AuthInfo: {}, {}, {}, {}, {}",
        name, annotations, value, actualClass, genericType);
    JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();

    return new AuthInfo(
        jsonObject.get("username").getAsString(), jsonObject.get("password").getAsString());

  }

}
