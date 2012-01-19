package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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

	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;
	
	public String department;

	public String headOffice;
 
	public String room;
}
