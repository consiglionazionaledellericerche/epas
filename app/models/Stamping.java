/**
 * 
 */
package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDateTime;

import play.data.validation.InPast;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Audited
@Entity
@Table(name = "stampings")

public class Stamping extends Model implements Comparable<Stamping> {

	private static final long serialVersionUID = -2422323948436157747L;

	public enum WayType {
		in("in"),
		out("out");
		
		public String description;
		
		private WayType(String description){
			this.description = description;
		}
		
		public String getDescriptio(){
			return this.description;
		}
	}
	
	@Required
	@ManyToOne(optional = false)
		
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
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDateTime")
	
	public LocalDateTime date;
	
	@Required
	@Enumerated(EnumType.STRING)
	public WayType way;
	
	@ManyToOne
	@JoinColumn(name ="badge_reader_id")
	public BadgeReader badgeReader;
	
	public String note;
		
	/**
	 * questo campo booleano consente di determinare se la timbratura è stata effettuata dall'utente all'apposita
	 * macchinetta (valore = false) o se è stato l'amministratore a settare l'orario di timbratura poichè la persona 
	 * in questione non ha potuto effettuare la timbratura (valore = true)
	 */
	@Column(name = "marked_by_admin")
	public Boolean markedByAdmin;	
	
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

	//setter implementato per yaml parser TODO toglierlo configurando snakeyaml
	public void setDate(String date){
		
		//2013-10-03T19:18:00.000
		String data = date.split("T")[0];
		String time = date.split("T")[1];
		//2013-10-03
		int year = Integer.parseInt(data.split("-")[0]);
		int month= Integer.parseInt(data.split("-")[1]);
		int day  = Integer.parseInt(data.split("-")[2]);
		//19:18:00.000
		int hour = Integer.parseInt(time.split(":")[0]);
		int min  = Integer.parseInt(time.split(":")[1]);
		//int sec  = Integer.parseInt( (time.split(":")[2]).split(".")[0] );
		this.date = new LocalDateTime(year,month,day,hour,min,0);
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isValid()
	{
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
	
	public boolean isServiceStamping() {
		if(this.stampType!=null && this.stampType.identifier!=null && this.stampType.identifier.equals("s"))
		{
			return true;
		}
		return false;
	}
	




}
