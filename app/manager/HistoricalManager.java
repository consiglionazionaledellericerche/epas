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

package manager;

import models.Contract;
import org.joda.time.LocalDate;

/**
 * Manager per la gestione dello storico delle entity.
 */
public class HistoricalManager {
  
  /**
   * Controllo sulle date passate.
   *
   * @param date1 la prima data da controllare
   * @param date2 la seconda data da controllare
   * @return true se le date sono uguali, false altrimenti.
   */
  public boolean checkDates(LocalDate date1, LocalDate date2) {
    
    if (date1 == null || date2 == null) {
      return false;
    }
    if (!date1.isEqual(date2)) {
      return false;
    }
    return true;
  }
  
  /**
   * Controlla i vari parametri dei due oggetti per verificare che siano identici.
   *
   * @param obj1 il primo oggetto da verificare
   * @param obj2 il secondo oggetto da verificare
   * @return true se i due oggetti sono uguali, false altrimenti.
   */
  public boolean checkObjects(Object obj1, Object obj2) {
    if (obj1 instanceof Contract && obj2 instanceof Contract) {
      Contract c1 = (Contract) obj1;
      Contract c2 = (Contract) obj2;
      if (c1.getPreviousContract() == null && c2.getPreviousContract() != null) {
        return false;
      }
      if (c1.getPreviousContract() != null && c2.getPreviousContract() == null) {
        return false;
      }
      if (c1.getPreviousContract() != null && c2.getPreviousContract() != null
          && c1.getPreviousContract() != c2.getPreviousContract()) {
        return false;
      }
      return true;
    } else {
      //TODO: completare per altre istanze
    }
    return false;
    
  }
}