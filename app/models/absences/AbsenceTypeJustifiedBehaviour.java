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

package models.absences;

import it.cnr.iit.epas.DateUtility;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.absences.JustifiedBehaviour.JustifiedBehaviourName;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

/**
 * Associazione tra tipo di assenza e comportamento nella giustificazione
 * dell'orario giornaliero.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "absence_types_justified_behaviours")
public class AbsenceTypeJustifiedBehaviour extends BaseModel {

  private static final long serialVersionUID = -3532986170397408935L;

  @Required
  @ManyToOne
  @JoinColumn(name = "absence_type_id")
  private AbsenceType absenceType;
  
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "justified_behaviour_id")
  private JustifiedBehaviour justifiedBehaviour;
  
  @Getter
  @Column
  private Integer data;
  
  /**
   * Stampa la quantità di ore e minuti giustificata.
   *
   * @return la stringa in cui stampare la quantità di ore e minuti giustificata.
   */
  public String printData() {
    if (justifiedBehaviour.getName().equals(JustifiedBehaviourName.minimumTime) 
        || justifiedBehaviour.getName().equals(JustifiedBehaviourName.maximumTime)) {
      return DateUtility.fromMinuteToHourMinute(data);
    }
    if (justifiedBehaviour.getName().equals(JustifiedBehaviourName.takenPercentageTime)) {
      return DateUtility.fromMinuteToHourMinute(432 * data / 1000);
    }
    return data + "";
  }

}
