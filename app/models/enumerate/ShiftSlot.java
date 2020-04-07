package models.enumerate;

/**
 * Enumerato per la tipoloia di slot di turno.
 * @author arianna
 */
public enum ShiftSlot {
  MORNING("mattina"),
  AFTERNOON("pomeriggio"),
  EVENING("sera");

  private String name;

  ShiftSlot(String name) {
    this.name = name;
  }

  ;

  /**
   * Lo slot di turno a partire dal nome.
   * @param name il nome dello slot
   * @return lo slot di turno col nome passato come parametro.
   */
  public static ShiftSlot getEnum(String name) {
    for (ShiftSlot shiftSlot : values()) {
      if (shiftSlot.getName().equals(name)) {
        return shiftSlot;
      }
    }
    throw new IllegalArgumentException(String.format("ShiftSlot with name = %s not found", name));
  }

  public String getName() {
    return name;
  }

}
