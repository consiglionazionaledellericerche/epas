package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name = "locations")
public class Location extends Model {
	
	private static final long serialVersionUID = -5959095020484665233L;

	@OneToOne
	@JoinColumn(name = "person_id")
	public Person person;
	
	public String department;

	public String headOffice;
 
	public String room;
}
