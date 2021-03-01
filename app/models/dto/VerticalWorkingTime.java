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

import org.joda.time.DateTimeConstants;
import play.data.validation.Max;
import play.data.validation.Min;

/**
 * Rappresentazione di un orario di lavoro di tipo verticale.
 */
public class VerticalWorkingTime {
  
  @Min(1)
  @Max(DateTimeConstants.HOURS_PER_DAY - 1)
  public int workingTimeHour = 7;

  @Min(0)
  @Max(DateTimeConstants.MINUTES_PER_HOUR - 1)
  public int workingTimeMinute = 12;

  public boolean holiday = false;

  public boolean mealTicketEnabled = true;

  @Max(7)
  public int dayOfWeek;

  @Min(1)
  @Max(23)
  public int mealTicketTimeHour = 6;
  @Min(0)
  @Max(59)
  public int mealTicketTimeMinute = 0;
  @Min(30)
  public int breakTicketTime = 30;
  
  public boolean afternoonThresholdEnabled = false;

  @Min(1)
  @Max(23)
  public int ticketAfternoonThresholdHour = 13;
  @Min(0)
  @Max(59)
  public int ticketAfternoonThresholdMinute = 30;
  @Min(0)
  public int ticketAfternoonWorkingTime = 1;
  
  public String name;

}
