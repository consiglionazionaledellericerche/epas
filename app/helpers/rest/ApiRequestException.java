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

/**
 * Eccezioni per api.
 *
 * @author Daniele Murgia
 * @since 19/04/16.
 */
public class ApiRequestException extends RuntimeException {

  private static final long serialVersionUID = 5106927141254697844L;

  public ApiRequestException(final String message) {
    super(message);
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
