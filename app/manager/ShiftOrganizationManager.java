package manager;

import org.joda.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import models.CompetenceCode;
import models.Person;
import models.PersonShiftDay;
import models.ShiftType;
import models.ShiftTypeMonth;

@Slf4j
public class ShiftOrganizationManager {

  private void recalculate(PersonShiftDay personShiftDay) {
    
  }
  
  private void saveCompetence(Person person, ShiftTypeMonth shiftTypeMonth, 
      CompetenceCode shiftCode, Integer calculatedCompetences) {    
  }
  
  public void checkShiftValid(PersonShiftDay personShiftDay) {
    
  }
  
  public int calculatePersonShiftCompetencesInPeriod(ShiftType activity, Person person,
      LocalDate from, LocalDate to, boolean holiday) {
    return 0;
  }
}
