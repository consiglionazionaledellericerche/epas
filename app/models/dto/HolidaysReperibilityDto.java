package models.dto;

import com.google.common.collect.Range;
import java.util.List;
import models.Person;
import org.joda.time.LocalDate;


public class HolidaysReperibilityDto {

  public Person person;
  
  public int holidaysReperibility;
  
  public List<Range<LocalDate>> holidaysPeriods;
}
