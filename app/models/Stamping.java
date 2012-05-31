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

import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

import play.data.validation.InPast;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "stampings")
public class Stamping extends Model {

	private static final long serialVersionUID = -2422323948436157747L;

	public enum WayType {
		in,
		out
	}
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false, updatable = false)
	public Person person;
	
	@Required
	@ManyToOne(optional = false)
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
	
	public String note;
	
	/**
	 * questo campo booleano consente di determinare se la timbratura è stata effettuata dall'utente all'apposita
	 * macchinetta (valore = false) o se è stato l'amministratore a settare l'orario di timbratura poichè la persona 
	 * in questione non ha potuto effettuare la timbratura (valore = true)
	 */
	@Column(name = "marked_by_admin")
	public Boolean markedByAdmin;
	
	/**
	 * questo campo booleano consente di determinare se la timbratura è come uscita di servizio.
	 */
	@Column(name = "service_exit")
	public Boolean serviceExit;
	
		
		
}
