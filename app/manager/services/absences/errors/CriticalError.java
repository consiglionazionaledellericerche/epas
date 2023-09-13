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

package manager.services.absences.errors;

import lombok.Builder;
import lombok.ToString;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import org.joda.time.LocalDate;

/**
 * Rappresenta gli errori critici durante la gestione di un'assenza.
 */
@ToString
@Builder
public class CriticalError {
  
  public CriticalProblem criticalProblem;
  public LocalDate date;
  public GroupAbsenceType groupAbsenceType;
  public JustifiedType justifiedType;
  public AbsenceType absenceType;
  public AbsenceType conflictingAbsenceType;
  public Absence absence;
  
  /**
   * Tipolog di errori critici.
   */
  public enum CriticalProblem {

    //Errori 
    UnimplementedTakableComplationGroup,
    
    //Errore generatore form html
    CantInferAbsenceCode,
    WrongJustifiedType,
    CodeNotAllowedInGroup,
    
    //Errori inaspettati
    TwoPeriods,                      //Absence/Group
    IncalcolableJustifiedAmount,     //Absence
    IncalcolableComplationAmount,    //Absence
    OnlyReplacingRuleViolated,       //Group  
    IncalcolableReplacingAmount,     //Group    
    ConflictingReplacingAmount,      //Group
    IncalcolableAdjustment;          

  }
}