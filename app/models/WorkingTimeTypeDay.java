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
	public WorkingTimeType workingTimeType;
	
	@Required
	public DayOfWeek dayOfWeek;
		
	/**
	 * tempo di lavoro giornaliero espresso in minuti 
	 */
	public int workingTime = 0;
	
	/**
	 * tempo di lavoro espresso in minuti che conteggia se possibile usufruire del buono pasto
	 */
	public int mealTicketTime = 0;
	
	public int breakTicketTime = 0;
	/**
	 * booleano per controllo se il giorno in questione è festivo o meno
	 */
	public boolean holiday = false;
	/**
	 * tempo di inizio finestra di entrata
	 */
	public int timeSlotEntranceFrom = 0;
	/**
	 * tempo di fine finestra di entrata
	 */
	public int timeSlotEntranceTo = 0;
	public int timeSlotExitFrom = 0;
	public int timeSlotExitTo = 0;
	/**
	 * tempo inizio pausa pranzo
	 */
	public int timeMealFrom = 0;
	/**
	 * tempo fine pausa pranzo
	 */
	public int timeMealTo = 0;
	
}
