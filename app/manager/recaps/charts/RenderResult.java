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

package manager.recaps.charts;

import models.enumerate.CheckType;
import org.joda.time.LocalDate;

/**
 * classe privata per la restituzione del risultato relativo al processo di controllo sulle assenze
 * dell'anno passato.
 **/
public class RenderResult {
  public String line;
  public String matricola;
  public String nome;
  public String cognome;
  public String codice;
  public LocalDate data;
  public boolean check;
  public String message;
  public String codiceInAnagrafica;
  public CheckType type;

  /**
   * Costruttore.
   */
  public RenderResult(
      String line, String matricola, String nome, String cognome, String codice,
      LocalDate data, boolean check, String message, String codiceInAnagrafica, CheckType type) {
    this.line = line;
    this.matricola = matricola;
    this.nome = nome;
    this.codice = codice;
    this.cognome = cognome;
    this.data = data;
    this.check = check;
    this.message = message;
    this.codiceInAnagrafica = codiceInAnagrafica;
    this.type = type;

  }
}
