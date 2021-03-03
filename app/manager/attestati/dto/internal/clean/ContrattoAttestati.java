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

package manager.attestati.dto.internal.clean;

import lombok.Builder;
import org.joda.time.LocalDate;

/**
 * Modella i dati contrattuali in attestati. Questo dto contiene le informazioni consumate da
 * ePAS e viene generato a ripulendo i dati forniti dagli endPoint interni di attestati.
 *
 * @author Alessandro Martelli
 *
 */
@Builder
public class ContrattoAttestati {

  public String matricola;
  public LocalDate beginContract;
  public LocalDate endContract;

  //Tipologia?
  //Parttime?

}