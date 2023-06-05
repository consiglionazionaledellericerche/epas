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

package helpers.validators;

import java.util.regex.Pattern;
import play.data.validation.Check;

/**
 * Validatore che verifica che una stringa rappresenti un orario in ore e minuti.
 */
public class StringIsTime extends Check {

  /**
   * matches strings as HH:MM or HHMM.
   */
  @Override
  public boolean isSatisfied(Object validatedObject, Object time) {

    setMessage("invalid.time");

    return Pattern.compile("^(([0-1][0-9]|2[0-3]):?[0-5][0-9])$")
            .matcher((String) time).matches();
  }
}
