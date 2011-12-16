/**
 * 
 */
package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CollectionId;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "stampings")
public class Stamping extends Model {

	public enum WayType {
		in,
		out
	}
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	//@Required
	@ManyToOne(optional = true)
	@JoinColumn(name = "stamp_type_id")
	public StampType stampType;
	
	@Required
	public LocalDate date;
	
	public int dayType;
	
	
	@Required
	@Enumerated(EnumType.STRING)
	public WayType way;
	
	public String notes;
	
	/**
	 * questo campo booleano consente di determinare se la timbratura è stata effettuata dall'utente all'apposita
	 * macchinetta (valore = false) o se è stato l'amministratore a settare l'orario di timbratura poichè la persona 
	 * in questione non ha potuto effettuare la timbratura (valore = true)
	 */
	public boolean isMarkedByAdmin;
	
	/**
	 * questo campo booleano consente di determinare se la timbratura è come uscita di servizio.
	 */
	public boolean isServiceExit;
	
	public boolean isMealTicketAssigned() {
		//Se il tempo è maggiore delle ore impostate nel tipo di orario di questa timbratura return true, false altrimenti
		return true;
	}
	
}
