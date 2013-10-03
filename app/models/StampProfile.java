/**
 * 
 */
package models;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "stamp_profiles")
public class StampProfile extends Model {

	private static final long serialVersionUID = 5187385003376986175L;

	@Required
	@ManyToOne
	@JoinColumn(name="person_id", nullable=false)
	public Person person;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="start_from")
	public LocalDate startFrom;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_to")
	public LocalDate endTo;
	
	/**
	 * Corrisponde alla voce "Presenza predefinita" della vecchia applicazione
	 * Quando è true viene impostato nel PersonDay l'orario di lavoro previsto
	 * per il giorno indipendentemente dalla presenza di timbrature.
	 * Nel caso ci siano timbrature viene impostato nel PersonDay il minimo tra
	 * l'orario di lavoro previsto per il giorno ed il timeAtWork calcolato con 
	 * le timbrature presenti
	 */
	public boolean fixedWorkingTime = false;
	
	

}
