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

package models.enumerate;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tipologie di possibili problemi nei controlli effettuati sui turni.
 */
public enum ShiftTroubles {

  //persona non assegnata all'attività
  PERSON_NOT_ASSIGNED,
  //timbratura fuori dalla tolleranza minima in ingresso (turno decurtato di 1 ora)
  MIN_ENTRANCE_TOLERANCE_EXCEEDED,
  //timbratura fuori dalla tolleranza massima in ingresso (turno non valido)
  MAX_ENTRANCE_TOLERANCE_EXCEEDED,
  //timbratura fuori dalla tolleranza minima in uscita (turno decurtato di 1 ora)
  MIN_EXIT_TOLERANCE_EXCEEDED,
  //timbratura fuori dalla tolleranza massima in uscita (turno non valido)
  MAX_EXIT_TOLERANCE_EXCEEDED,
  // pausa fatta durante il turno superiore a quella permessa (turno decurtato di 1 ora)
  MIN_BREAK_TOLERANCE_EXCEEDED,
  // pausa durante il turno superiore alla massima permessa (turno non valido)
  MAX_BREAK_TOLERANCE_EXCEEDED,
  // Superate più soglie di quelle permesse
  TOO_MANY_EXCEEDED_THRESHOLDS,
  // non c'è abbastanza tempo a lavoro per avere il turno
  NOT_ENOUGH_WORKING_TIME,
  // problemi su altro slot
  PROBLEMS_ON_OTHER_SLOT,
  // turno incompleto a causa della mancanza di uno slot
  SHIFT_INCOMPLETED,
  // la persona è assente nel giorno
  PERSON_IS_ABSENT, 
  // eccessiva disparità tra slot di turno
  TOO_MANY_DIFFERENCE_BETWEEN_SLOTS,
  // Giorno futuro
  FUTURE_DAY;

  /**
   * La lista degli errori specifici per un singolo turno.
   *
   * @return la lista degli errori che sono specifici di un singolo turno (non include quelli
   *        derivanti dai controlli sul giorno di turno).
   */
  public static List<ShiftTroubles> shiftSpecific() {
    return Lists.newArrayList(
        MIN_ENTRANCE_TOLERANCE_EXCEEDED,
        MAX_ENTRANCE_TOLERANCE_EXCEEDED,
        MIN_EXIT_TOLERANCE_EXCEEDED,
        MAX_EXIT_TOLERANCE_EXCEEDED,
        MIN_BREAK_TOLERANCE_EXCEEDED,
        MAX_BREAK_TOLERANCE_EXCEEDED,
        TOO_MANY_EXCEEDED_THRESHOLDS,
        NOT_ENOUGH_WORKING_TIME,
        PERSON_IS_ABSENT,
        FUTURE_DAY,
        PERSON_NOT_ASSIGNED);
  }

  /**
   * La lista degli errori che invalidano il turno.
   *
   * @return la lista degli errori che invalidano il turno.
   */
  public static List<ShiftTroubles> invalidatingTroubles() {
    return Lists.newArrayList(
        MAX_ENTRANCE_TOLERANCE_EXCEEDED,
        MAX_EXIT_TOLERANCE_EXCEEDED,
        MAX_BREAK_TOLERANCE_EXCEEDED,
        TOO_MANY_EXCEEDED_THRESHOLDS,
        NOT_ENOUGH_WORKING_TIME,
        PERSON_IS_ABSENT,
        PERSON_NOT_ASSIGNED,
        FUTURE_DAY,
        PROBLEMS_ON_OTHER_SLOT,
        TOO_MANY_DIFFERENCE_BETWEEN_SLOTS,
        SHIFT_INCOMPLETED);
  }
  
  /**
   * La lista degli warning sul turno.
   *
   * @return la lista degli errori che invalidano il turno.
   */
  public static List<ShiftTroubles> warningTroubles() {
    List<ShiftTroubles> troubles = Arrays.stream(values()).collect(Collectors.toList());
    troubles.removeAll(invalidatingTroubles());
    return troubles;
  }
  
  

}
