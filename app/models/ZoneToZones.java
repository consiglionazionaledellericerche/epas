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

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import play.data.validation.Min;
import play.data.validation.Required;

/**
 * Associazione tra zone di timbratura.
 */
@Getter
@Setter
@Entity
@Table(name = "zone_to_zones")
public class ZoneToZones extends BaseModel {

  private static final long serialVersionUID = 1252197401101094698L;

  @Required
  @ManyToOne
  @JoinColumn(name = "zone_base_id", updatable = false)
  private Zone zoneBase;
  
  @Required
  @ManyToOne
  @JoinColumn(name = "zone_linked_id", updatable = false)
  private Zone zoneLinked;
  
  @Required
  @Min(1)
  private int delay;  

  @Override
  public String toString() {
    return String.format(
        "Zone[%d] - zone.name = %s, zoneLinked.name= %s, delay = %d",
         id, zoneBase.getName(), zoneLinked.getName(), delay);
  }
}
