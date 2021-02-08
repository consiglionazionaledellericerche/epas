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

package models.base;

import java.util.Collection;

/**
 * Il modello è il target del periodo. EX. Contract.
 *
 * @author Alessandro Martelli
 *
 */
public interface IPropertiesInPeriodOwner extends IPeriodModel {

  /**
   * La lista dei periodi del tipo specificato.
   */
  Collection<IPropertyInPeriod> periods(Object type);
  
  Collection<Object> types();
}
