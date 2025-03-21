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

import com.google.common.base.Verify;
import helpers.JodaConverters;
import lombok.Builder;
import models.Contract;
import models.Person;

/**
 * Dati per l'aggiornamento vai REST di un contratto di 
 * una persona.
 *
 * @author Cristian Lucchesi
 *
 */
@Builder
public class ContractUpdateDto extends ContractCreateDto {

  /**
   * Aggiorna i dati dell'oggetto contract passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Contract contract) {
    Verify.verifyNotNull(contract);
    Verify.verifyNotNull(contract.getPerson());
    Verify.verifyNotNull(getPersonId());
    
    contract.setPerson(Person.findById(getPersonId()));
    contract.setBeginDate(JodaConverters.javaToJodaLocalDate(getBeginDate()));
    contract.setEndDate(JodaConverters.javaToJodaLocalDate(getEndDate()));
    contract.setEndContract(JodaConverters.javaToJodaLocalDate(getEndContract()));
    contract.setPerseoId(getPerseoId());
    //contract.setOnCertificate(getOnCertificate());
    contract.setExternalId(getExternalId());
  }
}