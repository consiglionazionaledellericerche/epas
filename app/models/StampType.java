/**
 * 
 */
package models;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import net.sf.oval.constraint.MinLength;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "stamp_types")
public class StampType extends Model {

	@Required
	@MinLength(value=2)
	public String description;	

	@ManyToOne
	@JoinColumn( name = "code_id") 
	public Code code;
	
	@OneToMany(mappedBy="stampType")
	public Set<Stamping> stampings; 
}
