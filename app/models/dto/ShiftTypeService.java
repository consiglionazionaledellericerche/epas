package models.dto;

import models.ShiftTimeTable;
import models.ShiftType.ToleranceType;

import play.data.validation.Required;


public class ShiftTypeService {

  @Required
  public String name;
  
  @Required
  public String description;
  
  
  public int entranceTolerance = 0;
  public int entranceMaxTolerance = 0;
  public int exitTolerance = 0;
  public int exitMaxTolerance = 0;
  public int breakInShift;
  public int breakMaxInShift;
     
  public Integer maxTolerance;
  
  public ToleranceType toleranceType;
  
  
  public ShiftTimeTable timeTable;
}
