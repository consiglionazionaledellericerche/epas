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

package models.dto;

import models.ShiftTimeTable;
import models.ShiftType.ToleranceType;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;


/**
 * Tipologia di servizio di turno.
 */
public class ShiftTypeService {

  @Required
  public String name;
  
  @Required
  public String description;
  
  @Min(0)
  public int entranceTolerance = 0;
  @Min(0)
  public int entranceMaxTolerance = 0;
  @Min(0)
  public int exitTolerance = 0;
  @Min(0)
  public int exitMaxTolerance = 0;
  @Min(0)
  public int breakInShift;
  @Min(0)
  public int breakMaxInShift;
  @Min(0)
  @Max(3)
  
  public Integer maxTolerance = 0;
  
  public ToleranceType toleranceType;
  
  
  public ShiftTimeTable timeTable;
}
