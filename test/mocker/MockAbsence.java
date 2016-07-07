package mocker;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.Builder;

import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;

public class MockAbsence {
  
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
