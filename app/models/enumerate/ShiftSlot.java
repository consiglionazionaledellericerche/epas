package models.enumerate;

/**
 * 
 * @author arianna
 *
 */
public enum ShiftSlot {
	MATTINA("mattina"),
	POMERIGGIO("pomeriggio");
	
	private String id;
	
	ShiftSlot(String id) {
		this.id = id;
	};
	
	public String getId() {
		return id;
	}
}
