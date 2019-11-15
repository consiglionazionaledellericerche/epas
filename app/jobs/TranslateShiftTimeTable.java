package jobs;

import java.lang.reflect.Field;
import java.util.List;
import javax.inject.Inject;
import org.joda.time.LocalTime;
import manager.ShiftOrganizationManager;
import models.OrganizationShiftSlot;
import models.OrganizationShiftTimeTable;
import models.ShiftTimeTable;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import models.enumerate.*;

@OnApplicationStart(async = true)
public class TranslateShiftTimeTable extends Job<Void> {


  @Inject
  private static ShiftOrganizationManager manager;

  @Override
  public void doJob() {
    final List<ShiftTimeTable> list = ShiftTimeTable.findAll();
    for (ShiftTimeTable tt : list) {
      
      OrganizationShiftTimeTable ostt = new OrganizationShiftTimeTable();
      ostt.calculationType = tt.calculationType;
      ostt.office = tt.office;
      ostt.name = manager.transformTimeTableName(tt);
      ostt.save();
      OrganizationShiftSlot slotMorning = new OrganizationShiftSlot();
      OrganizationShiftSlot slotAfternoon = new OrganizationShiftSlot();
      OrganizationShiftSlot slotEvening = new OrganizationShiftSlot();
      if (ostt.name.contains("IIT")) {
        slotMorning.name = "IIT - "+ShiftSlot.MORNING.toString();
      } else {
        slotMorning.name = ShiftSlot.MORNING.toString();
      }      
      slotMorning.beginSlot = tt.startMorning;
      slotMorning.endSlot = tt.endMorning;
      slotMorning.beginMealSlot = tt.startMorningLunchTime;
      slotMorning.endMealSlot = tt.endMorningLunchTime;
      slotMorning.minutesPaid = tt.paidMinutes;
      slotMorning.minutesSlot = tt.totalWorkMinutes/2;
      slotMorning.shiftTimeTable = ostt;
      slotMorning.save();

      if (ostt.name.contains("IIT")) {
        slotMorning.name = "IIT - "+ShiftSlot.AFTERNOON.toString();
      } else {
        slotMorning.name = ShiftSlot.AFTERNOON.toString();
      } 
      slotAfternoon.beginSlot = tt.startAfternoon;
      slotAfternoon.endSlot = tt.endAfternoon;
      slotAfternoon.beginMealSlot = tt.startAfternoonLunchTime;
      slotAfternoon.endMealSlot = tt.endAfternoonLunchTime;
      slotAfternoon.minutesPaid = tt.paidMinutes;
      slotAfternoon.minutesSlot = tt.totalWorkMinutes/2;
      slotAfternoon.shiftTimeTable = ostt;
      slotAfternoon.save();

      if (ostt.name.contains("IIT")) {
        slotMorning.name = "IIT - "+ShiftSlot.EVENING.toString();
      } else {
        slotMorning.name = ShiftSlot.EVENING.toString();
      } 
      slotEvening.beginSlot = tt.startEvening;
      slotEvening.endSlot = tt.endEvening;
      slotEvening.beginMealSlot = tt.startEveningLunchTime;
      slotEvening.endMealSlot = tt.endEveningLunchTime;
      slotEvening.minutesPaid = tt.paidMinutes;
      slotEvening.minutesSlot = tt.totalWorkMinutes/2;
      slotEvening.shiftTimeTable = ostt;
      slotEvening.save();
    }
  }



}
