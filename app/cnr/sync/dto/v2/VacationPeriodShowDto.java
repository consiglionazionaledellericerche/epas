/*
 * Copyright (C) 2025 Consiglio Nazionale delle Ricerche
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
import models.VacationPeriod;
import models.enumerate.VacationCode;
import models.flows.Affiliation;
import org.modelmapper.ModelMapper;

import helpers.JodaConverters;

/**
 * Dati esportati in Json per il Vacation Period di una persona.
 *
 * @author Cristian Lucchesi
 *
 */ 
@Data
public class VacationPeriodShowDto {

  private Long contractId;
  private LocalDate beginDate;
  private LocalDate endDate;
  private VacationCode vacationCode;

  /**
   * Nuova instanza di un VacationPeriodShow contenente i valori 
   * dell'oggetto vacationPeriod passato.
   */
  public static VacationPeriodShowDto build(VacationPeriod vp) {
    val modelMapper = new ModelMapper();
    val dto = modelMapper.map(vp, VacationPeriodShowDto.class);
    dto.setContractId(vp.getContract().getId());
    dto.setBeginDate(JodaConverters.jodaToJavaLocalDate(vp.getBeginDate()));
    dto.setEndDate(JodaConverters.jodaToJavaLocalDate(vp.getEndDate()));
    return dto;
  }
}