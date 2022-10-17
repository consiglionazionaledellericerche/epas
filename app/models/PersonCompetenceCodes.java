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
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.base.PeriodModel;
import play.data.validation.Required;

/**
 * Relazione tra persona e competenze abilitate.
 *
 * @author Dario Tagliaferri
 */
@Getter
@Setter
@Entity
@Table(name = "persons_competence_codes")
public class PersonCompetenceCodes extends PeriodModel {

  private static final long serialVersionUID = 1769306446762966211L;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_id")
  private Person person;
  
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "competence_code_id")
  private CompetenceCode competenceCode;

  @Override
  public String toString() {
    return String.format(
        "PersonCompetenceCodes[%d] - person.name = %s, competenceCode = %s, "
        + "beginDate = %s, endDate = %s",
         id, person.fullName(), competenceCode.getCode(), getBeginDate(), getEndDate());
  }
}
