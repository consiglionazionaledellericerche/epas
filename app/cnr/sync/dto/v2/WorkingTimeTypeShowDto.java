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
import java.util.List;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.WorkingTimeType;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per le impostazioni di un 
 * orario di lavoro compreso gli orario giornalieri.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkingTimeTypeShowDto extends WorkingTimeTypeShowTerseDto {

  private List<WorkingTimeTypeDayShowDto> workingTimeTypeDays;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova istanza del DTO a partire dal'entity.
   */
  public static WorkingTimeTypeShowDto build(WorkingTimeType wtt) {
    return modelMapper.map(wtt, WorkingTimeTypeShowDto.class);
  }
}
