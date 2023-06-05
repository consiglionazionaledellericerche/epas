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
 * Enumerato per la definizione del tipo di pagamento del turno.
 * Enumerato che serve a stabilire se pagare tutto il turno con una certa tipologia o con 
 * un'altra se questo ricade in parte in una fascia e in parte in un'altra.
 *
 * @author Dario Tagliaferri
 */
public enum PaymentType {

  T1("T1", "Calcolo tutto con turno diurno"),
  SPLIT_CALCULATION("SPLIT_CALCULATION", "Calcolo sulla base delle fasce di turno");
  
  public String name;
  public String description;
  
  private PaymentType(String name, String description) {
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
