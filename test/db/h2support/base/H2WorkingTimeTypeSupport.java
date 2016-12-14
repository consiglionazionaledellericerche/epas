package db.h2support.base;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import dao.WorkingTimeTypeDao;

import db.h2support.base.WorkingTimeTypeDefinitions.WorkingDayDefinition;
import db.h2support.base.WorkingTimeTypeDefinitions.WorkingDefinition;

import models.WorkingTimeType;
import models.WorkingTimeTypeDay;

import java.util.List;

public class H2WorkingTimeTypeSupport {

  private final WorkingTimeTypeDao workingTimeTypeDao;
  
  @Inject
  public H2WorkingTimeTypeSupport(WorkingTimeTypeDao workingTimeTypeDao) {
    this.workingTimeTypeDao = workingTimeTypeDao;
    
  }

  private WorkingTimeTypeDay createWorkingTimeTypeDay(
      WorkingDayDefinition workingDayDefinition, WorkingTimeType workingTimeType) {
    
    WorkingTimeTypeDay workingTimeTypeDay = new WorkingTimeTypeDay();
    workingTimeTypeDay.workingTimeType = workingTimeType;
    workingTimeTypeDay.dayOfWeek = workingDayDefinition.dayOfWeek;
    workingTimeTypeDay.workingTime = workingDayDefinition.workingTime;
    workingTimeTypeDay.holiday = workingDayDefinition.holiday;
    workingTimeTypeDay.mealTicketTime = workingDayDefinition.mealTicketTime;
    workingTimeTypeDay.breakTicketTime = workingDayDefinition.breakTicketTime;
    workingTimeTypeDay.ticketAfternoonThreshold = workingDayDefinition.ticketAfternoonThreshold;
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
   * @param workingDefinition definizione 
   * @return persisted entity
   */
  public WorkingTimeType getWorkingTimeType(WorkingDefinition workingDefinition) {
    
    WorkingTimeType workingTimeType = workingTimeTypeDao
        .workingTypeTypeByDescription(workingDefinition.name(), Optional.absent());
    
    if (workingTimeType != null) {
      return workingTimeType;
    }
    
    workingTimeType = new WorkingTimeType();
    workingTimeType.description = workingDefinition.name();
    workingTimeType.horizontal = workingDefinition.horizontal;
    workingTimeType.save();
    workingTimeType.workingTimeTypeDays = 
        createWorkingTimeTypeDays(workingDefinition.orderedWorkingDayDefinition, workingTimeType);
    workingTimeType.refresh();
    return workingTimeType;
  }
  
}
