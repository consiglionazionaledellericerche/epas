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

import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v3.OfficeShowTerseDto;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Il profilo del dipendente autenticato.
 */
@Data
@Builder
public class MeShowDto {

  private PersonShowTerseDto person;
  private OfficeShowTerseDto office;
  private Integer qualification;
  // livelli I-III
  private boolean topQualification;
  private LocalDate currentContractBeginDate;
  private LocalDate currentContractEndDate;
  private List<AbsenceRequestTypeDto> absenceRequestTypes;
}
