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

import com.google.gson.GsonBuilder;
import common.injection.StaticInject;
import helpers.deserializers.AbsenceFromClientDeserializer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import models.exports.AbsenceFromClient;
import play.data.binding.Global;
import play.data.binding.TypeBinder;


/**
 * Binder per il json delle assenze.
 *
 * @author Cristian Lucchesi
 */
@Global
@StaticInject
public class JsonAbsenceBinder implements TypeBinder<AbsenceFromClient> {

  /**
   * Bind delle assenze recuperate via JSON.
   *
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      Class actualClass, Type genericType) throws Exception {

    return new GsonBuilder().registerTypeAdapter(AbsenceFromClient.class,
        new AbsenceFromClientDeserializer()).create()
        .fromJson(value, AbsenceFromClient.class);
  }
}
