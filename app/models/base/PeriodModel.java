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

package models.base;

import helpers.validators.PeriodEndDateCheck;
import it.cnr.iit.epas.DateInterval;
import java.util.Comparator;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import play.data.validation.CheckWith;
import play.data.validation.Required;

/**
 * Una entity con data di inizio e fine.
 */
@Getter
@Setter
@Audited
@MappedSuperclass
public abstract class PeriodModel extends BaseModel 
    implements IPeriodModel, Comparable<PeriodModel> {

  private static final long serialVersionUID = 701063571599514955L;


  @NotNull
  @Required
  @Column(name = "begin_date")
  private LocalDate beginDate;

  @CheckWith(PeriodEndDateCheck.class)
  @Getter
  @Setter
  @Column(name = "end_date")
  private LocalDate endDate;

  private Comparator<PeriodModel> comparator() {
    return Comparator.comparing(
        PeriodModel::getBeginDate, Comparator.nullsFirst(LocalDate::compareTo))
        .thenComparing(PeriodModel::getId, Comparator.nullsFirst(Long::compareTo));
  }

  @Override
  public int compareTo(PeriodModel other) {
    return comparator().compare(this, other);
  }

  @Override
  public LocalDate calculatedEnd() {
    return endDate;
  }

  @Override
  public DateInterval periodInterval() {
    return new DateInterval(this.getBeginDate(), this.calculatedEnd()); 
  }

}
