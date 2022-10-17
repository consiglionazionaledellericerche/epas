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

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import models.absences.Absence;
import org.joda.time.LocalDate;

/**
 * DTO per l'esportazione via REST delle informazioni 
 * principali di un'assenza.
 *
 * @since versione 2 dell'API REST
 * @author Cristian Lucchesi
 *
 */
@Builder
@Data
public class AbsenceDto {

  private Long id;
  private LocalDate date;
  private String code;
  private Integer justifiedTime;
  private String justifiedType;
  private String note;
  private LocalDateTime updatedAt;

  /**
   * Builder del dto.
   *
   * @param absence l'assenza da trasformare in dto
   * @return il dto costruito.
   */
  public static AbsenceDto build(Absence absence) {
    return AbsenceDto.builder()
        .id(absence.id)
        .date(absence.date)
        .code(absence.getCode())
        .justifiedTime(absence.justifiedTime())
        .justifiedType(absence.getJustifiedType().getName().name())
        .note(absence.getNote())
        .updatedAt(absence.getUpdatedAt())
        .build();
  }
}