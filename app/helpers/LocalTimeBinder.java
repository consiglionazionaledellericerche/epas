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
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.data.binding.TypeBinder;
import play.data.binding.types.DateBinder;

/**
 * Binder per le LocalTime joda.
 */
@Slf4j
public class LocalTimeBinder implements TypeBinder<LocalTime> {
  
  private static final DateBinder TIME_BINDER = new DateBinder();
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("HH:mm");

  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value, Class actualClass,
      Type genericType) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalTime.parse(value, dtf);
    } catch (Exception ignored) {
      log.info("Exception during LocalDate binding, value = {}", value);
    }
    return new LocalTime(TIME_BINDER.bind(name, annotations, value, actualClass, genericType));
  }

}
