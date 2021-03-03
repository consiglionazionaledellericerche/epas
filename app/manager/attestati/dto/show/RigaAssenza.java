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

package manager.attestati.dto.show;

import com.google.common.base.MoreObjects;

/**
 * Rappresenta una riga di assenza di attestati.
 */
public class RigaAssenza {

  public int id;
  public String codiceAssenza;
  public int giornoInizio;
  public int giornoFine;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(RigaAssenza.class)
        .add("id", id)
        .add("codiceAssenza", codiceAssenza)
        .add("giornoInizio", giornoInizio)
        .add("giornoFine", giornoFine)
        .toString();
  }
  
  /**
   * Serializzazione della riga nel formato di attestati.
   */
  public String serializeContent() {
    return this.codiceAssenza + ";" + this.giornoInizio + ";" + this.giornoFine;
  }
}
