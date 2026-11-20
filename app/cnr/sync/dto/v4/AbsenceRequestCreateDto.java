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

import java.time.LocalDate;
import lombok.Data;
import models.flows.enumerate.AbsenceRequestType;
import play.data.validation.Required;

/**
 * I dati di una nuova richiesta di assenza del dipendente autenticato.
 */
@Data
public class AbsenceRequestCreateDto {

  @Required
  private AbsenceRequestType type;
  @Required
  private LocalDate startAt;
  // se assente la richiesta è per il solo giorno di inizio
  private LocalDate endTo;
  // ore e minuti solo per i permessi personali orari, altrimenti il permesso è giornaliero
  private Integer hours;
  private Integer minutes;
  // obbligatoria per le richieste nel passato
  private String note;
}
