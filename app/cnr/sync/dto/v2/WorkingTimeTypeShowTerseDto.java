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
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import models.WorkingTimeType;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per le impostazioni di un 
 * orario di lavoro.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
public class WorkingTimeTypeShowTerseDto {

  private Long id;
  private String description;
  private Boolean horizontal;
  private OfficeDto office;
  private boolean disabled;
  private String externalId;
  private LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova istanza del DTO a partire dal'entity.
   */
  public static WorkingTimeTypeShowTerseDto build(WorkingTimeType wtt) {
    return modelMapper.map(wtt, WorkingTimeTypeShowTerseDto.class);
  }
}