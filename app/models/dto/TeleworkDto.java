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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.persistence.Transient;
import lombok.Builder;
import lombok.Data;
import models.enumerate.TeleworkStampTypes;
import org.joda.time.YearMonth;
import play.data.validation.Required;

/**
 * Informazioni sulla timbratura per lavoro fuori sede.
 */
@Data
@Builder
public class TeleworkDto {

  private Long id;

  private long personDayId;

  @Required
  private TeleworkStampTypes stampType;

  private LocalDateTime date;

  private String note;

  /**
   * Utile per effettuare i controlli temporali sulle drools.
   *
   * @return il mese relativo alla data della timbratura.
   */
  public YearMonth getYearMonth() {
    return new YearMonth(date.getYear(), date.getMonthValue());
  }
  
  public boolean isPersistent() {
    return id != null;
  }

  /**
   * Orario formattato come HH:mm.
   *
   * @return orario della timbratura formattato come HH:mm.
   */
  @Transient
  public String formattedHour() {
    if (this.date != null) {
      return date.format(DateTimeFormatter.ofPattern("HH:mm"));
      //return date.toString("HH:mm");
    } else {
      return "";
    }
  }

  @Override
  public String toString() {
    return "Id timbratura: " + id + ", PersonDayId: " + personDayId + ", Causale: " 
        + stampType + ", Data: " + date + ", Note: " + note;
  }
}
