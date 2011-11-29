/**
 * 
 */
package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * Per ogni giorno della settimana ci sono riportate le informazioni necessarie 
 * all'utilizzo di questa tipologia di orario nel giorno specificato
 * 
 * @author cristian
 * @author dario
 */
@Entity
public class WorkingTimeTypeDay extends Model {

	public enum DayOfWeek {
		monday, tuesday, wednesday, thursday, friday, saturday, sunday
	}

	@ManyToOne
	@JoinColumn(name = "working_time_type_id")
	@Column(name = "working_time_type")
	public WorkingTimeType workingTimeType;
	
	@Required
	public DayOfWeek dayOfWeek;
		
	/**
	 * tempo di lavoro giornaliero espresso in minuti 
	 */
	public int workingTime = 0;
	
	public int mealTicketTime = 0;
	public int breakTicketTime = 0;
	public boolean holiday = false;
	public int timeSlotEntranceFrom = 0;
	public int timeSlotEntranceTo = 0;
	public int timeSlotExitFrom = 0;
	public int timeSlotExitTo = 0;
	public int timeMealFrom = 0;
	public int timeMealTo = 0;
	
}
