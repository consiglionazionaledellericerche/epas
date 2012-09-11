/**
 * 
 */
package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * Per ogni giorno della settimana ci sono riportate le informazioni necessarie 
 * all'utilizzo di questa tipologia di orario nel giorno specificato
 * 
 * @author cristian
 * @author dario
 */
@Audited
@Entity
@Table(name = "working_time_type_days")
public class WorkingTimeTypeDay extends Model {

	private static final long serialVersionUID = 4622948996966018754L;

	@Required
	@ManyToOne
	@JoinColumn(name = "working_time_type_id")
	public WorkingTimeType workingTimeType;
	
	@Required
	@Min(1)
	@Max(7)
	public int dayOfWeek;
		
	/**
	 * tempo di lavoro giornaliero espresso in minuti 
	 */
	public Integer workingTime;
	
	/**
	 * tempo di lavoro espresso in minuti che conteggia se possibile usufruire del buono pasto
	 */
	public Integer mealTicketTime;
	
	public Integer breakTicketTime;
	/**
	 * booleano per controllo se il giorno in questione è festivo o meno
	 */
	public boolean holiday = false;
	/**
	 * tempo di inizio finestra di entrata
	 */
	public Integer timeSlotEntranceFrom;
	/**
	 * tempo di fine finestra di entrata
	 */
	public Integer timeSlotEntranceTo;
	public Integer timeSlotExitFrom;
	public Integer timeSlotExitTo;
	/**
	 * tempo inizio pausa pranzo
	 */
	public Integer timeMealFrom;
	/**
	 * tempo fine pausa pranzo
	 */
	public Integer timeMealTo;
	
	
}
