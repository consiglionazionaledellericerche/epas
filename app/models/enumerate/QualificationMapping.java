package models.enumerate;

import com.google.common.collect.Range;

import models.Qualification;

public enum QualificationMapping {

  TECNOLOGI(Range.closed(1, 3)),
  TECNICI(Range.closed(4, 10));

  private Range<Integer> qualifiche;

  QualificationMapping(Range<Integer> range) {
    this.qualifiche = range;
  }

  public Range<Integer> getRange() {
    return qualifiche;
  }

  public boolean contains(Qualification qualification) {
    return qualifiche.contains(qualification.qualification);
  }

}
