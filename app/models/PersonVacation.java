package models;

import java.util.Date;

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
public class PersonVacation extends Model{
	
	
	@ManyToOne	
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	@ManyToOne
	@JoinColumn(name = "vacationType_id", nullable = false)
	public VacationType vacationType;
	
	@Column
	public Date beginFrom;
	
	@Column
	public Date endTo;
}
