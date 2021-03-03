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

/**
 * Oggetto per l'esportazione di un periodo di assenza per una certa persona relativa a un
 * certo codice.
 *
 * @author Dario Tagliaferri
 * @author alessandro Martelli
 * @author arianna Del Soldato
 */
public class PersonPeriodAbsenceCode {

  public long personId;
  public String name;
  public String surname;
  public String code;
  public String start;
  public String end;
}
