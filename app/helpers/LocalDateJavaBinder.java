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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * Binder per le LocalDate java.util.
 */
@Slf4j
@Global
public class LocalDateJavaBinder implements TypeBinder<LocalDate> {

  //Questo formato è utilizzato nelle form HTML
  static final String ITALIAN_DATE_PATTERN = "dd/MM/yyyy"; 
  static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(ITALIAN_DATE_PATTERN);

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
      log.debug("Enable to bind java LocalDate using pattern {}, value={}", ITALIAN_DATE_PATTERN, value);
    }
    //Nei metodi REST le date vengono passate nel formato ISO
    return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(value));
  }
}
