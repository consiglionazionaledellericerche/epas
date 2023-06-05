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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import models.enumerate.LimitType;
import models.enumerate.LimitUnit;
import org.hibernate.envers.Audited;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * I gruppi servono per descrivere comportamenti e limiti comuni a più
 * codici di competenza.
 *
 * @author Dario Tagliaferri
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "competence_code_groups",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"label"})})
public class CompetenceCodeGroup extends BaseModel {

  private static final long serialVersionUID = 6486248571013912369L;

  @OneToMany(mappedBy = "competenceCodeGroup")
  public List<CompetenceCode> competenceCodes = Lists.newArrayList();

  @Required
  @Unique
  private String label;

  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_type")
  private LimitType limitType;

  @Column(name = "limit_value")
  private Integer limitValue;

  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_unit")
  private LimitUnit limitUnit;
}
