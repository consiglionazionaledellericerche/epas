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

import lombok.Builder;
import models.Office;
import models.Person;
import models.flows.Group;

/**
 * Dati per l'aggiornamento di un gruppo via REST.
 *
 * @author Cristian Lucchesi
 *
 */
@Builder
public class GroupUpdateDto extends GroupCreateDto {

  /**
   * Aggiorna i dati dell'oggetto Group passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Group group) {
    group.name = getName();
    group.description = getDescription();
    if (getSendFlowsEmail() != null) {
      group.sendFlowsEmail = getSendFlowsEmail();
    }
    if (getOfficeId() != null) {
      group.office = Office.findById(getOfficeId());
    }
    if (getManagerId() != null) {
      group.manager = Person.findById(getManagerId());  
    }
    group.externalId = getExternalId();
    group.endDate = getEndDate();
  }
}