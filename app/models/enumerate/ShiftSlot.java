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

package models.enumerate;

/**
 * Enumerato per la tipoloia di slot di turno.
 *
 * @author Arianna Del Soldato
 */
public enum ShiftSlot {
  MORNING("mattina"),
  AFTERNOON("pomeriggio"),
  EVENING("sera");

  private String name;

  ShiftSlot(String name) {
    this.name = name;
  }

  /**
   * Lo slot di turno a partire dal nome.
   *
   * @param name il nome dello slot
   * @return lo slot di turno col nome passato come parametro.
   */
  public static ShiftSlot getEnum(String name) {
    for (ShiftSlot shiftSlot : values()) {
      if (shiftSlot.getName().equals(name)) {
        return shiftSlot;
      }
    }
    throw new IllegalArgumentException(String.format("ShiftSlot with name = %s not found", name));
  }

  public String getName() {
    return name;
  }

}
