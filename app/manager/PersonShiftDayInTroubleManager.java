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

import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import models.PersonShiftDay;
import models.PersonShiftDayInTrouble;
import models.enumerate.ShiftTroubles;

/**
 * Manager per la gestione dei PersonShiftDayInTrouble.
 */
@Slf4j
public class PersonShiftDayInTroubleManager {

  
  /**
   * Crea il personShiftDayInTrouble per quel giorno (se non esiste già).
   *
   * @param pd    giorno
   * @param cause causa
   */
  public void setTrouble(PersonShiftDay pd, ShiftTroubles cause) {

    for (PersonShiftDayInTrouble pdt : pd.getTroubles()) {
      if (pdt.getCause() == cause) {
        // Se esiste gia' non faccio nulla
        return;
      }
    }

    // Se non esiste lo creo
    PersonShiftDayInTrouble trouble = new PersonShiftDayInTrouble(pd, cause);
    trouble.save();
    pd.getTroubles().add(trouble);

    log.info("Nuovo PersonDayInTrouble {} - {} - {}",
        pd.getPersonShift().getPerson().getFullname(), pd.getDate(), cause);
  }


  /**
   * Metodo per rimuovere i problemi con una determinata causale all'interno del
   * personShiftDay.
   */
  public void fixTrouble(final PersonShiftDay pd, final ShiftTroubles cause) {

    Iterables.removeIf(pd.getTroubles(), pdt -> {
      if (pdt.getCause() == cause) {
        pdt.delete();

        log.info("Rimosso PersonDayInTrouble {} - {} - {}",
            pd.getPersonShift().getPerson().getFullname(), pd.getDate(), cause);
        return true;
      }
      return false;
    });
  }
}