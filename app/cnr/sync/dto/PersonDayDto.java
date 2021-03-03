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

import com.google.common.collect.Lists;
import java.util.List;

/**
 * DTO per rappresentare via REST i dati riepilogativi del tempo lavorato 
 * comprensivi delle timbrature e dei codici di assenza.
 */
public class PersonDayDto {

  public int tempolavoro;
  public int differenza;
  public int progressivo;
  public boolean buonopasto;
  public List<String> timbrature = Lists.newArrayList();
  public List<String> codiceassenza = Lists.newArrayList();
}
