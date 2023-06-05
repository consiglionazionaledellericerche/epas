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

package manager;

import java.util.List;
import models.Office;
import models.OrganizationShiftSlot;
import models.OrganizationShiftTimeTable;
import models.Person;
import models.ShiftTimeTable;
import models.ShiftType;
import models.dto.OrganizationTimeTable;
import models.enumerate.CalculationType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Manager per la gestione degli ShiftOrganization.
 *
 * @author Dario Tagliaferri
 */
public class ShiftOrganizationManager {
  
  /**
   * Totale competenze in un periodo temporale per una persona ed un tipo di turno.
   */
  public int calculatePersonShiftCompetencesInPeriod(ShiftType activity, Person person,
      LocalDate from, LocalDate to, boolean holiday) {
    return 0;
  }
  
  /**
   * Ritorna una stringa contenente un eventuale errore in creazione di time table e slot.
   *
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
      shiftTimeTable.setName(name);
      shiftTimeTable.setCalculationType(calculationType);
      shiftTimeTable.setConsiderEverySlot(considerEverySlot);
      shiftTimeTable.setOffice(office);
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
        shiftSlot.setBeginSlot(begin);
        shiftSlot.setEndSlot(end);
        LocalTime beginMeal = null;
        LocalTime endMeal = null;
        if (ott.isMealActive) {
          beginMeal = LocalTime.parse(ott.beginMealSlot);
          endMeal = LocalTime.parse(ott.endMealSlot);
        }
        shiftSlot.setBeginMealSlot(beginMeal);
        shiftSlot.setEndMealSlot(endMeal);
        shiftSlot.setMinutesPaid(ott.minutesPaid);
        shiftSlot.setPaymentType(ott.paymentType);
        shiftSlot.setShiftTimeTable(shiftTimeTable);
        shiftSlot.save();
      }
    } catch (Exception e) {
      result = "Errore nella creazione degli slot di turno";
    }
    
    return result;
  }

  /**
   * Ritorna il nome della timetable trasformata secondo la nuova modellazione.
   *
   * @param shiftTimeTable la timetable da trasformare
   * @return il nome della timetable trasformata secondo la nuova modellazione.
   */
  public String transformTimeTableName(ShiftTimeTable shiftTimeTable) {
    if (shiftTimeTable.getShiftTypes().stream()
        .anyMatch(e -> e.getShiftCategories().getOffice().getCodeId().equals("223400"))) {
      return String.format("IIT - %s - %s / %s - %s", 
          shiftTimeTable.getStartEvening(), shiftTimeTable.getEndMorning(), 
          shiftTimeTable.getStartAfternoon(), shiftTimeTable.getEndAfternoon());
    }
    return String.format("%s - %s / %s - %s", shiftTimeTable.getStartMorning(), 
        shiftTimeTable.getEndMorning(), shiftTimeTable.getStartAfternoon(), 
        shiftTimeTable.getEndAfternoon());
  }

}