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
import models.Person;
import models.flows.Affiliation;
import models.flows.Group;

/**
 * Dati per l'aggiornamento vai REST di una affiliazione di 
 * una persona ad un gruppo.
 *
 * @author Cristian Lucchesi
 *
 */
@Builder
public class AffiliationUpdateDto extends AffiliationCreateDto {

  /**
   * Aggiorna i dati dell'oggetto affiliation passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Affiliation affiliation) {
    if (getGroupId() != null) {
      affiliation.setGroup(Group.findById(getGroupId()));
    }
    if (getPersonId() != null) {
      affiliation.setPerson(Person.findById(getPersonId()));  
    }
    affiliation.setPercentage(getPercentage());
    affiliation.setBeginDate(getBeginDate());
    affiliation.setEndDate(getEndDate());
  }
}