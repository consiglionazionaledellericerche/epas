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

package manager.recaps.traininghours;

import org.joda.time.LocalDate;

/**
 * Recap giorno e ora.
 *
 * @author Dario Tagliaferri
 *
 */
public class DayAndHourRecap {
  
  public LocalDate begin;
  public LocalDate end;
  public Integer trainingHours;

  @Override
  public String toString() {
    return "Da: " + begin.toString() + " A: " + end.toString() + " " + trainingHours + "ore"; 
  }
}
