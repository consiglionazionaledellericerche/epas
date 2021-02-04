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

package mocker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.Builder;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import org.joda.time.LocalDate;

public class MockAbsence {
  
  /**
   * Costruisce un'assenza tramite mock.
   */
  @Builder
  public static Absence absence(
      PersonDay personDay,
      LocalDate date,
      AbsenceType absenceType) {
    
    Absence absence = mock(Absence.class);
    when(absence.getPersonDay()).thenReturn(personDay);
    when(absence.getDate()).thenReturn(date);
    when(absence.getAbsenceType()).thenReturn(absenceType);

    return absence;
  }
  
 
}
