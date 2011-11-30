package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;
/**
 * 
 * @author dario
 *
 */

@Entity
@Table(name = "absence_type_group")
public class AbsenceTypeGroup extends Model{
	

	@OneToOne
	@JoinColumn(name="absenceType_id")
	public AbsenceType absenceType;
	
	@Column
	public String label;
	@Column
	public boolean minutesExcess;
	@Column
	public int buildUp;
	@Column
	public int buildUpLimit;
	@Column
	public int buildUpEdgeBehaviour;
	@Column
	public String equivalentCode;
}
