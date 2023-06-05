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
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import models.WorkingTimeTypeDay;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per le impostazioni giornaliere di un 
 * orario di lavoro.
 *
 * @author Cristian Lucchesi
 *
 */
@Data
public class WorkingTimeTypeDayShowDto {

  private Long id;
  private int dayOfWeek;
  private Integer workingTime;
  private boolean holiday;
  private Integer breakTicketTime;
  private Integer ticketAfternoonThreshold;
  private Integer ticketAfternoonWorkingTime;
  private Integer timeMealFrom;
  private Integer timeMealTo;
  private LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova istanza del DTO a partire dal'entity.
   */
  public WorkingTimeTypeDayShowDto build(WorkingTimeTypeDay wttd) {
    return modelMapper.map(wttd, WorkingTimeTypeDayShowDto.class);
  }
}
