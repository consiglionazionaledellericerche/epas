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

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.joda.time.LocalDate;


/**
 * DTO che rappresenta le informazioni sommario di timbrature/assenze e
 * tempo a lavoro in un giorno di una specifica persona.
 */
@Builder
@Data
public class PersonDayDto {

  private LocalDate data;
  private String number;
  private int tempoLavoro;
  private int differenza;
  private int progressivo;
  private boolean buonoPasto;
  private boolean giornoLavorativo;
  @Builder.Default
  private List<StampingDto> timbrature = Lists.newArrayList();
  @Builder.Default
  private List<AbsenceDto> codiciAssenza = Lists.newArrayList();
}