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
import models.Qualification;

/**
 * Dati per l'aggiornamento di una persona via REST.
 *
 * @author Cristian Lucchesi
 *
 */
@Builder
public class PersonUpdateDto extends PersonCreateDto {

  /**
   * Aggiorna i dati dell'oggetto Person passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Person person) {
    person.setName(getName());
    person.setSurname(getSurname());
    person.setOthersSurnames(getOthersSurnames());
    person.setFiscalCode(getFiscalCode());
    person.setEmail(getEmail());
    person.setNumber(getNumber());
    person.setEppn(getEppn());
    person.setTelephone(getTelephone());
    person.setFax(getFax());
    person.setMobile(getMobile()); 
    if (getQualification() != null) {
      person.setQualification(
          ((Qualification) Qualification.findAll().stream()
              .filter(q -> ((Qualification) q).getQualification() == getQualification().intValue())
              .findFirst().get()));        
    }
    if (getOfficeId() != null) {
      person.setOffice(Office.findById(getOfficeId()));  
    }

  }
}