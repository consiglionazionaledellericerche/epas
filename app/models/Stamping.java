/**
 * 
 */
package models;

import it.cnr.iit.epas.NullStringBinder;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDateTime;
import play.data.binding.As;
import play.data.validation.InPast;
import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * @author cristian
 *
 */
@Audited
@Entity
@Table(name = "stampings")

public class Stamping extends BaseModel implements Comparable<Stamping> {

	private static final long serialVersionUID = -2422323948436157747L;

	public Stamping(PersonDay personDay, LocalDateTime time) {
		this.personDay = personDay;
		this.date = time;
	}
	
	public enum WayType {
		in("in"),
		out("out");

		public String description;

		private WayType(String description){
			this.description = description;
		}

		public String getDescription(){
			return this.description;
		}
	}

	@Required
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "personDay_id", nullable = false, updatable = false)
	public PersonDay personDay;

	@ManyToOne
	@JoinColumn(name = "stamp_type_id")
	public StampType stampType;

	@ManyToOne(optional = true)
	@JoinColumn(name = "stamp_modification_type_id")
	public StampModificationType stampModificationType;

	@Required
	@InPast
	public LocalDateTime date;

	@Required
	@Enumerated(EnumType.STRING)
	public WayType way;

	@ManyToOne
	@JoinColumn(name ="badge_reader_id")
	public BadgeReader badgeReader;

	@As(binder=NullStringBinder.class)
	public String note;

	/**
	 * questo campo booleano consente di determinare se la timbratura è stata effettuata dall'utente all'apposita
	 * macchinetta (valore = false) o se è stato l'amministratore a settare l'orario di timbratura poichè la persona 
	 * in questione non ha potuto effettuare la timbratura (valore = true)
	 */
	@Column(name = "marked_by_admin")
	public Boolean markedByAdmin = false;
	
	/**
	 * con la nuova interpretazione delle possibilità del dipendente, questo campo viene settato a true quando
	 * è il dipendente a modificare la propria timbratura
	 */
	@Column(name = "marked_by_employee")
	public Boolean markedByEmployee = false;

	/**
	 * true, cella bianca; false, cella gialla
	 */
	@Transient
	public boolean valid;
	@Transient
	public int pairId = 0;

	/**
	 * true, la cella fittizia di uscita adesso
	 */
	@Transient
	public boolean exitingNow = false;

	@Transient
	public boolean isValid() {
		return this.valid;
	}

	@Transient
	public boolean isIn() {
		return way.equals(WayType.in);
	}

	@Transient
	public boolean isOut() {
		return way.equals(WayType.out);
	}

	@Override
	public String toString() {
		return String.format("Stamping[%d] - personDay.id = %d, way = %s, date = %s, stampType.id = %s, stampModificationType.id = %s",
				id, personDay.id, way, date, stampType != null ? stampType.id : "null", stampModificationType != null ? stampModificationType.id : "null");
	}

	/**
	 * Comparator Stamping
	 */
	public int compareTo(Stamping compareStamping)
	{
		if (date.isBefore(compareStamping.date))
			return -1;
		else if (date.isAfter(compareStamping.date))
			return 1;
		else
			return 0; 
	}

	@Transient
	public boolean isServiceStamping() {
		if(this.stampType!=null && this.stampType.identifier!=null && this.stampType.identifier.equals("s"))
		{
			return true;
		}
		return false;
	}

	@Transient
	public String formattedHour() {
		if(this.date != null)
			return date.toString("HH:mm");
		else
			return "";
	}

	@Transient
	public String formattedMark() {
		String mark = "";
		if(this.markedByAdmin!=null && this.markedByAdmin==true)
			mark = mark + "m";
		if(this.stampType != null)
			mark = mark + " " + this.stampType.identifier;
		return mark;
	}
	
	@Transient
	public boolean getBooleanWay() {
		return this.way.equals(Stamping.WayType.in) ? true : false;
	}
	
	@Transient
	public String getTime(){
		if (date == null) {
			return null;
		}
		if (date.getHourOfDay() > 10) {
			return date.getHourOfDay() + ":" + date.getMinuteOfHour();
		} else {
			return "0" + date.getHourOfDay() + ":" + date.getMinuteOfHour();
		}
	}
}
