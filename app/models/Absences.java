package models;

import java.sql.Time;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;

/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name = "absences")
public class Absences extends Model{
	
	@Column
	public Date date;
	
	
	@ManyToOne
	@JoinColumn(name = "absenceType_id")
	public AbsenceType absenceType;
	
	@ManyToOne
	@JoinColumn(name="person_id")
	public Person person;
	

}
