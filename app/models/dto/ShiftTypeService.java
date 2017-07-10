package models.dto;

import models.ShiftTimeTable;
import models.ShiftType.ToleranceType;

import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;


public class ShiftTypeService {

  @Required
  public String name;
  
  @Required
  public String description;
  
  @Min(0)
  public int entranceTolerance = 0;
  @Min(0)
  public int entranceMaxTolerance = 0;
  @Min(0)
  public int exitTolerance = 0;
  @Min(0)
  public int exitMaxTolerance = 0;
  @Min(0)
  public int breakInShift;
  @Min(0)
  public int breakMaxInShift;
  @Min(0)
  @Max(3)
  
  public Integer maxTolerance = 0;
  
  public ToleranceType toleranceType;
  
  
  public ShiftTimeTable timeTable;
}
