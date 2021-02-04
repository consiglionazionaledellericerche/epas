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
import injection.StaticInject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import models.ContractWorkingTimeType;
import org.modelmapper.ModelMapper;

@StaticInject
@Data
public class ContractWorkingTimeTypeShowTerseDto {

  private LocalDate beginDate;
  private LocalDate endDate;
  private WorkingTimeTypeShowTerseDto workingTimeType;
  private LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  public static ContractWorkingTimeTypeShowTerseDto build(ContractWorkingTimeType cwtt) {
    return modelMapper.map(cwtt, ContractWorkingTimeTypeShowTerseDto.class);
  }
}
