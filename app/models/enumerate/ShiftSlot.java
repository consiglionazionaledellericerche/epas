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
	
	private ShiftSlot(String name) {
		this.name = name;
	};
	
	public String getName() {
		return name;
	}
}
