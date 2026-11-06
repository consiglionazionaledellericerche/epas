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

import java.util.List;

/**
 * DTO con lo stato di validazione degli attestati mensili dei dipendenti di una sede.
 * GET /api/ext/dipendenti/statoValida/{codiceSede}/{anno}/{mese}
 */
public class StatoValidazioneDipendenti {

  public String codiceSede;
  public int anno;
  public int mese;
  // Senza inizializzazione: se Attestati non restituisce il campo resta null e la
  // risposta viene trattata come errore invece che come sede senza validazioni.
  public List<ValidazioneDipendente> dipendenti;

  /**
   * Stato di validazione dell'attestato mensile di un dipendente.
   */
  public static class ValidazioneDipendente {
    public String matricola;
    public boolean validato;
  }

}
