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

package models;

import com.google.common.collect.Range;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.base.IPropertiesInPeriodOwner;
import models.base.PropertyInPeriod;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

/**
 * Tipologia di orario di lavoro associata ad un contratto in un certo periodo temporale.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "contract_stamp_profiles")
public class ContractStampProfile extends PropertyInPeriod {

  private static final long serialVersionUID = 3503562995113282540L;

  @Column(name = "fixed_working_time")
  private boolean fixedworkingtime;

  @ManyToOne
  @JoinColumn(name = "contract_id", nullable = false)
  private Contract contract;

  /**
   * TODO: questa implementazione andrebbe spostata nel PeriodModel
   * Se la data è contenuta nel periodo.
   *
   * @param date data da verificare
   * @return esito
   */
  public boolean includeDate(LocalDate date) {
    if (getBeginDate() == null && getEndDate() == null) {
      //TODO decidere se considerare l'intervallo infinito, oppure nullo
      return false;
    }
    if (getBeginDate() == null) {
      return !getEndDate().isAfter(date);
    }
    if (getEndDate() == null) {
      return !getBeginDate().isBefore(date);
    }
    return !getBeginDate().isBefore(date) && !getEndDate()
        .isAfter(date);
  }

  /**
   * TODO: questa implementazione andrebbe spostata nel PeriodModel.
   * Il range del periodo.
   *
   * @return range
   */
  public Range<LocalDate> dateRange() {
    if (getBeginDate() == null && getEndDate() == null) {
      return Range.all();
    }
    if (getBeginDate() == null) {
      return Range.atMost(getEndDate());
    }
    if (getEndDate() == null) {
      return Range.atLeast(getBeginDate());
    }
    return Range.closed(getBeginDate(), getEndDate());
  }

  @Override
  public IPropertiesInPeriodOwner getOwner() {
    return this.contract;
  }

  @Override
  public void setOwner(IPropertiesInPeriodOwner owner) {
    this.contract = (Contract) owner;
  }

  @Override
  public Object getType() {
    return this.getClass();
  }

  @Override
  public void setType(Object value) {
    // questo metodo in questo caso non serve, perchè i periods sono tutti dello stesso tipo.
  }

  @Override
  public Object getValue() {
    return this.fixedworkingtime;
  }

  @Override
  public void setValue(Object value) {
    this.fixedworkingtime = (Boolean) value;

  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof Boolean) {
      return this.getValue() == (Boolean) otherValue;
    }
    return false;
  }

  @Override
  public String getLabel() {
    return this.fixedworkingtime + "";
  }

}
