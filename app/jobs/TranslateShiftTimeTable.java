/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
        ostt.setCalculationType(tt.getCalculationType());
        ostt.setOffice(tt.getOffice());
        ostt.setName(manager.transformTimeTableName(tt));
        ostt.save();
        log.info("Salvata timetable {}", ostt.getName());
        OrganizationShiftSlot slotMorning = new OrganizationShiftSlot();
        
        OrganizationShiftSlot slotEvening = new OrganizationShiftSlot();
        if (ostt.getName().contains("IIT")) {
          slotMorning.setName("IIT - " + ShiftSlot.MORNING.toString());
        } else {
          slotMorning.setName(ShiftSlot.MORNING.toString());
        }      
        slotMorning.setBeginSlot(tt.getStartMorning());
        slotMorning.setEndSlot(tt.getEndMorning());
        slotMorning.setBeginMealSlot(tt.getStartMorningLunchTime());
        slotMorning.setEndMealSlot(tt.getEndMorningLunchTime());
        slotMorning.setMinutesPaid(tt.getPaidMinutes());
        slotMorning.setPaymentType(PaymentType.T1);
        slotMorning.setShiftTimeTable(ostt);
        slotMorning.save();
        log.debug("Salvato slot {} per timetable {}", slotMorning.getName(), ostt.getName());
        OrganizationShiftSlot slotAfternoon = new OrganizationShiftSlot();
        if (ostt.getName().contains("IIT")) {
          slotMorning.setName("IIT - " + ShiftSlot.AFTERNOON.toString());
        } else {
          slotMorning.setName(ShiftSlot.AFTERNOON.toString());
        } 
        slotAfternoon.setBeginSlot(tt.getStartAfternoon());
        slotAfternoon.setEndSlot(tt.getEndAfternoon());
        slotAfternoon.setBeginMealSlot(tt.getStartAfternoonLunchTime());
        slotAfternoon.setEndMealSlot(tt.getEndAfternoonLunchTime());
        slotAfternoon.setMinutesPaid(tt.getPaidMinutes());
        slotMorning.setPaymentType(PaymentType.T1);
        slotAfternoon.setShiftTimeTable(ostt);
        slotAfternoon.save();
        log.debug("Salvato slot {} per timetable {}", slotMorning.getName(), ostt.getName());
        
        if (ostt.getName().contains("IIT")) {
          slotMorning.setName("IIT - " + ShiftSlot.EVENING.toString());
        } else {
          slotMorning.setName(ShiftSlot.EVENING.toString());
        } 
        if (tt.getStartEvening() != null && tt.getEndEvening() != null) {
          slotEvening.setBeginSlot(tt.getStartEvening());
          slotEvening.setEndSlot(tt.getEndEvening());
          slotEvening.setBeginMealSlot(tt.getStartEveningLunchTime());
          slotEvening.setEndMealSlot(tt.getEndEveningLunchTime());
          slotEvening.setMinutesPaid(tt.getPaidMinutes());
          slotMorning.setPaymentType(PaymentType.T1);
          slotEvening.setShiftTimeTable(ostt);
          slotEvening.save();
          log.debug("Salvato slot {} per timetable {}", slotMorning.getName(), ostt.getName());
        }
      }
      
      
    }
    log.info("Terminata procedura");
  }



}
