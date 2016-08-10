package models.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public enum LimitDescription {

  daysOfMonth("giorni del mese"),
  daysOfYear("giorni dell'anno");
  
  @Getter
  private final String description;
  
  
  public static LimitDescription getByDescription(String description) {
    for (val lu : values()) {
      if (lu.description.equals(description)) {
        return lu;
      }
    }

    throw new IllegalStateException(String.format("unknonw LimitDescription %s", description));

  }
  
}
