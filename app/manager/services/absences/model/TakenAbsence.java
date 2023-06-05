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

package manager.services.absences.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import models.absences.Absence;
import models.absences.AmountType;

/**
 * Assenza già inserite con le relative informazioni.
 */
@Builder 
@Getter 
@Setter(AccessLevel.PACKAGE)
public class TakenAbsence {
  public Absence absence;
  public AmountType amountType;     //risalibile dal period..
  
  
  public int periodTakableTotal;   //situazione prima della assenza
  public int periodTakenBefore;

  public int takenAmount;
  
  public boolean beforeInitialization; // = false;
  
  public boolean toInsert;          //segnala che è l'assenza da inserire nella chain
  
  /**
   * Se l'assenza non supera i limiti.
   *
   * @return esito
   */
  public boolean canAddTakenAbsence() {
    
    if (beforeInitialization) {
      return true;
    }
    
    if (periodTakableTotal < 0) {
      //TODO: se non c'è limite programmarlo in un booleano
      return true;
    }
    if (periodTakableTotal - periodTakenBefore - takenAmount >= 0) {
      return true;
    }
    return false;
  }
  
  public int periodResidualBefore() {
    return periodTakableTotal - periodTakenBefore;
  }
}
