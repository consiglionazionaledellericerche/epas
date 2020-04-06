package manager.competences;

/**
 * Dto per le timetable dei turni.
 * @author dario
 *
 */
public class ShiftTimeTableDto {

  public long id;
  public String calculationType;
  public String startMorning;
  public String endMorning;
  public String startAfternoon;
  public String endAfternoon;
  public String startMorningLunchTime;
  public String endMorningLunchTime;
  public String startAfternoonLunchTime;
  public String endAfternoonLunchTime;
  public String startEvening;
  public String endEvening;
  public String startEveningLunchTime;
  public String endEveningLunchTime;
  public boolean isOfficeTimeTable;
}
