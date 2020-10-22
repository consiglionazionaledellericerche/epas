package manager;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import models.CompetenceCode;
import models.Office;
import models.OrganizationShiftSlot;
import models.OrganizationShiftTimeTable;
import models.Person;
import models.PersonShiftDay;
import models.ShiftTimeTable;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.dto.OrganizationTimeTable;
import models.enumerate.CalculationType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;


public class ShiftOrganizationManager {
  
  public int calculatePersonShiftCompetencesInPeriod(ShiftType activity, Person person,
      LocalDate from, LocalDate to, boolean holiday) {
    return 0;
  }
  
  /**
   * Ritorna una stringa contenente un eventuale errore in creazione di time table e slot.
   * @param list la lista di oggetti che compongono l'organizationTimeTable
   * @param office la sede
   * @param calculationType il tipo di calcolo della timetable
   * @param name il nome
   * @param considerEverySlot se bisogna considerare tutti gli slot per assegnare il turno
   * @return la stringa contenente eventuali errori in creazione.
   */
  public String generateTimeTableAndSlot(List<OrganizationTimeTable> list, 
      Office office, CalculationType calculationType, String name, boolean considerEverySlot) {
    String result = "";
    OrganizationShiftTimeTable shiftTimeTable = null;
    try {      
      shiftTimeTable = new OrganizationShiftTimeTable();
      shiftTimeTable.name = name;
      shiftTimeTable.calculationType = calculationType;
      shiftTimeTable.considerEverySlot = considerEverySlot;
      shiftTimeTable.office = office;
      shiftTimeTable.save();
    } catch (Exception e) {
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
        shiftSlot.paymentType = ott.paymentType;
        shiftSlot.shiftTimeTable = shiftTimeTable;
        shiftSlot.save();
      }
    } catch (Exception e) {
      result = "Errore nella creazione degli slot di turno";
    }
    
    return result;
  }

  /**
   * Ritorna il nome della timetable trasformata secondo la nuova modellazione.
   * @param shiftTimeTable la timetable da trasformare
   * @return il nome della timetable trasformata secondo la nuova modellazione.
   */
  public String transformTimeTableName(ShiftTimeTable shiftTimeTable) {
    if (shiftTimeTable.shiftTypes.stream()
        .anyMatch(e -> e.shiftCategories.office.codeId.equals("223400"))) {
      return String.format("IIT - %s - %s / %s - %s", 
          shiftTimeTable.startMorning, shiftTimeTable.endMorning, 
          shiftTimeTable.startAfternoon, shiftTimeTable.endAfternoon);
    }
    return String.format("%s - %s / %s - %s", shiftTimeTable.startMorning, 
        shiftTimeTable.endMorning, shiftTimeTable.startAfternoon, shiftTimeTable.endAfternoon);
  }
  
}
