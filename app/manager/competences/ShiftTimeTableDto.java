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

package manager.competences;

/**
 * Dto per le timetable dei turni.
 *
 * @author Dario Tagliaferri
 *
 */
public class ShiftTimeTableDto {

  public long id;
  public String calculationType;
  public String startMorning;
  public String endMorning;
  public String startAfternoon;
  public String endAfternoon;
  public String startMorningLunchTime;
  public String endMorningLunchTime;
  public String startAfternoonLunchTime;
  public String endAfternoonLunchTime;
  public String startEvening;
  public String endEvening;
  public String startEveningLunchTime;
  public String endEveningLunchTime;
  public boolean isOfficeTimeTable;
}
