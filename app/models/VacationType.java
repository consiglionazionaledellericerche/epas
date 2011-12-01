/**
 * 
 */
package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "vacation_types")
public class VacationType extends Model {
	
	@OneToMany(mappedBy="vacationType")
	public List<PersonVacation> personVacation;
	
	@Required
	public String description;
	
	@Required
	public int vacationDays = 28;
	
	@Required
	public int permissionDays = 4;
	
}
