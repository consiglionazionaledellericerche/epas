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
import org.hibernate.envers.NotAudited;

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
public class AbsenceType extends Model{
	
	@OneToMany(mappedBy="absenceType")
	public List<Absence> absence;	
	
	@OneToOne
	@JoinColumn(name="absenceTypeGroup_id")
	public AbsenceTypeGroup absenceTypeGroup;
	
	@OneToOne
	@JoinColumn(name="hourlyAbsenceType_id")
	public HourlyAbsenceType hourlyAbsenceType;
	
	@OneToOne
	@JoinColumn(name="dailyAbsenceType_id")
	public DailyAbsenceType dailyAbsenceType;
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

}
