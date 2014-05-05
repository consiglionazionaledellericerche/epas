/**
 * 
 */
package models.enumerate;

/**
 * @author cristian
 *
 */
public enum WorkingTimeTypeValues {

	NORMALE_MOD(1l);
	
	private Long id;
	
	private WorkingTimeTypeValues(Long id) {
		this.id = id;
	}
	
	public Long getId() { return id; }
}
