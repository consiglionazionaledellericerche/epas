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

import cnr.sync.dto.v3.AbsenceShowTerseDto;
import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Dati esportati in Json per i periodi di assenza di una persona in un periodo.
 *
 * @author Cristian Lucchesi
 *
 */
@Data
@RequiredArgsConstructor
public class AbsencePeriodDto {

  private final PersonShowTerseDto person;
  private final String code;
  private final LocalDate start;
  private LocalDate end;
  private List<AbsenceShowTerseDto> absences = Lists.newArrayList();
}