package models.enumerate;

import lombok.val;

public enum LimitType {

  monthly("mensile"),
  yearly("annuale"),
  onMonthlyPresence("su presenza mensile"),
  noLimit("senza limite");
  
  public String description;
  
  LimitType(String description) {
    this.description = description;
  }
  
  public static LimitType getByDescription(String description) {
    for (val lt : values()) {
      if (lt.description.equals(description)) {
        return lt;
      }
    }
    throw new IllegalArgumentException(String.format("unknonw LimitType %s", description));
  }
}
