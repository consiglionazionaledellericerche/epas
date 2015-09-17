/**
 * 
 */
package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
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
	
	@OneToOne (optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	public User user;
	
	public boolean enabled = true;
}
