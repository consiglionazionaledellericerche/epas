package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.data.validation.Email;
import play.data.validation.Phone;
import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;
/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name = "contact_data")
public class ContactData extends Model{

//	@ManyToOne
//	@JoinColumn(name = "person_id", nullable = false)
//	public Person person;

	@Phone
	public String telephone;

	@Phone
	public String fax;
	
	@Email
	public String email;
	
	@Phone
	public String mobile;
}
