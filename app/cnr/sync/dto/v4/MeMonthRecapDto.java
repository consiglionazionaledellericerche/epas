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

import cnr.sync.dto.v3.PersonDayShowTerseDto;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * La situazione mensile del dipendente autenticato.
 */
@Data
@Builder
public class MeMonthRecapDto {

  private int year;
  private int month;
  private int basedWorkingDays;
  // residuo orario (anno precedente + anno corrente) a fine mese, in minuti
  private int residualMinutes;
  private int mealTicketResidual;
  private List<PersonDayShowTerseDto> personDays;
}
