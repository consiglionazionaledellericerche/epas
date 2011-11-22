package models;

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
public class ContactData extends Model{

	@ManyToOne
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	@Column
	public int telephone;
	@Column
	public int fax;
	@Column
	public String email;
	@Column
	public int mobile;
}
