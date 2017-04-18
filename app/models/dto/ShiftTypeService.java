package models.dto;

import models.ShiftTimeTable;
import models.ShiftType.ToleranceType;

public class ShiftTypeService {

  public String name;
  public String description;
  
  public int tolerance;
  public int hourTolerance;
  public ToleranceType toleranceType;
  
  public boolean breakInShiftEnabled;
  public int breakInShift;
  
  public ShiftTimeTable timeTable;
}
