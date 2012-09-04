package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;

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

	@OneToMany(mappedBy="absenceTypeGroup")
	public List<AbsenceType> absenceTypes;
		
	public String label;

	public Boolean minutesExcess;

	public Integer limitInMinute;
	
	
	@Enumerated(EnumType.STRING)
	public AccumulationType accumulationType;
	
	@Enumerated(EnumType.STRING)
	public AccumulationBehaviour accumulationBehaviour;
	
	
	@OneToOne
	@JoinColumn(name="replacing_absence_type_id")
	public AbsenceType replacingAbsenceType;
}
