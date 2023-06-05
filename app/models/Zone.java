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
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import play.data.validation.Unique;


/**
 * Una zona di timbratura. Utilizzate per calcoli particolari tra zone collegate.
 */
@Getter
@Setter
@Entity
@Table(name = "zones")
public class Zone extends BaseModel {

  private static final long serialVersionUID = 2466096445310199806L;

  @Unique
  @NotNull
  private String name;
  
  private String description;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "badge_reader_id")
  @Nullable
  private BadgeReader badgeReader;
  
  @OneToMany(mappedBy = "zoneBase")
  private List<ZoneToZones> zoneLinkedAsMaster = Lists.newArrayList();
  
  @OneToMany(mappedBy = "zoneLinked")
  private List<ZoneToZones> zoneLinkedAsSlave = Lists.newArrayList();
  
  /* Utilizzata nelle select html per mostrare questa zona.
   * @see models.base.BaseModel#getLabel()
   */
  @Override
  public String getLabel() {
    return name;
  }
  
  @Override
  public String toString() {
    return String.format(
        "Zone[%d] - %s", id, this.name);
  }
}
