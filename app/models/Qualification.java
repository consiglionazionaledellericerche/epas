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

import com.google.common.collect.Lists;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.absences.AbsenceType;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;


/**
 * Qualifiche CNR (livello 1, 2, 3, 4, ...).
 *
 * @author Dario Tagliaferri
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "qualifications")
public class Qualification extends BaseModel {

  private static final long serialVersionUID = 7147378609067987191L;

  @OneToMany(mappedBy = "qualification")
  private List<Person> persons = Lists.newArrayList();

  @ManyToMany(mappedBy = "qualifications")
  private List<AbsenceType> absenceTypes = Lists.newArrayList();

  @Getter
  @Setter
  @Required
  private int qualification;

  @Required
  private String description;

  /**
   * I livelli I-III.
   */
  @Transient
  public boolean isTopQualification() {
    return qualification <= 3;
  }
  
  @Override
  public String getLabel() {
    return this.description;
  }

  @Override
  public String toString() {
    return getLabel();
  }

  
}
