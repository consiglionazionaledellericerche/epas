package db.h2support.base;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class WorkingTimeTypeDefinitions {

  public static Integer ONE_HOUR = 60;
  public static Integer TWO_HOUR = 120;
  public static Integer THREE_HOUR = 180;
  public static Integer FOUR_HOUR = 240;
  public static Integer FIVE_HOUR = 300;
  public static Integer SIX_HOUR = 360;
  public static Integer SEVEN_HOUR = 420;

  public enum WorkingDayDefinition {

    Normal_1(1, 432, false, 360, 30, null, null),
    Normal_2(2, 432, false, 360, 30, null, null),
    Normal_3(3, 432, false, 360, 30, null, null),
    Normal_4(4, 432, false, 360, 30, null, null),
    Normal_5(5, 432, false, 360, 30, null, null),
    Normal_6(6, 0, true, 0, 0, null, null),
    Normal_7(7, 0, true, 0, 0, null, null),
    
    PartTime50_1(1, 216, false, 360, 30, null, null),
    PartTime50_2(2, 216, false, 360, 30, null, null),
    PartTime50_3(3, 216, false, 360, 30, null, null),
    PartTime50_4(4, 216, false, 360, 30, null, null),
    PartTime50_5(5, 216, false, 360, 30, null, null),
    PartTime50_6(6, 0, true, 0, 0, null, null),
    PartTime50_7(7, 0, true, 0, 0, null, null);

    public Integer dayOfWeek;
    public Integer workingTime;
    public boolean holiday;
    public Integer mealTicketTime;
    public Integer breakTicketTime;
    public Integer ticketAfternoonThreshold;
    public Integer ticketAfternoonWorkingTime;

    private WorkingDayDefinition(Integer dayOfWeek, Integer workingTime, 
        boolean holiday, Integer mealTicketTime, Integer breakTicketTime, 
        Integer ticketAfternoonThreshold, Integer ticketAfternoonWorkingTime) {
      this.dayOfWeek = dayOfWeek;
      this.workingTime = workingTime;
      this.holiday = holiday;
      this.mealTicketTime = mealTicketTime;
      this.breakTicketTime = breakTicketTime;
      this.ticketAfternoonThreshold = ticketAfternoonThreshold;
      this.ticketAfternoonWorkingTime = ticketAfternoonWorkingTime;
    }

  }

  public enum WorkingDefinition {

    Normal(true, 
        ImmutableList.of(WorkingDayDefinition.Normal_1, WorkingDayDefinition.Normal_2, 
            WorkingDayDefinition.Normal_3, WorkingDayDefinition.Normal_4,
            WorkingDayDefinition.Normal_5, WorkingDayDefinition.Normal_6, 
            WorkingDayDefinition.Normal_7)),
    
    PartTime50(true, 
        ImmutableList.of(WorkingDayDefinition.PartTime50_1, WorkingDayDefinition.PartTime50_2, 
            WorkingDayDefinition.PartTime50_3, WorkingDayDefinition.PartTime50_4,
            WorkingDayDefinition.PartTime50_5, WorkingDayDefinition.PartTime50_6, 
            WorkingDayDefinition.PartTime50_7));

    public boolean horizontal;
    public List<WorkingDayDefinition> orderedWorkingDayDefinition;

    private WorkingDefinition(boolean horizontal, 
        List<WorkingDayDefinition> orderedWorkingDayDefinition) {
      this.horizontal = horizontal;
      this.orderedWorkingDayDefinition = orderedWorkingDayDefinition;
    }
  }


}
