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
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import models.base.MutableModel;
import org.hibernate.envers.Audited;
import org.joda.time.YearMonth;
import play.data.validation.Required;

/**
 * Mantiene l'informazione sull'approvazione delle reperibilità
 * di una persona in un determinato mese.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "reperibility_type_month", uniqueConstraints = @UniqueConstraint(columnNames = {
    "person_reperibility_type_id", "year_month"}))
public class ReperibilityTypeMonth extends MutableModel {

  private static final long serialVersionUID = 4745667554574561506L;

  @Required
  @Column(name = "year_month", nullable = false)
  private YearMonth yearMonth;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_reperibility_type_id", nullable = false)
  private PersonReperibilityType personReperibilityType;

  private boolean approved;
}
