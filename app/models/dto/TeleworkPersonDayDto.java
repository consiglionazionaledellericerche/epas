package models.dto;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import models.PersonDay;
import models.TeleworkStamping;
import models.absences.definitions.DefaultAbsenceType;
import models.enumerate.TeleworkStampTypes;
import org.joda.time.LocalDateTime;


@Builder
@Data
public class TeleworkPersonDayDto {

  public PersonDay personDay;
  public List<TeleworkStamping> beginEnd;
  public List<TeleworkStamping> meal;
  public List<TeleworkStamping> interruptions;

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
    if (this.personDay.teleworkStampings.size() == 0 
        || this.personDay.teleworkStampings.size() % 2 != 0) {
      return false;
    }
    List<TeleworkStampTypes> completeDayInTelework = TeleworkStampTypes.beginEndTelework();
    completeDayInTelework.addAll(TeleworkStampTypes.beginEndMealInTelework());
    int count = completeDayInTelework.size();
    for (TeleworkStamping tws : this.personDay.teleworkStampings) {
      if (completeDayInTelework.contains(tws.stampType)) {
        count--;
      }
    }
    if (count == 0) {
      return true;
    }
    return false;
  }
}
