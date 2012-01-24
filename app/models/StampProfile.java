/**
 * 
 */
package models;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "stamp_profiles")
public class StampProfile extends Model {

	private static final long serialVersionUID = 5187385003376986175L;

	@Required
	@OneToOne
	@JoinColumn(name="person_id", nullable=false)
	public Person person;
	
	@Required
	public boolean onCertificate = true;
	
	public Boolean fixedWorkTime;
	
	@ManyToMany(mappedBy="stampProfiles")
	public Set<CompetenceProfile> competenceProfiles;
	
}
