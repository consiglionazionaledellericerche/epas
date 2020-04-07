package models.exports;

import java.util.ArrayList;
import java.util.List;

/**
 * I periodi di reperibilità.
 * @author cristian
 */
public class ReperibilityPeriods {

  public List<ReperibilityPeriod> periods = new ArrayList<ReperibilityPeriod>();

  public ReperibilityPeriods(List<ReperibilityPeriod> periods) {
    this.periods = periods;
  }
}
