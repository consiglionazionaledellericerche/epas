/**
 * 
 */
package models;



import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "persons")
public class Person extends Model {
	
	@Required
	public String name;
	
	@Required
	public String surname;
	
	@Column
	public Date bornDate;
	
	@Email
	public String email;
	
	@Column(name = "contract_level")
	public int contractLevel;
	
	public int number;
	
}
