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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.data.binding.Global;
import play.data.binding.TypeBinder;
import play.data.binding.types.DateBinder;

/**
 * Binder per le LocalDate joda.
 */
@Slf4j
@Global
public class LocalDateBinder implements TypeBinder<LocalDate> {

  private static final DateBinder DATE_BINDER = new DateBinder();
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYYY");

  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      Class actualClass, Type genericType) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalDate.parse(value, dtf);
    } catch (Exception ignored) {
      log.debug("Exception during binding LocalDate {} format dd/MM/YYYY", value);
      log.trace("Exception: ", ignored);
    }
    try {
      return LocalDate.parse(value, DateTimeFormat.forPattern("YYYY-MM-dd"));
    } catch (Exception e) {
      log.debug("Exception during binding LocalDate {} as ISO", value);
    }

    return new LocalDate(DATE_BINDER.bind(name, annotations, value, actualClass, genericType));
  }

}