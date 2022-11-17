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

package models.enumerate;

import com.google.common.collect.Range;
import models.Qualification;

/**
 * Differenziazione tra livelli 1-3 e 4-10.
 *
 */
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
    return qualifiche.contains(qualification.getQualification());
  }

}
