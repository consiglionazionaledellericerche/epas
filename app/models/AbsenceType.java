package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;
/**
 * 
 * @author dario
 *
 */
@Entity
@Audited
@Table(name = "absence_types")
public class AbsenceType extends Model{
	
	@OneToMany(mappedBy="absenceType")
	public List<Absences> absence;
	
	@OneToOne
	@JoinColumn(name="absenceTypeGroup_id")
	public AbsenceTypeGroup absenceTypeGroup;
	
//	@OneToMany(mappedBy="absenceType")
//	public List<ContractLevel> contractLevel;
	
		
	@Column
	public String code;
	@Column
	public String certificateCode;
	@Column
	public String description;
	@Column
	public Date validFrom;
	@Column
	public Date validTo;
	@Column
	public boolean internalUse;
	@Column
	public boolean multipleUse;
	@Column
	public int justifiedWorkTime;
	@Column
	public boolean mealTicketCalculation;
	@Column
	public boolean ignoreStamping;	
	@Column
	public int groupValue;
	
}
