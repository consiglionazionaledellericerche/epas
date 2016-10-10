package mocker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.Builder;

import models.PersonDay;

import org.joda.time.LocalDate;

public class MockPersonDay {
  
  @Builder
  public static PersonDay personDay(
      LocalDate date) {
    
    PersonDay personDay = mock(PersonDay.class);
    when(personDay.getDate()).thenReturn(date);
    
    return personDay;
  }
  
 
}
