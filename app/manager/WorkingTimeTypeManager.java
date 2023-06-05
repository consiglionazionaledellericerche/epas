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

import com.google.common.base.Verify;
import com.google.gdata.util.common.base.Preconditions;
import controllers.WorkingTimes;
import java.util.List;
import models.Office;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.dto.VerticalWorkingTime;
import org.joda.time.DateTimeConstants;

/**
 * Manager per la gestione dei tipi di orario di lavoro.
 */
public class WorkingTimeTypeManager {
  
  /**
   * Associa il giorno all'orario di lavoro.
   *
   * @param wttd il giorno da associare all'orario di lavoro
   * @param wtt l'orario di lavoro a cui associare il giorno
   * @param dayOfWeek il giorno della settimana da persistere
   */
  public void saveWorkingTimeType(
      WorkingTimeTypeDay wttd, WorkingTimeType wtt, int dayOfWeek) {

    wttd.setDayOfWeek(dayOfWeek);
    wttd.setWorkingTimeType(wtt);
    wttd.save();
  }

  /**
   * Genera l'orario di lavoro verticale e lo persiste sul db.
   *
   * @param list la lista dei dto contenenti le info sugli orari di lavoro
   * @param office l'ufficio a cui associare l'orario di lavoro
   * @param name il nome del nuovo orario di lavoro
   */
  public void saveVerticalWorkingTimeType(List<VerticalWorkingTime> list, 
      Office office, String name, boolean reproportionEnabled, String externalId) {
    
    Preconditions.checkState(list.size() == WorkingTimes.NUMBER_OF_DAYS);
    for (int i = 1; i <= WorkingTimes.NUMBER_OF_DAYS; i++) {
      boolean finded = false;
      for (VerticalWorkingTime vwt : list) {
        if (vwt.dayOfWeek == i) {
          finded = true;
        }
      }
      Verify.verify(finded);
    }

    WorkingTimeType wtt = new WorkingTimeType();
    wtt.setOffice(office);
    wtt.setHorizontal(false);
    wtt.setDescription(name);
    wtt.setEnableAdjustmentForQuantity(reproportionEnabled);
    wtt.setExternalId(externalId);
    wtt.save();
    
    for (VerticalWorkingTime vwt : list) {
      WorkingTimeTypeDay wttd = new WorkingTimeTypeDay();
      wttd.setWorkingTimeType(wtt);
      wttd.setDayOfWeek(vwt.dayOfWeek);
      wttd.setHoliday(vwt.holiday);
      if (vwt.holiday) {
        wttd.setBreakTicketTime(0);
        wttd.setMealTicketTime(0);
        wttd.setWorkingTime(0);
        wttd.setTicketAfternoonThreshold(0);
        wttd.ticketAfternoonWorkingTime = 0;
      } else {
        wttd.setWorkingTime(
            vwt.workingTimeHour * DateTimeConstants.SECONDS_PER_MINUTE
                    + vwt.workingTimeMinute);
        if (vwt.mealTicketEnabled) {
          wttd.setMealTicketTime(
              vwt.mealTicketTimeHour
                          *
                          DateTimeConstants.SECONDS_PER_MINUTE
                          +
                          vwt.mealTicketTimeMinute);
          wttd.setBreakTicketTime(vwt.breakTicketTime);

          if (vwt.afternoonThresholdEnabled) {
            wttd.setTicketAfternoonThreshold(
                vwt.ticketAfternoonThresholdHour
                            *
                            DateTimeConstants.SECONDS_PER_MINUTE
                            +
                            vwt.ticketAfternoonThresholdMinute);
            wttd.ticketAfternoonWorkingTime =
                vwt.ticketAfternoonWorkingTime;
          }
        }
                
      }
      wttd.save();      

    }
  }
}