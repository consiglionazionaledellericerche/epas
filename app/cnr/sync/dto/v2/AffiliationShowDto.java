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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.val;
import models.flows.Affiliation;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per l'affiliazione di una persona ad un gruppo.
 *
 * @author Cristian Lucchesi
 *
 */ 
@Data
public class AffiliationShowDto {
  
  private Long id;
  private GroupShowTerseDto group;
  private PersonShowTerseDto person;
  private BigDecimal percentage;
  private LocalDate beginDate;
  private LocalDate endDate;
  private String externalId;
  private LocalDateTime updatedAt;

  /**
   * Nuova instanza di un GroupShowTerseDto contenente i valori 
   * dell'oggetto group passato.
   */
  public static AffiliationShowDto build(Affiliation affiliation) {
    val modelMapper = new ModelMapper();
    val affilationDto = modelMapper.map(affiliation, AffiliationShowDto.class);
    affilationDto.setGroup(GroupShowTerseDto.build(affiliation.getGroup()));
    affilationDto.setPerson(PersonShowTerseDto.build(affiliation.getPerson()));
    return affilationDto;
  }
}