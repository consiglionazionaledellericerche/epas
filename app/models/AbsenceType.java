package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;
/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name="absence_types")
@Audited
@Inheritance(strategy=InheritanceType.JOINED)
public class AbsenceType extends Model {
	
	private static final long serialVersionUID = 7157167508454574329L;

	@OneToMany(mappedBy="absenceType")
	public List<Absence> absence;	
	
	@ManyToOne
	@JoinColumn(name="absenceTypeGroup_id")
	public AbsenceTypeGroup absenceTypeGroup;
	
	@ManyToMany(cascade = {CascadeType.REFRESH}, fetch = FetchType.LAZY)
	public List<Qualification> qualifications;
	
//	@OneToOne
//	@JoinColumn(name="hourlyAbsenceType_id")
//	public HourlyAbsenceType hourlyAbsenceType;
//	
//	@OneToOne
//	@JoinColumn(name="dailyAbsenceType_id")
//	public DailyAbsenceType dailyAbsenceType;
	
	public String code;
	
	public String certificateCode;
	
	public String description;
	
	public Date validFrom;
	
	public Date validTo;
	
	public boolean internalUse;
	
	public boolean multipleUse;	

	public boolean mealTicketCalculation;

	public boolean ignoreStamping;		
	
	public boolean isHourlyAbsence;
	
	public int justifiedWorkTime;
	
	public boolean isDailyAbsence;
	
		
}
