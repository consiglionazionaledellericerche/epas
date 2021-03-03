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

import play.data.validation.Required;

/**
 * Rappresentazione di una time table per i turni.
 */
public class TimeTableDto {

  @Required
  public String startMorning;

  @Required
  public String endMorning;

  @Required
  public String startAfternoon;

  @Required
  public String endAfternoon;
  
  
  public String startEvening;
  
  
  public String endEvening;

  @Required
  public String startMorningLunchTime;

  @Required
  public String endMorningLunchTime;

  @Required
  public String startAfternoonLunchTime;

  @Required
  public String endAfternoonLunchTime;
  
  
  public String startEveningLunchTime;

  
  public String endEveningLunchTime;

  @Required
  public Integer totalWorkMinutes;

  @Required
  public Integer paidMinutes;
   
  
}
