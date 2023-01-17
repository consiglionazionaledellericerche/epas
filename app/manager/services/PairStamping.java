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

package manager.services;

import it.cnr.iit.epas.DateUtility;
import models.Stamping;
import models.enumerate.StampTypes;

/**
 * Classe che modella due stampings logicamente accoppiate nel PersonDay. (una di ingresso ed una di
 * uscita).
 */
public class PairStamping {

  /**
   * Id univoco alla sequenza..
   */
  private static int SEQUENCE_ID = 1;
  
  public Stamping first;
  public Stamping second;
  
  public int timeInPair = 0;

  /**
   * Coppia di timbrature per pranzo. CNR centrale.
   */
  public boolean prPair = false;
  
  /**
   * Costruisce la coppia di timbrature.
   *
   * @modify in.pairId e out.pairId se appartengono ad una coppia valida. 
   * @param first ingresso
   * @param second uscita
   */
  public PairStamping(Stamping first, Stamping second) {

    this.first = first;
    this.second = second;

    timeInPair = 0;
    timeInPair = timeInPair - DateUtility.toMinute(first.getDate());
    timeInPair = timeInPair + DateUtility.toMinute(second.getDate());
    
    //La coppia valida la imposto nel caso di coppia definitiva (non contenente l'uscita fittizia
    // e se si tratta di una coppia in-out, il caso out-in è usato nel calcolo del buono pasto.
    if (!second.exitingNow && first.isIn() && second.isOut()) {
      int pairId = SEQUENCE_ID++;
      first.pairId = pairId;
      second.pairId = pairId;
    }

    // TODO: decidere se entrambe o almeno una.
    if ((first.getStampType() != null && first.getStampType().equals(StampTypes.PAUSA_PRANZO))
        || (second.getStampType() != null 
        && second.getStampType().equals(StampTypes.PAUSA_PRANZO))) {
      prPair = true;
    }
  }
  
  public String toString() {
    return String.format("[%s,%s]", first.getDate().toString("HH:mm:ss"),
        second.getDate().toString("HH:mm:ss"));
  }

}
