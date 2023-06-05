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
import lombok.ToString;
import models.Office;

/**
 * Informazioni esportate in Json per l'ufficio.
 *
 * @author Cristian Lucchesi
 *
 */
@ToString
@Builder
@Data
public class OfficeDto {

  private Long id;
  private String name;
  private String code;
  private String codeId;
  private LocalDateTime updatedAt;
  
  /**
   * Nuova instanza di un OfficeDto contenente i valori 
   * dell'oggetto office passato.
   */
  public static OfficeDto build(Office office) {
    return OfficeDto.builder()
        .id(office.id)
        .name(office.getName())
        .code(office.getCode())
        .codeId(office.getCodeId())
        .updatedAt(office.getUpdatedAt())
        .build();
  }
}