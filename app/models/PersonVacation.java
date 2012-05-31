package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.db.jpa.Model;

/** 
 * @author dario
 * @author cristian
 */
@Audited
@Entity
@Table(name = "person_vacations", uniqueConstraints = { @UniqueConstraint(columnNames={ "person_id", "date"}) } )
public class PersonVacation extends Model {
	
	private static final long serialVersionUID = -4964482244412822954L;

	@Required
	@ManyToOne	
	@JoinColumn(name = "person_id", nullable = false, updatable = false)
	public Person person;

	@Required
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")	
	@Column(name="date", updatable = false)
	public Date date;
	
	PersonVacation() { super(); }
	
	public PersonVacation(Person person, Date date) {
		this.person = person;
		this.date = date;
	}
	
}
