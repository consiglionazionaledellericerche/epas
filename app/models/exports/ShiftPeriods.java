package models.exports;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arianna
 */
public class ShiftPeriods {

  public List<ShiftPeriod> periods = new ArrayList<ShiftPeriod>();

  public ShiftPeriods(List<ShiftPeriod> periods) {
    this.periods = periods;
  }
}
