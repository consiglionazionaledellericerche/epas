package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.data.validation.Email;
import play.data.validation.Phone;
import play.db.jpa.Model;
/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name = "contact_data")
public class ContactData extends Model {

	private static final long serialVersionUID = -4896743772636303002L;

	@OneToOne
	@JoinColumn(name = "person_id")
	public Person person;

	@Phone
	public String telephone;

	@Phone
	public String fax;
	
	@Email
	public String email;
	
	@Phone
	public String mobile;
}
