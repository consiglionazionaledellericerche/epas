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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

/**
 * Classe dei tipi di giustificazione delle assenze.
 *
 * @author dario
 *
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "justified_types")
public class JustifiedType extends BaseModel {

  private static final long serialVersionUID = -3532986170397408935L;

  /**
   * Enumerato che gestisce i nomi dei tipi di giustificazione dell'assenza.
   *
   * @author dario
   */
  public enum JustifiedTypeName {

    nothing,

    // il tempo giustificato è definito dal tipo di assenza
    absence_type_minutes,
    half_day,
    all_day,
    all_day_percentage, // giustifica un giorno, scala una percentuale (ex. 661G)

    // il tempo giustificato è specificato nell'assenza.
    specified_minutes,
    missing_time,
    
    // assegna il tempo a lavoro come timbrature (ex telelavoro)
    assign_all_day,

    // scala una giornata dal limite (ex. congedi altro coniuge)
    all_day_limit,
        
    // i minuti specificati scalano dal limite e non dal tempo a lavoro (ex. permessi brevi)
    specified_minutes_limit,
    
    
    // altri (documentare)
    complete_day_and_add_overtime,
    recover_time;

  }

  @Getter
  @Column
  @Enumerated(EnumType.STRING)
  private JustifiedTypeName name;

  @ManyToMany(mappedBy = "justifiedTypesPermitted")
  private Set<AbsenceType> absenceTypes;

  @OneToMany(mappedBy = "justifiedType")
  private List<Absence> absences = Lists.newArrayList();

  @Override
  public String toString() {
    return this.name.name();
  }

}
