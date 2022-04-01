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

package cnr.sync.dto.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import manager.services.mealtickets.BlockMealTicket.TemplateRow;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

import javax.inject.Inject;

/**
 * DTO per creare via JSON le informazioni principali di un
 * blocchetto di buoni pasto.
 *
 * @author Loredana Sideri
 *
 */
@StaticInject
@Data
public class BlockMealTicketCreateDto {

  @Required
  private String codeBlock;
  @Required
  private BlockType blockType;
  @Required
  private Long contractId;
  @Required
  private Integer first;
  @Required
  private Integer last;
  @Required
  public LocalDate expiredDate;
  @Required
  public LocalDate deliveryDate;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un BlockMealTicket a partire dai
   * valori presenti nel rispettivo DTO.
   */
  public static BlockMealTicketCreateDto build(TemplateRow templateRow) {

    BlockMealTicketCreateDto bmt = new BlockMealTicketCreateDto();

    bmt.codeBlock = templateRow.codeBlock;
    bmt.blockType = templateRow.blockType;
    bmt.contractId = templateRow.contractId;
    bmt.first = templateRow.first;
    bmt.last = templateRow.last;
    bmt.expiredDate = templateRow.expiredDate;
    bmt.deliveryDate = templateRow.deliveryDate;

    return bmt;
  }
}