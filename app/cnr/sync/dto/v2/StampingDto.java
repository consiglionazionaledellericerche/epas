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

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import models.Stamping;
import models.Stamping.WayType;
import models.enumerate.StampTypes;

/**
 * DTO per l'esportazione via REST delle informazioni 
 * principali di una timbratura.
 *
 * @version 2
 * @author Cristian Lucchesi
 *
 */
@ToString
@Builder
@Data
public class StampingDto {

  private String date;
  private WayType way;
  private StampTypes stampType;
  private String place;
  private String reason;
  private boolean markedByAdmin;
  private boolean markedByEmployee;
  private String note;

  /**
   * Nuova instanza di un StampingDto contenente i valori 
   * dell'oggetto stamping passato.
   */
  public static StampingDto build(Stamping stamping) {
    return StampingDto.builder()
        .date(stamping.getDate().toString())
        .way(stamping.getWay())
        .place(stamping.getPlace())
        .reason(stamping.getReason())
        .markedByAdmin(stamping.isMarkedByAdmin())
        .markedByEmployee(stamping.isMarkedByEmployee())
        .note(stamping.getNote())        
        .build();
  }
}