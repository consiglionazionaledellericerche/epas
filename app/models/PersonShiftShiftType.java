/**
 * 
 */
package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 * @author arianna
 */
@Entity
@Table(name = "person_shift_shift_type")
public class PersonShiftShiftType extends Model {

	@Required
	@ManyToOne
	@JoinColumn(name="personshifts_id")
	public PersonShift personShift;
	
	@Required
	@ManyToOne
	@JoinColumn(name="shifttypes_id")
	public ShiftType shiftType;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="begin_date")
	public LocalDate beginDate;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_date")
	public LocalDate endDate;
}
