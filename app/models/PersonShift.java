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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import models.base.PeriodModel;

/**
 * Associazione di una persona ad una tipologia di turno.
 */
@Entity
@Table(name = "person_shift")
public class PersonShift extends PeriodModel {

  private static final long serialVersionUID = 651448817233184716L;

  public String description;

  @ManyToOne
  @JoinColumn(name = "person_id", nullable = false, updatable = false)
  public Person person;

  @OneToMany(mappedBy = "personShift")
  public List<PersonShiftShiftType> personShiftShiftTypes;

  @OneToMany(mappedBy = "personShift")
  public List<PersonShiftDay> personShiftDays = new ArrayList<>();

  public boolean disabled;

  @Override
  public String toString() {
    return person.name + " " + person.surname;
  }

}
