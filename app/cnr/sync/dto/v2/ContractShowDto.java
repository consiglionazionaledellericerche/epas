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

import com.beust.jcommander.internal.Sets;
import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.Contract;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per un contratto.
 *
 * @author Cristian Lucchesi
 *
 */ 
@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractShowDto extends ContractShowTerseDto {

  private Set<ContractWorkingTimeTypeShowTerseDto> workingTimeTypes = Sets.newHashSet();

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un ContractShowDto contenente i valori 
   * dell'oggetto contract passato.
   */
  public static ContractShowDto build(Contract contract) throws IllegalStateException {
    val contractDto = modelMapper.map(contract, ContractShowDto.class);
    contractDto.setPerson(PersonShowTerseDto.build(contract.getPerson()));
    if (contract.getPreviousContract() != null) {
      if (contract.getPreviousContract().id.equals(contract.id)) {
        throw new IllegalStateException(
            String.format(
                "The previous contract is equal to the current contract (id=%s), "
                + "please correct this error", contract.id));
      }
      contractDto.setPreviousContract(ContractShowTerseDto.build(contract.getPreviousContract()));
    }
    contractDto.setWorkingTimeTypes(
        contract.getContractWorkingTimeType().stream()
          .map(ContractWorkingTimeTypeShowTerseDto::build)
          .collect(Collectors.toSet()));
    return contractDto;
  }
}
