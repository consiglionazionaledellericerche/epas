package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.joda.time.LocalTime;

import play.db.jpa.Model;

@Entity
@Table(name="shift_time_table")
public class ShiftTimeTable extends Model{

	public LocalTime startShift;
	public LocalTime endShift;
	
	public String description;
	
	@OneToOne
	@JoinColumn(name="person_shift_day_id")
	public PersonShiftDay personShiftDay;
}
