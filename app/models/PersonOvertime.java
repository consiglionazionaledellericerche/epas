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
package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import models.flows.Group;

/**
 * Oggetto che modella il calcolo totale degli straordinari per persona.
 *
 * @author Dario Tagliaferri
 */

@Getter
@Setter
@Entity
@Audited
@Table(name = "person_overtimes")
public class PersonOvertime extends BaseModel{

  @NotNull
  private LocalDate dateOfUpdate;

  private Integer numberOfHours;

  private Integer year;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  private Person person;
}
