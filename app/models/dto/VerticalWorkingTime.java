package models.dto;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;

import java.util.List;

public class VerticalWorkingTime {
  
  @Min(1)
  @Max(DateTimeConstants.HOURS_PER_DAY - 1)
  public int workingTimeHour = 7;

  @Min(0)
  @Max(DateTimeConstants.MINUTES_PER_HOUR - 1)
  public int workingTimeMinute = 12;

  public boolean holiday = false;

  public boolean mealTicketEnabled = true;
  
 
  @Max(7)
  public int dayOfWeek;

  @Min(1)
  @Max(23)
  public int mealTicketTimeHour = 6;
  @Min(0)
  @Max(59)
  public int mealTicketTimeMinute = 0;
  @Min(30)
  public int breakTicketTime = 30;
  
  public boolean afternoonThresholdEnabled = false;

  @Min(1)
  @Max(23)
  public int ticketAfternoonThresholdHour = 13;
  @Min(0)
  @Max(59)
  public int ticketAfternoonThresholdMinute = 30;
  @Min(0)
  public int ticketAfternoonWorkingTime = 1;
  
  @Required
  public String name;
  
  

}
