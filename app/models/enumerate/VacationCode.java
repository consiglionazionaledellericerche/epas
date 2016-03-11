package models.enumerate;

import models.enumerate.EpasParam.EpasParamTimeType;
import models.enumerate.EpasParam.EpasParamValueType;
import models.enumerate.EpasParam.RecomputationType;

import java.util.List;

public enum VacationCode {
  
  VACATION_28_4("28+4"),
  VACATION_26_4("26+4"),
  VACATION_25_4("25+4"),
  VACATION_21_4("21+4"),
  
  VACATION_22_3("22+3"),
  VACATION_21_3("21+3");
    
  public final String name;

  VacationCode(String name) {
    this.name = name;
  }
  
  
  
}
