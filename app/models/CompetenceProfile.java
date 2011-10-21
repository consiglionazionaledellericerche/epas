/**
 * 
 */
package models;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "competence_profiles")
public class CompetenceProfile extends Model {

	@Required
	public String code;
	
	public String certificateCode;
	
	public String description;
	
	@Required
	public boolean inactive = false;
	
	@ManyToMany
	@JoinTable(name="stamp_profile_competence_profile", 
			joinColumns=@JoinColumn(name="competence_profile_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="stamp_profile_id", referencedColumnName="id"))
	public Set<StampProfile> stampProfiles;
	
	
}
