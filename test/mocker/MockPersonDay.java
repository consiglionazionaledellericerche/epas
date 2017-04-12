package mocker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import lombok.Builder;

import models.PersonDay;
import models.Stamping;

import org.joda.time.LocalDate;

public class MockPersonDay {
  
  @Builder
  public static PersonDay personDay(
      LocalDate date, List<Stamping> stampings) {
    
    PersonDay personDay = mock(PersonDay.class);
    when(personDay.getDate()).thenReturn(date);
   
    return personDay;
  }
  
 
}
