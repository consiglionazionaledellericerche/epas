package mocker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.Builder;

import models.PersonDay;
import models.Stamping;

import org.joda.time.LocalDate;

import java.util.List;

public class MockPersonDay {
  
  @Builder
  public static PersonDay personDay(
      LocalDate date, List<Stamping> stampings) {
    
    PersonDay personDay = mock(PersonDay.class);
    when(personDay.getDate()).thenReturn(date);
   
    return personDay;
  }
  
 
}
