package manager.services.absences.model;

import com.google.common.collect.ImmutableList;

import java.util.List;

import models.enumerate.VacationCode;

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
      new YearPortion(226, 336, 1)));

  public List<YearPortion> yearPortions;
  public int total;
  
  private YearProgression(int total, ImmutableList<YearPortion> yearPortions) {
    this.total = total;
    this.yearPortions = yearPortions;
  }
  
  public static class YearPortion {
    public final int from;
    public final int to;
    public final int days;
    public final int amount;
    
    public YearPortion(int from, int to, int amount) {
      this.from = from;
      this.to = to;
      this.amount = amount;
      this.days = to - from + 1;
    }
  }
  
  public static YearProgression whichVacationProgression(VacationCode vacationCode) {
    for (YearProgression yearProgression : YearProgression.values()) {
      if (yearProgression.total == vacationCode.vacations) {
        return yearProgression;
      }
    }
    throw new IllegalStateException();
  }
  
  public static YearProgression whichPermissionProgression(VacationCode vacationCode) {
    for (YearProgression yearProgression : YearProgression.values()) {
      if (yearProgression.total == vacationCode.permissions) {
        return yearProgression;
      }
    }
    throw new IllegalStateException();
  }

}