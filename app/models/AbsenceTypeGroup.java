package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import net.sf.oval.constraint.NotNull;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
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

	@OneToMany(mappedBy="absenceTypeGroup", fetch = FetchType.LAZY)
	public List<AbsenceType> absenceTypes;
		
	@Required
	public String label;

	@Column(name = "minutes_excess")
	public Boolean minutesExcess;

	@Column(name = "limit_in_minute")
	public Integer limitInMinute;
	
	
	@Enumerated(EnumType.STRING)
	@Column(name = "accumulation_type")
	public AccumulationType accumulationType;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "accumulationBehaviour")
	public AccumulationBehaviour accumulationBehaviour;
	
	
	@OneToOne
	@JoinColumn(name="replacing_absence_type_id")
	public AbsenceType replacingAbsenceType;
}
