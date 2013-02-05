package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.db.jpa.Model;

@Entity
@Table(name="person_shift_days")
public class PersonShiftDay extends Model{

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	@ManyToOne
	@JoinColumn(name="shift_type_id")
	public ShiftType shiftType;
	
	@ManyToOne
	@JoinColumn(name="shift_time_table_id")
	public ShiftTimeTable shiftTimeTable;
	
	@ManyToOne
	@JoinColumn(name="person_shift_id", nullable=false)
	public PersonShift personShift;
}
