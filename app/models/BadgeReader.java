/**
 * 
 */
package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import net.sf.oval.constraint.NotNull;

import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Audited
@Entity
@Table(name="badge_readers")
@Audited
public class BadgeReader extends Model {

	@NotNull
	public String code;
	
	public String description;
	
	public String location;
	
	@OneToMany(mappedBy="badgeReader")
	public List<Stamping> stampings = new ArrayList<Stamping>();
	
	public boolean enabled = true;
}
