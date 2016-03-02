package manager;

import models.Office;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.dto.VerticalWorkingTime;

import org.joda.time.DateTimeConstants;

import java.util.List;

public class WorkingTimeTypeManager {
  
  /**
   * associa il giorno all'orario di lavoro.
   * @param wttd il giorno da associare all'orario di lavoro
   * @param wtt l'orario di lavoro a cui associare il giorno
   * @param dayOfWeek il giorno della settimana da persistere
   */
  public void saveWorkingTimeType(
      WorkingTimeTypeDay wttd, WorkingTimeType wtt, int dayOfWeek) {

    wttd.dayOfWeek = dayOfWeek;
    wttd.workingTimeType = wtt;
    wttd.save();
  }

  /**
   * genera l'orario di lavoro verticale e lo persiste sul db.
   * @param list la lista dei dto contenenti le info sugli orari di lavoro
   * @param office l'ufficio a cui associare l'orario di lavoro
   * @param name il nome del nuovo orario di lavoro
   */
  public void saveVerticalWorkingTimeType(List<VerticalWorkingTime> list, 
      Office office, String name) {
    WorkingTimeType wtt = new WorkingTimeType();
    wtt.office = office;
    wtt.horizontal = false;
    wtt.description = name;
    wtt.save();
    
    for (VerticalWorkingTime vwt : list) {
      WorkingTimeTypeDay wttd = new WorkingTimeTypeDay();
      wttd.workingTimeType = wtt;
      wttd.dayOfWeek = vwt.dayOfWeek;
      wttd.holiday = vwt.holiday;
      if (vwt.holiday) {
        wttd.breakTicketTime = 0;
        wttd.mealTicketTime = 0;
        wttd.workingTime = 0;
        wttd.ticketAfternoonThreshold = 0;
        wttd.ticketAfternoonWorkingTime = 0;
      } else {
        wttd.workingTime =
            vwt.workingTimeHour * DateTimeConstants.SECONDS_PER_MINUTE
                    + vwt.workingTimeMinute;
        if (vwt.mealTicketEnabled) {
          wttd.mealTicketTime =
              vwt.mealTicketTimeHour
                          *
                          DateTimeConstants.SECONDS_PER_MINUTE
                          +
                          vwt.mealTicketTimeMinute;
          wttd.breakTicketTime = vwt.breakTicketTime;

          if (vwt.afternoonThresholdEnabled) {
            wttd.ticketAfternoonThreshold =
                vwt.ticketAfternoonThresholdHour
                            *
                            DateTimeConstants.SECONDS_PER_MINUTE
                            +
                            vwt.ticketAfternoonThresholdMinute;
            wttd.ticketAfternoonWorkingTime =
                vwt.ticketAfternoonWorkingTime;
          }
        }
                
      }
      wttd.save();      
      
    }
  }
}
