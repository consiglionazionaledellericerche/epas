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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import play.data.validation.Required;

/**
 * Rappresenta un giorno di reperibilità di una persona reperibile.
 *
 * @author Cristian Lucchesi
 */
@Getter
@Setter
@Audited
@Entity
@Table(
    name = "person_reperibility_days",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"person_reperibility_id", "date"})})
public class PersonReperibilityDay extends BaseModel {

  private static final long serialVersionUID = 6170327692153445002L;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_reperibility_id", nullable = false)
  private PersonReperibility personReperibility;

  @Required
  private LocalDate date;

  @Column(name = "holiday_day")
  private Boolean holidayDay;

  @ManyToOne
  @JoinColumn(name = "reperibility_type")
  private PersonReperibilityType reperibilityType;
  
  @Transient
  public String getLabel() {
    return this.date.dayOfMonth().getAsText() + " " + this.date.monthOfYear().getAsText();
  }

}
