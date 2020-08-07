package jobs;

import com.google.common.base.Optional;
import dao.OrganizationShiftTimeTableDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ShiftOrganizationManager;
import models.OrganizationShiftSlot;
import models.OrganizationShiftTimeTable;
import models.ShiftTimeTable;
import models.enumerate.PaymentType;
import models.enumerate.ShiftSlot;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart(async = true)
@Slf4j
public class TranslateShiftTimeTable extends Job<Void> {


  @Inject
  private static ShiftOrganizationManager manager;
  @Inject
  private static OrganizationShiftTimeTableDao dao;

  @Override
  public void doJob() {
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    final List<ShiftTimeTable> list = ShiftTimeTable.findAll();
    log.info("Inizio procedura trasformazione timetable");
    for (ShiftTimeTable tt : list) {
      OrganizationShiftTimeTable ostt = null;
      Optional<OrganizationShiftTimeTable> optional = 
          dao.getByName(manager.transformTimeTableName(tt));
      if (!optional.isPresent()) {
        ostt = new OrganizationShiftTimeTable();
        ostt.calculationType = tt.calculationType;
        ostt.office = tt.office;
        ostt.name = manager.transformTimeTableName(tt);
        ostt.save();
        log.info("Salvata timetable {}", ostt.name);
        OrganizationShiftSlot slotMorning = new OrganizationShiftSlot();
        
        OrganizationShiftSlot slotEvening = new OrganizationShiftSlot();
        if (ostt.name.contains("IIT")) {
          slotMorning.name = "IIT - " + ShiftSlot.MORNING.toString();
        } else {
          slotMorning.name = ShiftSlot.MORNING.toString();
        }      
        slotMorning.beginSlot = tt.startMorning;
        slotMorning.endSlot = tt.endMorning;
        slotMorning.beginMealSlot = tt.startMorningLunchTime;
        slotMorning.endMealSlot = tt.endMorningLunchTime;
        slotMorning.minutesPaid = tt.paidMinutes;
        slotMorning.paymentType = PaymentType.T1;
        slotMorning.shiftTimeTable = ostt;
        slotMorning.save();
        log.debug("Salvato slot {} per timetable {}", slotMorning.name, ostt.name);
        OrganizationShiftSlot slotAfternoon = new OrganizationShiftSlot();
        if (ostt.name.contains("IIT")) {
          slotMorning.name = "IIT - " + ShiftSlot.AFTERNOON.toString();
        } else {
          slotMorning.name = ShiftSlot.AFTERNOON.toString();
        } 
        slotAfternoon.beginSlot = tt.startAfternoon;
        slotAfternoon.endSlot = tt.endAfternoon;
        slotAfternoon.beginMealSlot = tt.startAfternoonLunchTime;
        slotAfternoon.endMealSlot = tt.endAfternoonLunchTime;
        slotAfternoon.minutesPaid = tt.paidMinutes;
        slotMorning.paymentType = PaymentType.T1;
        slotAfternoon.shiftTimeTable = ostt;
        slotAfternoon.save();
        log.debug("Salvato slot {} per timetable {}", slotMorning.name, ostt.name);
        
        if (ostt.name.contains("IIT")) {
          slotMorning.name = "IIT - " + ShiftSlot.EVENING.toString();
        } else {
          slotMorning.name = ShiftSlot.EVENING.toString();
        } 
        if (tt.startEvening != null && tt.endEvening != null) {
          slotEvening.beginSlot = tt.startEvening;
          slotEvening.endSlot = tt.endEvening;
          slotEvening.beginMealSlot = tt.startEveningLunchTime;
          slotEvening.endMealSlot = tt.endEveningLunchTime;
          slotEvening.minutesPaid = tt.paidMinutes;
          slotMorning.paymentType = PaymentType.T1;
          slotEvening.shiftTimeTable = ostt;
          slotEvening.save();
          log.debug("Salvato slot {} per timetable {}", slotMorning.name, ostt.name);
        }
      }
      
      
    }
    log.info("Terminata procedura");
  }



}
