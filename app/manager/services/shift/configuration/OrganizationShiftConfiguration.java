package manager.services.shift.configuration;

import com.google.common.collect.Lists;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.TimeInterval;
import java.util.List;
import manager.services.PairStamping;
import models.dto.ShiftComposition;
import org.joda.time.LocalTime;

public enum OrganizationShiftConfiguration {

  INAF_MORNING(new LocalTime(6,0), new LocalTime(14,0), CompetenceCodeDefinition.WORKING_DAY_SHIFT, false),
  INAF_EVENING(new LocalTime(14,0), new LocalTime(22,0), CompetenceCodeDefinition.WORKING_DAY_SHIFT, false),
  INAF_NIGHT(new LocalTime(22,0), new LocalTime(6,0), CompetenceCodeDefinition.NIGHT_SHIFT, false),
  INAF_MORNING_HOLIDAY(new LocalTime(6,0), new LocalTime(14,0), CompetenceCodeDefinition.HOLIDAY_SHIFT, true),
  INAF_EVENING_HOLIDAY(new LocalTime(14,0), new LocalTime(22,0), CompetenceCodeDefinition.HOLIDAY_SHIFT, true),
  INAF_NIGHT_HOLIDAY(new LocalTime(22,0), new LocalTime(6,0), CompetenceCodeDefinition.HOLIDAY_SHIFT, true);
  
  public LocalTime beginSlot;
  public LocalTime endSlot;
  public CompetenceCodeDefinition code;
  public boolean workingDay;
  
  private OrganizationShiftConfiguration(LocalTime beginSlot, LocalTime endSlot, 
      CompetenceCodeDefinition code, boolean workingDay) {
    this.beginSlot = beginSlot;
    this.endSlot = endSlot;
    this.code = code;
    this.workingDay = workingDay;
  }
  
  public List<ShiftComposition> getShiftComposition(List<PairStamping> validPairs) {
    // per ogni coppia di timbrature valida devo controllare in quale slot sta e per quanti minuti
    List<ShiftComposition> list = Lists.newArrayList();
    for (PairStamping validPair : validPairs) {
      TimeInterval stampingInterval = 
          new TimeInterval(validPair.first.date.toLocalTime(), validPair.second.date.toLocalTime());
      for (OrganizationShiftConfiguration conf : OrganizationShiftConfiguration.values()) {
        TimeInterval shiftInterval = new TimeInterval(conf.beginSlot, conf.endSlot);
        if (DateUtility.intervalIntersection(stampingInterval, shiftInterval) != null) {
          TimeInterval interval = DateUtility.intervalIntersection(stampingInterval, shiftInterval);
          list.add(new ShiftComposition(conf, interval.minutesInInterval()));
        }
      }
    }
    return list;
  }
}
