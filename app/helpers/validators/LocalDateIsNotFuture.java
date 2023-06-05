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

import org.joda.time.LocalDate;
import play.data.validation.Check;

/**
 * Controlla che una data non sia futura.
 *
 * @author Cristian Lucchesi
 *
 */
public class LocalDateIsNotFuture extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object date) {
    if (date == null) {
      return false;
    }
    setMessage("Richiesta una data non futura");
    return !((LocalDate) date).isAfter(LocalDate.now());
  }
}
