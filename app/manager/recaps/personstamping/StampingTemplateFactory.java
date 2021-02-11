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

package manager.recaps.personstamping;

import javax.inject.Inject;
import manager.cache.StampTypeManager;
import models.Stamping;

/**
 * Factory per StampingTemplate.
 */
public class StampingTemplateFactory {

  private final StampTypeManager stampTypeManager;

  /**
   * Costruttore per l'injection.
   */
  @Inject
  StampingTemplateFactory(StampTypeManager stampTypeManager) {
    this.stampTypeManager = stampTypeManager;
  }

  /**
   * Costruisce l'oggetto che rappresenta una timbratura da visualizzare nel tabellone timbrature.
   *
   * @param stamping timbratura del BaseModel
   * @param position la posizione all'interno della sua coppia.
   * @return la timbratura.
   */
  public StampingTemplate create(Stamping stamping, String position) {

    return new StampingTemplate(stampTypeManager, stamping, position);
  }

}
