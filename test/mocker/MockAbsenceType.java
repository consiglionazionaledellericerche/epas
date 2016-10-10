package mocker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.Builder;

import models.Absence;
import models.AbsenceType;
import models.PersonDay;

import org.joda.time.LocalDate;

public class MockAbsenceType {
  
  @Builder
  public static AbsenceType absenceType(
      String code) {
    
    AbsenceType absenceType = mock(AbsenceType.class);
    when(absenceType.getCode()).thenReturn(code);

    return absenceType;
  }
  
 
}
