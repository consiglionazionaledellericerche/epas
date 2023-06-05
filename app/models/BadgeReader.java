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
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import net.sf.oval.constraint.NotNull;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Oggetto che modella i lettori badge.
 *
 * @author Cristian Lucchesi
 */
@Getter
@Setter
@Entity
@Table(name = "badge_readers")
@Audited
public class BadgeReader extends BaseModel {

  private static final long serialVersionUID = -3508739971079270193L;

  @Unique
  @NotNull
  @Required
  private String code;

  private String description;

  private String location;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @OrderBy("code ASC")
  @OneToMany(mappedBy = "badgeReader")
  private Set<Badge> badges = Sets.newHashSet();

  @ManyToMany
  private List<BadgeSystem> badgeSystems = Lists.newArrayList();
  
  @OneToMany(mappedBy = "badgeReader")
  @NotAudited
  private List<Zone> zones = Lists.newArrayList();

  private boolean enabled = true;

  @Override
  public String toString() {
    return this.code;
  }
}
