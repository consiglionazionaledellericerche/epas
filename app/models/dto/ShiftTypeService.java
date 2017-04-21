package models.dto;

import models.ShiftTimeTable;
import models.ShiftType.ToleranceType;

import play.data.validation.Required;


public class ShiftTypeService {

  @Required
  public String name;
  
  @Required
  public String description;
  
  @Required
  public int tolerance = 0;
  @Required
  public int hourTolerance = 0;
  public ToleranceType toleranceType;
  
  public boolean breakInShiftEnabled = false;
  public int breakInShift;
  
  public ShiftTimeTable timeTable;
}
