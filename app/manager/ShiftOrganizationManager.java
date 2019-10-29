package manager;

import java.util.List;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import lombok.extern.slf4j.Slf4j;
import models.CompetenceCode;
import models.Office;
import models.OrganizationShiftSlot;
import models.OrganizationShiftTimeTable;
import models.Person;
import models.PersonShiftDay;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.dto.OrganizationTimeTable;
import models.enumerate.CalculationType;

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
  
  public String generateTimeTableAndSlot(List<OrganizationTimeTable> list, 
      Office office, CalculationType calculationType, String name) {
    String result = "";
    OrganizationShiftTimeTable shiftTimeTable = null;
    try {      
      shiftTimeTable = new OrganizationShiftTimeTable();
      shiftTimeTable.name = name;
      shiftTimeTable.calculationType = calculationType;
      shiftTimeTable.office = office;
      shiftTimeTable.save();
    } catch(Exception e) {
      result = "Errore in creazione della timetable";
    }
    try {
      for (OrganizationTimeTable ott : list) {
        if (ott == null) {
          continue;
        }
        OrganizationShiftSlot shiftSlot = new OrganizationShiftSlot();      
        LocalTime begin = LocalTime.parse(ott.beginSlot);
        LocalTime end = LocalTime.parse(ott.endSlot);
        shiftSlot.beginSlot = begin;
        shiftSlot.endSlot = end;
        LocalTime beginMeal = null;
        LocalTime endMeal = null;
        if (ott.isMealActive) {
          beginMeal = LocalTime.parse(ott.beginMealSlot);
          endMeal = LocalTime.parse(ott.endMealSlot);
        }
        shiftSlot.beginMealSlot = beginMeal;
        shiftSlot.endMealSlot = endMeal;
        shiftSlot.minutesPaid = ott.minutesPaid;
        shiftSlot.minutesSlot = ott.minutesSlot;
        shiftSlot.shiftTimeTable = shiftTimeTable;
        shiftSlot.save();
      }
    } catch (Exception e) {
      result = "Errore nella creazione degli slot di turno";
    }
    
    return result;
  }
  
  
}
