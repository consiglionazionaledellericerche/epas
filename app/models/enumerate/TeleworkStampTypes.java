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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Tipologie di timbrature relative al telelavoro.
 */
@Getter
public enum TeleworkStampTypes {

  INIZIO_TELELAVORO("it", "inizioTelelavoro", "Inizio telelavoro", true),
  FINE_TELELAVORO("ft", "fineTelelavoro", "Fine telelavoro", true),
  INIZIO_PRANZO_TELELAVORO("ipt", "inizioPranzoTelelavoro", "Inizio pranzo telelavoro", true),
  FINE_PRANZO_TELELAVORO("fpt", "finePranzoTelelavoro", "Fine pranzo telelavoro", true),
  INIZIO_INTERRUZIONE("ii", "inizioInterruzione", "Inizio interruzione", true),
  FINE_INTERRUZIONE("fi", "fineInterruzione", "Fine interruzione", true);
  
  private String identifier;
  private String code;
  private String description;
  private boolean isActive;

  TeleworkStampTypes(String identifier, String code, String description, 
      boolean isActive) {
    this.identifier = identifier;
    this.code = code;
    this.description = description;
    this.isActive = isActive;
  }
  
  /**
   * La lista delle causali attive.
   *
   * @return la lista degli stamptypes attivi.
   */
  public static List<TeleworkStampTypes> onlyActive() {
    return Arrays.stream(values())
        .filter(TeleworkStampTypes::isActive).collect(Collectors.toList());
  }
  
  
  /**
   * La lista delle stampTypes associatbili a timbrature in telelavoro.
   *
   * @return la lista di stampTypes associabili a timbrature in telelavoro.
   */
  public static List<TeleworkStampTypes> onlyActiveInTelework() {
    return onlyActive().stream().filter(TeleworkStampTypes::ableForTeleworkStamping)
        .collect(Collectors.toList());
  }
  
  /**
   * La lista di stampTypes di inizio e fine lavoro in telelavoro.
   */
  public static List<TeleworkStampTypes> beginEndTelework() {
    return onlyActive().stream().filter(TeleworkStampTypes::isBeginEndTelework)
        .collect(Collectors.toList());
  }
  
  /**
   * La lista di stampTypes di inizio e fine pranzo in telelavoro.
   */
  public static List<TeleworkStampTypes> beginEndMealInTelework() {
    return onlyActive().stream().filter(TeleworkStampTypes::isBeginEndMealInTelework)
        .collect(Collectors.toList());
  }
  
  /**
   * La lista di stampTypes di inizio e fine interruzione in telelavoro.
   */
  public static List<TeleworkStampTypes> beginEndInterruptionInTelework() {
    return onlyActive().stream().filter(TeleworkStampTypes::isBeginEndInterruptionInTelework)
        .collect(Collectors.toList());
  }
  
  /**
   * Controlla se la causale è abilitata per le timbrature in telelavoro.
   *
   * @return le causali associabili a timbrature in telelavoro
   */
  public boolean ableForTeleworkStamping() {
    return this == FINE_INTERRUZIONE || this == FINE_PRANZO_TELELAVORO 
        || this == FINE_TELELAVORO || this == INIZIO_INTERRUZIONE 
        || this == INIZIO_PRANZO_TELELAVORO || this == INIZIO_TELELAVORO;
  }
  
  public boolean isBeginEndTelework() {
    return this == INIZIO_TELELAVORO || this == FINE_TELELAVORO;
  }
  
  public boolean isBeginEndMealInTelework() {
    return this == INIZIO_PRANZO_TELELAVORO || this == FINE_PRANZO_TELELAVORO;
  }
  
  public boolean isBeginEndInterruptionInTelework() {
    return this == INIZIO_INTERRUZIONE || this == FINE_INTERRUZIONE;
  }
  
  public boolean isEndInTelework() {
    return this == FINE_PRANZO_TELELAVORO || this == FINE_TELELAVORO;
  }
  
  public boolean isBeginInTelework() {
    return this == INIZIO_PRANZO_TELELAVORO || this == INIZIO_TELELAVORO;
  }

}
