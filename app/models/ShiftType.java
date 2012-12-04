package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name="shift_type")
public class ShiftType extends Model{

	public String type;
	
	public String description;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_shift_id")
	public PersonShift personShift;
	
	@OneToOne
	@JoinColumn(name="person_shift_day_id", unique=true, nullable=false, updatable=false)
	public PersonShiftDay personShiftDay;
	
}
