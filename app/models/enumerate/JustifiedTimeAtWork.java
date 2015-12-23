package models.enumerate;

/**
 * Tipologie di tempo di lavoro giustificato Utilizzate nelle AbsenceType per modellare le diverse
 * tipologie.
 *
 * @author cristian
 * @author dario
 */
public enum JustifiedTimeAtWork {

  AllDay(null),
  HalfDay(null),
  OneHour(60),
  TwoHours(120),
  ThreeHours(180),
  FourHours(240),
  FiveHours(300),
  SixHours(360),
  SevenHours(420),
  EightHours(480),
  Nothing(0),
  TimeToComplete(null),
  ReduceWorkingTimeOfTwoHours(null),
  AssignAllDay(null);

  public Integer minutes;

  private JustifiedTimeAtWork(Integer minutes) {
    this.minutes = minutes;
  }

  public boolean isFixedJustifiedTime() {
    return minutes != null;
  }

}
