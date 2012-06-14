package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

import lombok.Data;
/**
 * 
 * @author dario
 *
 */
@Data
@Entity
@Audited
@Table(name="qualifications")
public class Qualification extends Model{
	
	@OneToMany(mappedBy="qualification", fetch = FetchType.LAZY)
	public List<Person> person;
	
	@ManyToMany(mappedBy = "qualifications", cascade = { CascadeType.ALL })
	public List<AbsenceType> absenceType;
	
	public int qualification;
	
	public String description;
	
}
