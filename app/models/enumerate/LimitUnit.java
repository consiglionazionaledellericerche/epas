package models.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public enum LimitUnit {

  minutes("minuti"),
  hours("ore"),
  days("giorni");

  @Getter
  private final String description;

  public static LimitUnit getByDescription(String description) {
    for (val lu : values()) {
      if (lu.description.equals(description)) {
        return lu;
      }
    }
    throw new IllegalArgumentException(String.format("unknonw LimitUnit %s", description));
  }

}
