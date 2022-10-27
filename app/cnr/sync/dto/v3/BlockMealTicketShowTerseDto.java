/*
 * Copyright (C) 2022  Consiglio Nazionale delle Ricerche
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

package cnr.sync.dto.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import common.injection.StaticInject;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import manager.services.mealtickets.BlockMealTicket;
import models.enumerate.BlockType;
import org.modelmapper.ModelMapper;

/**
 * DTO per esportare via JSON le informazioni principali di un 
 * blocchetto di buoni pasto.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@ToString
@Data
@EqualsAndHashCode
public class BlockMealTicketShowTerseDto {

  private String codeBlock;
  private BlockType blockType;
  private PersonShowTerseDto person;
  private Long first;
  private Long last;
  private Long contractId;
  private Long adminId;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un BlockMealTicketShowTerseDto contenente i valori 
   * dell'oggetto mealTicket passato.
   */
  public static BlockMealTicketShowTerseDto build(BlockMealTicket blockMealTicket) {
    Preconditions.checkNotNull(blockMealTicket);
    val dto = modelMapper.map(blockMealTicket, BlockMealTicketShowTerseDto.class);
    dto.setPerson(PersonShowTerseDto.build(blockMealTicket.getContract().getPerson()));
    dto.setContractId(blockMealTicket.getContract().getId());
    dto.setAdminId(blockMealTicket.getAdmin().id);
    return dto;
  }

}