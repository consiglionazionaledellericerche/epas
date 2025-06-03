/*
 * Copyright (C) 2025  Consiglio Nazionale delle Ricerche
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

import java.time.LocalDate;

import org.modelmapper.ModelMapper;

import helpers.JodaConverters;
import lombok.Data;
import lombok.val;
import models.Contract;
import models.VacationPeriod;
import models.enumerate.VacationCode;
import play.data.validation.Required;

/**
 * Dati per la creazione via REST di un vacation period di un contratto
 * di una persona.
 *
 * @author Cristian Lucchesi
 *
 */
@Data
public class VacationPeriodCreateDto {

  @Required
  private Long contractId;
  @Required
  private LocalDate beginDate;
  private LocalDate endDate;
  @Required
  private VacationCode vacationCode;
  
  /**
   * Nuova istanza di un oggetto vacationPeriod a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static VacationPeriod build(VacationPeriodCreateDto vpDto) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val vp = modelMapper.map(vpDto, VacationPeriod.class);
    if (vpDto.getContractId() != null) {
      vp.setContract(Contract.findById(vpDto.getContractId()));
    }
    vp.setBeginDate(JodaConverters.javaToJodaLocalDate(vpDto.getBeginDate()));
    vp.setEndDate(JodaConverters.javaToJodaLocalDate(vpDto.getEndDate()));
    return vp;
  }
}