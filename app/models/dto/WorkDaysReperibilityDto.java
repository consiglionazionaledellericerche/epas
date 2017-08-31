package models.dto;

import com.google.common.collect.Range;

import java.util.List;

import models.Person;

import org.joda.time.LocalDate;

public class WorkDaysReperibilityDto {

  public Person person;
  
  public int workdaysReperibility;
    
  public List<Range<LocalDate>> workdaysPeriods;
    
}
