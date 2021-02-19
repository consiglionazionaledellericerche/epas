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
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import models.base.BaseModel;
import models.enumerate.CalculationType;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

/**
 * Nuova relazione tra timetable delle organizzazioni e sedi.
 *
 * @author Dario Tagliaferri
 */
@Audited
@Entity
public class OrganizationShiftTimeTable extends BaseModel {

  private static final long serialVersionUID = 8292047096977861290L;

  public String name;
  
  @OneToMany(mappedBy = "shiftTimeTable", fetch = FetchType.EAGER)
  public Set<OrganizationShiftSlot> organizationShiftSlot;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;
  
  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "calculation_type")
  public CalculationType calculationType;
  
  @OneToMany(mappedBy = "shiftTimeTable")
  public List<ShiftType> shiftTypes = new ArrayList<>();
  
  public boolean considerEverySlot = true;
  
  
  /**
   * Il numero degli slot.
   *
   * @return quanti slot ci sono.
   */
  @Transient
  public long slotCount() {
    long slots = this.organizationShiftSlot.stream()
        .filter(s -> s.beginSlot != null && s.endSlot != null).count();
    return slots;
  }
}
