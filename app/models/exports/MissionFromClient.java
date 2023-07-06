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

package models.exports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import models.Person;
import org.joda.time.LocalDateTime;

/**
 * Esportazione delle informazioni relative alla missione.
 *
 * @author Dario Tagliaferri
 */
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MissionFromClient {

  public String tipoMissione;
  public String destinazioneMissione;
  public String codiceSede;
  public Long id;
  public Person person;
  public String matricola;
  public LocalDateTime dataInizio;
  public LocalDateTime dataFine;
  public Long idOrdine;
  public int anno;
  public Long numero;
  public Boolean destinazioneNelComuneDiResidenza;

}