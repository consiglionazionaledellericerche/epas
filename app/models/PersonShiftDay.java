package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.joda.time.LocalDate;

import play.db.jpa.Model;

@Entity
@Table(name="person_shift_day")
public class PersonShiftDay extends Model{

	public LocalDate date;
	
	@OneToOne
	@JoinColumn(name="shift_type_id")
	public ShiftType shiftType;
	
	@OneToOne
	@JoinColumn(name="shift_time_table_id")
	public ShiftTimeTable shiftTimeTables;
	
	@OneToOne
	@JoinColumn(name="person_shift_id", unique=true, nullable=false, updatable=false)
	public PersonShift personShift;
	

}
