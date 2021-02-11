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

package manager.attestati.dto.show;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * DTO per la rappresentazione della lista dei dipendenti.
 */
public class ListaDipendenti {

  public String codiceSede;
  public int anno;
  public int mese;
  public List<Matricola> dipendenti = Lists.newArrayList();

  /**
   * Rappresenta la Matricola di un dipendente.
   */
  public static class Matricola {
    public String matricola;
  }

}
