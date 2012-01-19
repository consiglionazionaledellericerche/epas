package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;
/**
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "absence_type_groups")
public class AbsenceTypeGroup extends Model{
	
	private static final long serialVersionUID = -8664634519147481684L;

	@OneToOne
	@JoinColumn(name="absence_type_id")
	public AbsenceType absenceType;
	
	public String label;

	public Boolean minutesExcess;

	public Integer buildUp;

	public Integer buildUpLimit;

	public Integer buildUpEdgeBehaviour;

	public String equivalentCode;
}
