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
 * Enumerato per la definizione del tipo di calcolo.
 *
 * @author Dario Tagliaferri
 *
 */
public enum CalculationType {

  standard_CNR("Calcolo CNR Standard", "Calcola il quantitativo orario in turno sulla base "
      + "del rispetto orario della fascia."),
  percentage("Calcolo percentuale", "In base a quanto tempo si trascorre all'interno della "
      + "fascia di turno, il turno viene pagato proporzionalmente.");
  
  public String name;
  public String description;
  
  private CalculationType(String name, String description) {
    this.name = name;
    this.description = description;
  }
  
  public String getDescription() {
    return description;
  }
  
  public String getName() {
    return name;
  }
  
}
