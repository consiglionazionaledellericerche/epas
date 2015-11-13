package models.enumerate;

/**
 * 
 * @author arianna
 *
 */
public enum ShiftSlot {
	MORNING("mattina"),
	AFTERNOON("pomeriggio");
	
	private String name;
	
	ShiftSlot(String name) {
		this.name = name;
	};
	
	public String getName() {
		return name;
	}
	
	public static ShiftSlot getEnum(String name) {
		for (ShiftSlot shiftSlot : values()) {
			if (shiftSlot.getName().equals(name)) {
				return shiftSlot;
			}
		}
		throw new IllegalArgumentException(String.format("ShiftSlot with name = %s not found", name));
	}

}
