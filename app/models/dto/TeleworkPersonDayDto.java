package models.dto;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.val;
import models.PersonDay;
import models.absences.definitions.DefaultAbsenceType;
import models.enumerate.TeleworkStampTypes;


@Builder
@Data
public class TeleworkPersonDayDto {

  public PersonDay personDay;
  public List<TeleworkDto> beginEnd;
  public List<TeleworkDto> meal;
  public List<TeleworkDto> interruptions;

  /**
   * @return true se non ci sono timbrature per telelavoro, 
   *   false altrimenti.
   */
  public boolean isEmpty() {
    return beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty();
  }
  
  public List<TeleworkDto> getTeleworkStampings() {
    List<TeleworkDto> list = Lists.newArrayList(beginEnd);
    list.addAll(meal);
    list.addAll(interruptions);
    return list;
  }
  
  public boolean isBeginEndComplete() {
    return !beginEnd.isEmpty() && beginEnd.size() % 2 == 0;
  }

  public boolean isMealComplete() {
    return !meal.isEmpty() && meal.size() % 2 == 0;
  }

  public boolean isTeleworkInDay() {
    return this.personDay.absences.stream()
        .anyMatch(abs -> abs.absenceType.code.equals(DefaultAbsenceType.A_103.getCode()));
  }

  /**
   * Controlla se le timbrature in telelavoro sono ben formate.
   * @return true se le timbrature di telelavoro sono ben formate, false altrimenti.
   */
  public boolean hasTeleworkStampingsWellFormed() {
    if (this.getTeleworkStampings().size() == 0 
        || this.getTeleworkStampings().size() % 2 != 0) {
      return false;
    }
    List<TeleworkStampTypes> completeDayInTelework = TeleworkStampTypes.beginEndTelework();
    completeDayInTelework.addAll(TeleworkStampTypes.beginEndMealInTelework());
    int count = completeDayInTelework.size();
    for (TeleworkDto tws : this.getTeleworkStampings()) {
      if (completeDayInTelework.contains(tws.getStampType())) {
        count--;
      }
    }
    if (count == 0) {
      return true;
    }
    return false;
  }
}
