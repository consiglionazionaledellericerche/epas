package mocker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.Builder;

import models.Absence;
import models.AbsenceType;
import models.PersonDay;

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
