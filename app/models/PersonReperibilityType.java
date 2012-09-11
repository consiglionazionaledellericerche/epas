/**
 * 
 */
package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Audited
@Entity
@Table(name = "person_reperibility_types")
public class PersonReperibilityType extends Model {

	@Required
	public String description;
	
	@OneToMany(mappedBy = "personReperibilityType")
	public List<PersonReperibility> personReperibilities;
	
	@Override
	public String toString() {
		return String.format("PersonReperibilityType[%d] - description = %s", id, description);
	}
}
