/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

package manager.services.absences.model;

import com.google.common.collect.ImmutableList;
import java.util.List;
import models.enumerate.VacationCode;

/**
 * Rappresenta la progessione temporale di maturazione delle ferie.
 */
public enum YearProgression {
  
  vacation32(32, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 3),
      new YearPortion(76, 106, 3),
      new YearPortion(107, 136, 2),
      new YearPortion(137, 167, 3),
      new YearPortion(168, 197, 3),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 3),
      new YearPortion(259, 288, 3),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 349, 3),
      new YearPortion(350, 366, 3))),
  
  vacation30(30, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 3),
      new YearPortion(76, 106, 2),
      new YearPortion(107, 136, 3),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 3),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 3),
      new YearPortion(259, 288, 2),
      new YearPortion(289, 319, 3),
      new YearPortion(320, 349, 2),
      new YearPortion(350, 366, 3))),
  
  vacation28(28, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 2),
      new YearPortion(76, 106, 3),
      new YearPortion(107, 136, 2),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 3),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 2),
      new YearPortion(259, 288, 3),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 349, 2),
      new YearPortion(350, 366, 3))),
  
  vacation27(27, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 2),
      new YearPortion(76, 106, 3),
      new YearPortion(107, 136, 2),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 3),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 2),
      new YearPortion(259, 288, 3),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 349, 2),
      new YearPortion(350, 366, 2))),
  
  vacation26(26, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 2),
      new YearPortion(76, 106, 2),
      new YearPortion(107, 136, 2),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 3),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 2),
      new YearPortion(259, 288, 2),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 349, 2),
      new YearPortion(350, 366, 3))),
  
  vacation25(25, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 2),
      new YearPortion(76, 106, 2),
      new YearPortion(107, 136, 2),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 2),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 2),
      new YearPortion(259, 288, 2),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 349, 2),
      new YearPortion(350, 366, 3))),
  
  vacation23(23, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 2),
      new YearPortion(76, 106, 3),
      new YearPortion(107, 136, 2),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 2),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 288, 2),
      new YearPortion(289, 319, 1),
      new YearPortion(320, 349, 2),
      new YearPortion(350, 366, 2))),
  
  vacation22(22, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 1),
      new YearPortion(76, 106, 3),
      new YearPortion(107, 136, 1),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 2),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 288, 3),
      new YearPortion(289, 319, 1),
      new YearPortion(320, 349, 2),
      new YearPortion(350, 366, 2))),
  
  vacation21(21, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 1),
      new YearPortion(76, 106, 2),
      new YearPortion(107, 136, 1),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 2),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 2),
      new YearPortion(259, 288, 1),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 349, 1),
      new YearPortion(350, 366, 3))),

  vacation20(20, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 75, 1),
      new YearPortion(76, 106, 2),
      new YearPortion(107, 136, 1),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 2),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 2),
      new YearPortion(259, 288, 1),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 349, 1),
      new YearPortion(350, 366, 2))),

  vacation17(17, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 1),
      new YearPortion(46, 75, 1),
      new YearPortion(76, 106, 2),
      new YearPortion(107, 136, 1),
      new YearPortion(137, 167, 2),
      new YearPortion(168, 197, 1),
      new YearPortion(198, 227, 2),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 288, 2),
      new YearPortion(289, 319, 1),
      new YearPortion(320, 349, 1),
      new YearPortion(350, 366, 2))),
  
  vacation16(16, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 1),
      new YearPortion(46, 75, 1),
      new YearPortion(76, 106, 2),
      new YearPortion(107, 136, 1),
      new YearPortion(137, 167, 1),
      new YearPortion(168, 197, 2),
      new YearPortion(198, 227, 1),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 288, 1),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 349, 1),
      new YearPortion(350, 366, 2))),
  
  vacation15(15, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 1),
      new YearPortion(46, 75, 1),
      new YearPortion(76, 106, 2),
      new YearPortion(107, 136, 1),
      new YearPortion(137, 167, 1),
      new YearPortion(168, 197, 2),
      new YearPortion(198, 227, 1),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 288, 1),
      new YearPortion(289, 319, 1),
      new YearPortion(320, 349, 1),
      new YearPortion(350, 366, 2))),
  
  vacation11(11, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 1),
      new YearPortion(46, 75, 1),
      new YearPortion(76, 106, 1),
      new YearPortion(107, 167, 1),
      new YearPortion(168, 227, 2),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 288, 1),
      new YearPortion(289, 319, 1),
      new YearPortion(320, 349, 1),
      new YearPortion(350, 366, 1))),

  vacation10(10, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 1),
      new YearPortion(46, 106, 1),
      new YearPortion(107, 136, 1),
      new YearPortion(137, 167, 1),
      new YearPortion(168, 197, 1),
      new YearPortion(198, 227, 1),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 319, 1),
      new YearPortion(320, 349, 1),
      new YearPortion(350, 366, 1))),
  
  vacation9(9, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 1),
      new YearPortion(46, 106, 1),
      new YearPortion(107, 136, 1),
      new YearPortion(137, 167, 1),
      new YearPortion(168, 197, 1),
      new YearPortion(198, 227, 1),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 319, 1),
      new YearPortion(320, 366, 1))),
  
  vacation13(13, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 106, 2),
      new YearPortion(107, 167, 2),
      new YearPortion(168, 227, 2),
      new YearPortion(228, 288, 2),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 366, 1))),
  
  vacation14(14, ImmutableList.of(
      new YearPortion(1, 15, 0),
      new YearPortion(16, 45, 2),
      new YearPortion(46, 106, 2),
      new YearPortion(107, 167, 3),
      new YearPortion(168, 227, 2),
      new YearPortion(228, 258, 1),
      new YearPortion(259, 288, 1),
      new YearPortion(289, 319, 2),
      new YearPortion(320, 366, 1))),
  
  permission4(4, ImmutableList.of(
      new YearPortion(1, 44, 0),
      new YearPortion(45, 135, 1),
      new YearPortion(136, 225, 1),
      new YearPortion(226, 315, 1),
      new YearPortion(316, 366, 1))),
  
  permission3(3, ImmutableList.of(
      new YearPortion(1, 44, 0),
      new YearPortion(45, 135, 1),
      new YearPortion(136, 315, 1),
      new YearPortion(316, 366, 1))),
  
  permission2(2, ImmutableList.of(
      new YearPortion(1, 44, 0),
      new YearPortion(45, 225, 1),
      new YearPortion(226, 366, 1))),
  
  permission1(1, ImmutableList.of(
      new YearPortion(1, 225, 0),
      new YearPortion(226, 366, 1)));

  public List<YearPortion> yearPortions;
  public int total;
  
  private YearProgression(int total, ImmutableList<YearPortion> yearPortions) {
    this.total = total;
    this.yearPortions = yearPortions;
  }
  
  /**
   * Rappresenta una porzione di anno (un periodo temporale all'interno di un anno).
   */
  public static class YearPortion {
    public final int from;
    public final int to;
    public final int days;
    public final int amount;
    
    /**
     * Costruttore.
     *
     * @param from giorno di inizio
     * @param to giorno di fine
     * @param amount quantità di giorni
     */
    public YearPortion(int from, int to, int amount) {
      this.from = from;
      this.to = to;
      this.amount = amount;
      this.days = to - from + 1;
    }
  }
  
  /**
   * Ritorna la progressione dei giorni di ferie in base al vacationCode.
   *
   * @param vacationCode il vacationCode da considerare
   * @return la progressione dei giorni di ferie in base al vacationCode.
   */
  public static YearProgression whichVacationProgression(VacationCode vacationCode) {
    for (YearProgression yearProgression : YearProgression.values()) {
      if (yearProgression.total == vacationCode.vacations) {
        return yearProgression;
      }
    }
    return null; //throw new IllegalStateException();
  }
  
  /**
   * Ritorna la progressione dei giorni di ex P.L. in base al vacationCode.
   *
   * @param vacationCode il vacationCode da considerare
   * @return la progressione dei giorni di ex P.L. in base al vacationCode.
   */
  public static YearProgression whichPermissionProgression(VacationCode vacationCode) {
    for (YearProgression yearProgression : YearProgression.values()) {
      if (yearProgression.total == vacationCode.permissions) {
        return yearProgression;
      }
    }
    return null; //throw new IllegalStateException();
  }

}