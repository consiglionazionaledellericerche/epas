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

package cnr.sync.dto;

import com.google.common.base.Function;
import models.Competence;

/**
 * DTO per rappresentare i dati una competenza (straordinari, turni, etc) via REST.
 */
public class CompetenceDto {
  public int year;
  public int month;
  public String code;
  public int valueApproved;

  /**
   * Applica la conversione da dto a oggetto.
   *
   * @author dario
   *
   */
  public enum FromCompetence implements Function<Competence, CompetenceDto> {
    ISTANCE;

    @Override
    public CompetenceDto apply(Competence competence) {
      CompetenceDto competenceDto = new CompetenceDto();
      competenceDto.year = competence.year;
      competenceDto.month = competence.month;
      competenceDto.code = competence.competenceCode.code;
      competenceDto.valueApproved = competence.valueApproved;
      return competenceDto;
    }
  }
}
