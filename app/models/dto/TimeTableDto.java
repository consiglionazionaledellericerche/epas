package models.dto;

import org.joda.time.LocalTime;

import play.data.validation.Required;

public class TimeTableDto {

  @Required
  public String startMorning;

  @Required
  public String endMorning;

  @Required
  public String startAfternoon;

  @Required
  public String endAfternoon;
  
  
  public String startEvening;
  
  
  public String endEvening;

  @Required
  public String startMorningLunchTime;

  @Required
  public String endMorningLunchTime;

  @Required
  public String startAfternoonLunchTime;

  @Required
  public String endAfternoonLunchTime;
  
  
  public String startEveningLunchTime;

  
  public String endEveningLunchTime;

  @Required
  public Integer totalWorkMinutes;

  @Required
  public Integer paidMinutes;
   
  
}
