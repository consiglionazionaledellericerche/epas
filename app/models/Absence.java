package models;

import java.sql.Time;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;

/**
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "absences")
public class Absence extends Model {
	
	public LocalDate date;
	
	@ManyToOne
	@JoinColumn(name = "absenceType_id")
	public AbsenceType absenceType;
	
	@ManyToOne
	@JoinColumn(name="person_id")
	public Person person;
	

}
