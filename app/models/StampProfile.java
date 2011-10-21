/**
 * 
 */
package models;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.ManyToAny;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "stamp_profiles")
public class StampProfile extends Model {

	@Required
	@OneToOne
	@JoinColumn(name="person_id", nullable=false)
	public Person person;
	
	@Required
	public Boolean onCertificate = true;
	
	public Boolean fixedWorkTime;
	
	@ManyToMany(mappedBy="stampProfiles")
	public Set<CompetenceProfile> competenceProfiles;
	
}
