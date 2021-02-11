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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import models.base.BaseModel;
import models.enumerate.LimitType;
import models.enumerate.LimitUnit;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Tabella di decodifica dei codici di competenza.
 *
 * @author Dario Tagliaferri
 */
@Audited
@Entity
@Table(name = "competence_codes")
public class CompetenceCode extends BaseModel {

  private static final long serialVersionUID = 9211205948423608460L;
  
  @NotAudited
  @OneToMany(mappedBy = "workdaysCode")
  public List<MonthlyCompetenceType> workdaysCodes = Lists.newArrayList();
  
  @NotAudited
  @OneToMany(mappedBy = "holidaysCode")
  public List<MonthlyCompetenceType> holidaysCodes = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "competenceCode")
  public List<Competence> competence = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "competenceCode")
  public List<PersonCompetenceCodes> personCompetenceCodes = Lists.newArrayList();

  @ManyToOne
  @JoinColumn(name = "competence_code_group_id")
  public CompetenceCodeGroup competenceCodeGroup;

  @Required
  @Unique
  public String code;

  @Column
  public String codeToPresence;

  @Unique
  @Required
  public String description;

  public boolean disabled;

  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_type")
  public LimitType limitType;

  @Column(name = "limit_value")
  public Integer limitValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "limit_unit")
  public LimitUnit limitUnit;


  @Override
  public String toString() {
    return String.format("%s - %s", code, description);
  }

  @Override
  public String getLabel() {
    return String.format("%s - %s", this.code, this.description);
  }


}
