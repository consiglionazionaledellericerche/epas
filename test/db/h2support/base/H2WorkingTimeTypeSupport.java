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

package db.h2support.base;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import dao.WorkingTimeTypeDao;
import db.h2support.base.WorkingTimeTypeDefinitions.WorkingDayDefinition;
import db.h2support.base.WorkingTimeTypeDefinitions.WorkingDefinition;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;

@Slf4j
public class H2WorkingTimeTypeSupport {

  private final WorkingTimeTypeDao workingTimeTypeDao;
  
  @Inject
  public H2WorkingTimeTypeSupport(WorkingTimeTypeDao workingTimeTypeDao) {
    this.workingTimeTypeDao = workingTimeTypeDao;
    
  }

  private WorkingTimeTypeDay createWorkingTimeTypeDay(
      WorkingDayDefinition workingDayDefinition, WorkingTimeType workingTimeType) {
    
    WorkingTimeTypeDay workingTimeTypeDay = new WorkingTimeTypeDay();
    workingTimeTypeDay.setWorkingTimeType(workingTimeType);
    workingTimeTypeDay.setDayOfWeek(workingDayDefinition.dayOfWeek);
    workingTimeTypeDay.setWorkingTime(workingDayDefinition.workingTime);
    workingTimeTypeDay.setHoliday(workingDayDefinition.holiday);
    workingTimeTypeDay.setMealTicketTime(workingDayDefinition.mealTicketTime);
    workingTimeTypeDay.setBreakTicketTime(workingDayDefinition.breakTicketTime);
    workingTimeTypeDay.setTicketAfternoonThreshold(workingDayDefinition.ticketAfternoonThreshold);
    workingTimeTypeDay.ticketAfternoonWorkingTime = workingDayDefinition.ticketAfternoonWorkingTime;
    workingTimeTypeDay.save();
    return workingTimeTypeDay;
  }
  
  private List<WorkingTimeTypeDay> createWorkingTimeTypeDays(
      List<WorkingDayDefinition> workingDayDefinitions, WorkingTimeType workingTimeType) {
    List<WorkingTimeTypeDay> list = Lists.newArrayList();
    for (WorkingDayDefinition definition : workingDayDefinitions) {
      list.add(createWorkingTimeTypeDay(definition, workingTimeType));
    }
    return list;
  }

  /**
   * Costruisce e persiste una istanza del tipo orario secondo definizione.
   *
   * @param workingDefinition definizione 
   * @return persisted entity
   */
  public WorkingTimeType getWorkingTimeType(WorkingDefinition workingDefinition) {
    
    WorkingTimeType workingTimeType = workingTimeTypeDao
        .workingTypeTypeByDescription(workingDefinition.name(), Optional.absent());
    
    if (workingTimeType != null) {
      return workingTimeType;
    }
    
    log.debug("Costruisco il workingTimeType {}", workingDefinition);
    workingTimeType = new WorkingTimeType();
    workingTimeType.setDescription(workingDefinition.name());
    workingTimeType.setHorizontal(workingDefinition.horizontal);
    workingTimeType.save();
    workingTimeType.setWorkingTimeTypeDays(
        createWorkingTimeTypeDays(workingDefinition.orderedWorkingDayDefinition, workingTimeType));
    return workingTimeType;
  }
  
}
