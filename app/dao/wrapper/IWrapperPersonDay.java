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

package dao.wrapper;

import com.google.common.base.Optional;
import models.Contract;
import models.PersonDay;
import models.PersonalWorkingTime;
import models.Stamping;
import models.WorkingTimeTypeDay;

/**
 * Oggetto PersonDay con funzionalità aggiuntive.
 */
public interface IWrapperPersonDay extends IWrapperModel<PersonDay> {

  /**
   * Il contratto cui appartiene il person day. Istanzia una variabile Lazy.
   *
   * @return Optional.absent() se non esiste contratto alla data.
   */
  Optional<Contract> getPersonDayContract();

  /**
   * True se il PersonDay cade in un tipo tirmbratura fixed. Istanzia una variabile Lazy.
   */
  boolean isFixedTimeAtWork();

  /**
   * Il tipo orario giornaliero del personDay. Istanzia una variabile Lazy.
   *
   * @return Optional.absent() in caso di mancanza contratto o di tipo orario.
   */
  Optional<WorkingTimeTypeDay> getWorkingTimeTypeDay();
  
  /**
   * L'orario giornaliero personalizzato se esiste.
   *
   * @return Optional.absent() in caso di mancanza di contratto o di tipo orario personale.
   */
  Optional<PersonalWorkingTime> getPersonalWorkingTime();

  /**
   * Il personDay precedente solo se immediatamente consecutivo.
   *
   * @return Optiona.absent() in caso di giorno non consecutivo o primo giorno del contratto
   */
  Optional<PersonDay> getPreviousForNightStamp();

  void setPreviousForNightStamp(Optional<PersonDay> potentialOnlyPrevious);

  /**
   * Il personDay precedente per il calcolo del progressivo.
   */
  Optional<PersonDay> getPreviousForProgressive();

  void setPreviousForProgressive(Optional<PersonDay> potentialOnlyPrevious);

  /**
   * L'ultima timbratura in ordine di tempo nel giorno.
   */
  Stamping getLastStamping();

}