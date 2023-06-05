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

package models.dto;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import models.PersonDay;
import models.absences.definitions.DefaultAbsenceType;
import models.enumerate.TeleworkStampTypes;

/**
 * Informazioni giornaliere sulle timbrature per lavoro fuori sede.
 */
@Builder
@Data
public class TeleworkPersonDayDto {

  public PersonDay personDay;
  public List<TeleworkDto> beginEnd;
  public List<TeleworkDto> meal;
  public List<TeleworkDto> interruptions;

  /**
   * Verifica se non ci sono timbrature per telelavoro.
   */
  public boolean isEmpty() {
    return beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty();
  }
  
  /**
   * Lista di tutte le timbrature per telelavoro di questo giorno.
   */
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
    return this.personDay.getAbsences().stream()
        .anyMatch(abs -> abs.getAbsenceType().getCode().equals(DefaultAbsenceType.A_103.getCode()));
  }

  /**
   * Controlla se le timbrature in telelavoro sono ben formate.
   *
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
