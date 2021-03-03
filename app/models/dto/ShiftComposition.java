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

package models.dto;

import lombok.Data;
import manager.services.shift.configuration.OrganizationShiftConfiguration;

/**
 * Configurazione e quantità di un turno.
 */
@Data
public class ShiftComposition {

  private int quantity;
  private OrganizationShiftConfiguration organizationShiftConfiguration;
  
  public ShiftComposition(OrganizationShiftConfiguration organizationShiftConfiguration, 
      int quantity) {
    this.organizationShiftConfiguration = organizationShiftConfiguration;
    this.quantity = quantity;
  }
  
}
