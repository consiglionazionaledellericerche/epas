package manager.recaps.trainingHours;

import org.joda.time.LocalDate;

/**
 * 
 * @author dario
 *
 */
public class DayAndHourRecap {
  
  public LocalDate begin;
  public LocalDate end;
  public Integer trainingHours;

  @Override
  public String toString() {
    return "Da: " + begin.toString() + " A: " + end.toString() + " " + trainingHours + "ore"; 
  }
}
