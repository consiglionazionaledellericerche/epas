/**
 * 
 */
package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;
import net.sf.oval.constraint.NotNull;

import org.hibernate.envers.Audited;



/**
 * @author cristian
 *
 */

@Entity
@Table(name="badge_readers")
@Audited
public class BadgeReader extends BaseModel {

	private static final long serialVersionUID = -3508739971079270193L;

	@NotNull
	public String code;
	
	public String description;
	
	public String location;
	
	@OneToMany(mappedBy="badgeReader")
	public List<Stamping> stampings = new ArrayList<Stamping>();
	
	public boolean enabled = true;
}
