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

import java.util.Set;
import lombok.Builder;
import lombok.ToString;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;

/**
 * DTO per contenere gli errori relativi alle assenze.
 */
@ToString
@Builder
public class AbsenceError {

  public Absence absence;
  public AbsenceProblem absenceProblem;
  public Set<Absence> conflictingAbsences;     //le assenze che conflittano per lo stesso problem
  
}