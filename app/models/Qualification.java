package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;
/**
 * 
 * @author dario
 *
 */
@Entity
@Audited
@Table(name="qualifications")
public class Qualification extends Model{
	
	@OneToMany(mappedBy="qualification")
	public List<Person> person;
	
	@ManyToMany(mappedBy = "qualifications", fetch = FetchType.LAZY)
	public List<AbsenceType> absenceTypes;
	
	public int qualification;
	
	public String description;
	
}
