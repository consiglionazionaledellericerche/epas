package models.enumerate;

/**
 * Tipologie di tempo di lavoro giustificato Utilizzate nelle AbsenceType per modellare le diverse
 * tipologie.
 *
 * @author cristian
 * @author dario
 */
public enum JustifiedTimeAtWork {

  AllDay(null, null),
  HalfDay(null, null),
  OneHour(60, false),
  TwoHours(120, false),
  ThreeHours(180, false),
  FourHours(240, false),
  FiveHours(300, false),
  SixHours(360, false),
  SevenHours(420, false),
  EightHours(480, false),
  OneHourMealTimeCounting(60, true),
  TwoHoursMealTimeCounting(120, true),
  ThreeHoursMealTimeCounting(180, true),
  FourHoursMealTimeCounting(240, true),
  FiveHoursMealTimeCounting(300, true),
  SixHoursMealTimeCounting(360, true),
  SevenHoursMealTimeCounting(420, true),
  EightHoursMealTimeCounting(480, true),
  Nothing(0, false),
  TimeToComplete(null, null),
  ReduceWorkingTimeOfTwoHours(null, null),
  AssignAllDay(null, null);

  public Integer minutes;
  public Boolean mealTimeCounting;

  private JustifiedTimeAtWork(Integer minutes, Boolean mealTimeCounting) {
    this.minutes = minutes;
    this.mealTimeCounting = mealTimeCounting;
  }

  public boolean isFixedJustifiedTime() {
    return minutes != null;
  }

}
