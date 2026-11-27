/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package cnr.sync.dto.v4;

import helpers.JodaConverters;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import models.flows.AbsenceRequestEvent;
import models.flows.enumerate.AbsenceRequestEventType;

/**
 * Un evento del flusso di approvazione di una richiesta di assenza.
 */
@Data
@Builder
public class AbsenceRequestEventShowDto {

  private LocalDateTime createdAt;
  private AbsenceRequestEventType eventType;
  private String description;
  // chi ha generato l'evento
  private String owner;

  /**
   * Nuova instanza di un AbsenceRequestEventShowDto contenente i valori dell'evento passato.
   */
  public static AbsenceRequestEventShowDto build(AbsenceRequestEvent event) {
    return AbsenceRequestEventShowDto.builder()
        .createdAt(JodaConverters.jodaToJavaLocalDateTime(event.createdAt))
        .eventType(event.eventType)
        .description(event.description)
        .owner(event.owner.getPerson() != null
            ? event.owner.getPerson().getFullname() : event.owner.getUsername())
        .build();
  }
}
