package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
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
	
	public int qualification;
	
	public String description;
	
}
