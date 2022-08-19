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

package models.exports;

import cnr.sync.dto.v3.StampingCreateDto;
import helpers.JodaConverters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import models.Person;
import models.Stamping.WayType;
import models.enumerate.StampTypes;
import org.joda.time.LocalDateTime;

/**
 * Esportazione delle informazioni relative ad una timbratura.
 *
 * @author Cristian Lucchesi
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StampingFromClient {

  public String numeroBadge;
  public Integer inOut;
  public StampTypes stampType;
  public Person person;
  public boolean markedByAdmin;
  public boolean markedByEmployee;
  public LocalDateTime dateTime;
  public String zona;
  public String note;
  public String reason;
  public String place;
  
  /**
   * Costruisce uno StampingFromClient a partire da
   * un StampingCreateDto.
   */
  public static StampingFromClient build(StampingCreateDto dto) {
    return StampingFromClient.builder()
      .numeroBadge(dto.getBadgeNumber())
      .inOut(dto.getWayType().equals(WayType.in) ? 0 : 1)
      .stampType(
          dto.getReasonType() != null ? StampTypes.byCode(dto.getReasonType().name()) : null)
      .markedByAdmin(dto.isMarkedByAdmin())
      .markedByEmployee(dto.isMarkedByEmployee())
      .dateTime(JodaConverters.javaToJodaLocalDateTime(dto.getDateTime()))
      .zona(dto.getStampingZone())
      .note(dto.getNote())
      .reason(dto.getReason())
      .place(dto.getPlace())
      .build();
  }
}
