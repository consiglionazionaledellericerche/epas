package models.dto;

import com.google.common.collect.Range;

import models.Person;

import org.joda.time.LocalDate;

import java.util.List;

public class HolidaysReperibilityDto {

  public Person person;
  
  public int holidaysReperibility;
  
  public List<Range<LocalDate>> holidaysPeriods;
}
