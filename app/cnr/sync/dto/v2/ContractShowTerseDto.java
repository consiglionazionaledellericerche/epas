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

import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import lombok.val;
import models.Contract;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in forma ridotta ed in Json per un contratto.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
public class ContractShowTerseDto {

  private Long id;
  private PersonShowTerseDto person;
  private LocalDate beginDate;
  private LocalDate endDate;
  private LocalDate endContract;
  private String externalId;

  private Boolean onCertificate;
  private ContractShowTerseDto previousContract;
  private LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un ContractShowTerseDto contenente i valori 
   * dell'oggetto contract passato.
   */
  public static ContractShowTerseDto build(Contract contract) throws IllegalStateException {
    val contractDto = modelMapper.map(contract, ContractShowTerseDto.class);
    contractDto.setPerson(PersonShowTerseDto.build(contract.person));
    if (contract.getPreviousContract() != null) {
      if (contract.getPreviousContract().id.equals(contract.id)) {
        throw new IllegalStateException(
            String.format(
                "The previous contract is equal to the current contract (id=%s), "
                + "please correct this error", contract.id));
      }
      contractDto.setPreviousContract(ContractShowTerseDto.build(contract.getPreviousContract()));
    }
    return contractDto;
  }
}
