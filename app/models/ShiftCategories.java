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
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Categoria di turno.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "shift_categories")
public class ShiftCategories extends BaseModel {

  private static final long serialVersionUID = 3156856871540530483L;

  @Required
  @Unique
  private String description;

  /**
   * responsabile della categoria turno.
   */
  @ManyToOne(optional = false)
  @JoinColumn(name = "supervisor")
  @Required
  private Person supervisor;
  
  private boolean disabled;
  
  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "office_id")
  @NotNull
  private Office office; 
  
  @NotAudited
  @OneToMany(mappedBy = "shiftCategories")
  private List<ShiftType> shiftTypes = new ArrayList<ShiftType>();
  
  @ManyToMany
  private List<Person> managers = Lists.newArrayList();
  
  @Override
  public String toString() {
    return String.format("ShiftCategory[%d] - description = %s", id, description);
  }
}

