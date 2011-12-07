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
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "stamp_type_id", nullable = false)
	public StampType stampType;
	
	@Required
	public LocalDate date;
	
	@Required
	@Enumerated(EnumType.STRING)
	public WayType way;
	
	public String notes;
	
	public boolean isMealTicketAssigned() {
		//Se il tempo è maggiore delle ore impostate nel tipo di orario di questa timbratura return true, false altrimenti
		return true;
	}
	
}
