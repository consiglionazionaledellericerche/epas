/**
 * 
 */
package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;


/**
 * @author cristian
 * @author arianna
 */
@Entity
@Table(name = "person_shift_shift_type")
public class PersonShiftShiftType extends BaseModel {

	private static final long serialVersionUID = -4476838239881674080L;

	@Required
	@ManyToOne
	@JoinColumn(name="personshifts_id")
	public PersonShift personShift;
	
	@Required
	@ManyToOne
	@JoinColumn(name="shifttypes_id")
	public ShiftType shiftType;
	

	@Column(name="begin_date")
	public LocalDate beginDate;
	

	@Column(name="end_date")
	public LocalDate endDate;
}
