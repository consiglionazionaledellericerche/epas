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

package cnr.sync.dto.v2;

import helpers.JodaConverters;
import lombok.Builder;
import models.PersonChildren;

/**
 * Dati per l'aggiornamento di un figlio/figlia via REST.
 *
 * @author Cristian Lucchesi
 *
 */
@Builder
public class ChildrenUpdateDto extends ChildrenCreateDto {

  /**
   * Aggiorna i dati dell'oggetto Person passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(PersonChildren children) {
    children.setName(getName());
    children.setSurname(getSurname());
    children.setBornDate(JodaConverters.javaToJodaLocalDate(getBornDate()));
    children.setTaxCode(getFiscalCode());
    children.setExternalId(getExternalId());
  }
}