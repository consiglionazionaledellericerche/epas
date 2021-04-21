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

package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.LocalDate;

/**
 * Classe di check per l'inserimento delle assenze.
 *
 */
public class CheckAbsenceInsert {

  public int totalAbsenceInsert;
  public List<LocalDate> dateInTrouble = new ArrayList<LocalDate>();
  public String message;
  public boolean insertInShiftOrReperibility;
  public int howManyAbsenceInReperibilityOrShift;

  /**
   * Costruttore.
   *
   * @param quantity la quantità
   * @param message il messaggio
   * @param insertInShiftOrReperibility se si inserisce l'assenza in turno o reperibilità
   * @param howManyAbsenceInReperibilityOrShift quante assenze in turno o reperibilità
   */
  public CheckAbsenceInsert(
      int quantity, String message, boolean insertInShiftOrReperibility,
      int howManyAbsenceInReperibilityOrShift) {
    this.totalAbsenceInsert = quantity;
    this.message = message;
    this.insertInShiftOrReperibility = insertInShiftOrReperibility;
    this.howManyAbsenceInReperibilityOrShift = howManyAbsenceInReperibilityOrShift;
  }
}
