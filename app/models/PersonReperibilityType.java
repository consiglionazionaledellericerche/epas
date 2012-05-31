/**
 * 
 */
package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Audited
@Entity
@Table(name = "person_reperibility_types")
public class PersonReperibilityType extends Model {

	public String description;
	
	@OneToMany(mappedBy = "personReperibilityType")
	public List<PersonReperibility> personReperibilities;
}
