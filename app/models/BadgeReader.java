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

import models.base.BaseModel;
import net.sf.oval.constraint.NotNull;



/**
 * @author cristian
 *
 */

@Entity
@Table(name="badge_readers")
@Audited
public class BadgeReader extends BaseModel {

	@NotNull
	public String code;
	
	public String description;
	
	public String location;
	
	@OneToMany(mappedBy="badgeReader")
	public List<Stamping> stampings = new ArrayList<Stamping>();
	
	public boolean enabled = true;
}
